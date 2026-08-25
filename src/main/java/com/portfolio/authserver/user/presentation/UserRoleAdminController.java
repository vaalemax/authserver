package com.portfolio.authserver.user.presentation;

import com.portfolio.authserver.authorization.application.UserRoleService;
import com.portfolio.authserver.authorization.domain.Role;
import com.portfolio.authserver.authorization.domain.RoleRepository;
import com.portfolio.authserver.authorization.domain.UserRole;
import com.portfolio.authserver.authorization.domain.UserRoleRepository;
import com.portfolio.authserver.authorization.presentation.dto.CreateUserRoleRequest;
import com.portfolio.authserver.authorization.presentation.dto.UserRoleResponse;
import com.portfolio.authserver.authorization.presentation.mapper.AuthorizationMapper;
import com.portfolio.authserver.user.domain.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realmName}/users/{username}/roles")
@RequiredArgsConstructor
public class UserRoleAdminController {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleService userRoleService;
    private final AuthorizationMapper authorizationMapper;

    @GetMapping
    public List<UserRoleResponse> findUserRoles(@PathVariable String realmName, @PathVariable String username) {
        return userRoleRepository.findByAppUser(userRoleService.findUserOrThrow(realmName, username))
                .stream().map(authorizationMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRoleResponse createUserRoles(@PathVariable String realmName, @PathVariable String username,
                                   @Valid @RequestBody CreateUserRoleRequest request) {
        AppUser user = userRoleService.findUserOrThrow(realmName, username);

        Role role = roleRepository.findByIdAndRealmName(request.roleId(), realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Role not found: " + request.roleId()));

        UserRole userRole = new UserRole();
        userRole.setId(UUID.randomUUID().toString());
        userRole.setAppUser(user);
        userRole.setRole(role);
        userRole.setValidFrom(request.validFrom() != null ? request.validFrom() : Instant.now());
        userRole.setValidTo(request.validTo());
        userRole.setAttributes(userRoleService.writeAttributes(request.attributes()));

        return authorizationMapper.toResponse(userRoleRepository.save(userRole));
    }
}