package com.portfolio.authserver.controller;

import com.portfolio.authserver.model.Permission;
import com.portfolio.authserver.model.Realm;
import com.portfolio.authserver.model.dto.CreatePermissionRequest;
import com.portfolio.authserver.model.dto.PermissionResponse;
import com.portfolio.authserver.repository.PermissionJpaRepository;
import com.portfolio.authserver.repository.RealmJpaRepository;
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

    private final RealmJpaRepository realmJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    @GetMapping
    public List<PermissionResponse> findPermissions(@PathVariable String realmName) {
        return permissionJpaRepository.findByRealm_Name(realmName).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse createPermission(@PathVariable String realmName, @Valid @RequestBody CreatePermissionRequest request) {
        Realm realm = realmJpaRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + realmName));

        if (permissionJpaRepository.findByRealm_NameAndSubjectAndAction(realmName, request.subject(), request.action()).isPresent()) {
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

        return toResponse(permissionJpaRepository.save(permission));
    }

    private PermissionResponse toResponse(Permission p) {
        return new PermissionResponse(p.getId(), p.getName(), p.getSubject(), p.getAction(), p.getConditionTemplate());
    }
}
