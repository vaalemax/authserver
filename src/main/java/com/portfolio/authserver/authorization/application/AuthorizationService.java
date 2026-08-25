package com.portfolio.authserver.authorization.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portfolio.authserver.authorization.domain.Permission;
import com.portfolio.authserver.authorization.domain.UserRole;
import com.portfolio.authserver.authorization.domain.UserRoleRepository;
import com.portfolio.authserver.authorization.presentation.dto.CanResult;
import com.portfolio.authserver.authorization.presentation.dto.ConditionMatch;
import com.portfolio.authserver.authorization.presentation.dto.UserRoleAttribute;
import com.portfolio.authserver.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final UserRoleRepository userRoleRepository;
    private final ObjectMapper objectMapper;

    public CanResult can(AppUser user, String subject, String action) {
        Instant now = Instant.now();
        List<UserRole> assignments = userRoleRepository.findActiveForUser(user, now);

        List<ConditionMatch> matches = new ArrayList<>();

        for (UserRole assignment : assignments) {
            for (Permission permission : assignment.getRole().getPermissions()) {
                if (permission.getSubject().equals(subject) && permission.getAction().equals(action)) {
                    JsonNode condition = resolveCondition(permission.getConditionTemplate(), assignment.getAttributes());
                    matches.add(new ConditionMatch(assignment.getRole().getName(), condition));
                }
            }
        }

        return new CanResult(!matches.isEmpty(), matches);
    }

    private JsonNode resolveCondition(String template, String attributesJson) {
        if (template == null)
            return null;

        Map<String, UserRoleAttribute> byKey;
        try {
            List<UserRoleAttribute> attrs = objectMapper.readValue(attributesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, UserRoleAttribute.class));
            byKey = attrs.stream().collect(Collectors.toMap(UserRoleAttribute::key, a -> a));
        } catch (Exception ex) {
            byKey = Map.of();
        }

        ObjectNode result = objectMapper.createObjectNode();
        Matcher matcher = PLACEHOLDER.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            UserRoleAttribute attr = byKey.get(key);

            if (attr == null) {
                result.putNull(key);
            } else if (Boolean.TRUE.equals(attr.isArray())) {
                try {
                    result.set(key, objectMapper.readTree(attr.value()));
                } catch (Exception ex) {
                    result.put(key, attr.value());
                }
            } else {
                result.put(key, attr.value());
            }
        }
        return result;
    }

}