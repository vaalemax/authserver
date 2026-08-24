package com.portfolio.authserver.realm;

import com.portfolio.authserver.authorization.PermissionJpaRepository;
import com.portfolio.authserver.authorization.RoleJpaRepository;
import com.portfolio.authserver.client.ClientJpaRepository;
import com.portfolio.authserver.user.AppUserJpaRepository;
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

    private final RealmService realmService;
    private final RealmJpaRepository realmJpaRepository;
    private final ClientJpaRepository clientJpaRepository;
    private final AppUserJpaRepository appUserJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    @GetMapping
    public List<RealmResponse> findRealms() {
        return realmService.listRealms().stream().map(realmService::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RealmResponse createRealm(@Valid @RequestBody CreateRealmRequest request) {
        try {
            return realmService.toResponse(realmService.createRealm(request.name(), request.displayName()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PatchMapping("/{name}")
    public RealmResponse update(@PathVariable String name, @RequestBody UpdateRealmRequest request) {
        Realm realm = realmJpaRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + name));

        if (request.displayName() != null) realm.setDisplayName(request.displayName());
        if (request.enabled() != null) realm.setEnabled(request.enabled());

        return realmService.toResponse(realmJpaRepository.save(realm));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        Realm realm = realmJpaRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + name));

        boolean hasClients = !clientJpaRepository.findByRealm_Name(name).isEmpty();
        boolean hasUsers = !appUserJpaRepository.findByRealm_Name(name).isEmpty();
        boolean hasRoles = !roleJpaRepository.findByRealm_Name(name).isEmpty();
        boolean hasPermissions = !permissionJpaRepository.findByRealm_Name(name).isEmpty();

        if (hasClients || hasUsers || hasRoles || hasPermissions) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete realm: it still contains clients, users, roles or permissions.");
        }

        realmJpaRepository.delete(realm);
        return ResponseEntity.noContent().build();
    }
}