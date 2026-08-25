package com.portfolio.authserver.realm.presentation;

import com.portfolio.authserver.authorization.domain.PermissionRepository;
import com.portfolio.authserver.authorization.domain.RoleRepository;
import com.portfolio.authserver.client.domain.ClientRepository;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.realm.application.RealmService;
import com.portfolio.authserver.realm.presentation.dto.CreateRealmRequest;
import com.portfolio.authserver.realm.presentation.dto.RealmResponse;
import com.portfolio.authserver.realm.presentation.dto.UpdateRealmRequest;
import com.portfolio.authserver.realm.presentation.mapper.RealmMapper;
import com.portfolio.authserver.user.domain.AppUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/realms")
@RequiredArgsConstructor
public class RealmAdminController {

    private final PermissionRepository permissionRepository;
    private final AppUserRepository appUserRepository;
    private final ClientRepository clientRepository;
    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;
    private final RealmService realmService;
    private final RealmMapper realmMapper;

    @GetMapping
    public List<RealmResponse> findRealms() {
        return realmService.listRealms().stream().map(realmMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RealmResponse createRealm(@Valid @RequestBody CreateRealmRequest request) {
        try {
            return realmMapper.toResponse(realmService.createRealm(request.name(), request.displayName()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PatchMapping("/{name}")
    public RealmResponse update(@PathVariable String name, @RequestBody UpdateRealmRequest request) {
        Realm realm = realmRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + name));

        if (request.displayName() != null) realm.setDisplayName(request.displayName());
        if (request.enabled() != null) realm.setEnabled(request.enabled());

        return realmMapper.toResponse(realmRepository.save(realm));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        Realm realm = realmRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + name));

        boolean hasClients = !clientRepository.findByRealmName(name).isEmpty();
        boolean hasUsers = !appUserRepository.findByRealmName(name).isEmpty();
        boolean hasRoles = !roleRepository.findByRealmName(name).isEmpty();
        boolean hasPermissions = !permissionRepository.findByRealmName(name).isEmpty();

        if (hasClients || hasUsers || hasRoles || hasPermissions) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete realm: it still contains clients, users, roles or permissions.");
        }

        realmRepository.delete(realm);
        return ResponseEntity.noContent().build();
    }
}