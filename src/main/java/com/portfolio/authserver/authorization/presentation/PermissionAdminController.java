package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.domain.Permission;
import com.portfolio.authserver.authorization.domain.PermissionRepository;
import com.portfolio.authserver.authorization.presentation.dto.CreatePermissionRequest;
import com.portfolio.authserver.authorization.presentation.dto.PermissionResponse;
import com.portfolio.authserver.authorization.presentation.mapper.AuthorizationMapper;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realmName}/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final RealmRepository realmJpaRepository;
    private final PermissionRepository permissionRepository;
    private final AuthorizationMapper authorizationMapper;

    @GetMapping
    public List<PermissionResponse> findPermissions(@PathVariable String realmName) {
        return permissionRepository.findByRealmName(realmName).stream().map(authorizationMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse createPermission(@PathVariable String realmName, @Valid @RequestBody CreatePermissionRequest request) {
        Realm realm = realmJpaRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + realmName));

        if (permissionRepository.findByRealmNameAndSubjectAndAction(realmName, request.subject(), request.action()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Already existing permission for " + request.subject() + "/" + request.action());
        }

        Permission permission = new Permission();
        permission.setId(UUID.randomUUID().toString());
        permission.setRealm(realm);
        permission.setName(request.name());
        permission.setSubject(request.subject());
        permission.setSubjectLabel(request.subjectLabel());
        permission.setAction(request.action());
        permission.setActionLabel(request.actionLabel());
        permission.setConditionTemplate(request.conditionTemplate());
        permission.setConditionLabel(request.conditionLabel());

        return authorizationMapper.toResponse(permissionRepository.save(permission));
    }
}
