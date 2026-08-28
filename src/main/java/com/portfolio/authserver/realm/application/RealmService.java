package com.portfolio.authserver.realm.application;

import com.portfolio.authserver.authorization.domain.PermissionRepository;
import com.portfolio.authserver.authorization.domain.RoleRepository;
import com.portfolio.authserver.client.domain.ClientRepository;
import com.portfolio.authserver.crypto.RsaKeyGenerator;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.user.domain.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RealmService {

    private final PermissionRepository permissionRepository;
    private final AppUserRepository appUserRepository;
    private final ClientRepository clientRepository;
    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;

    public List<Realm> listRealms() {
        return realmRepository.findAll();
    }

    public Realm getRealm(String realmName) {
        return realmRepository.findByName(realmName)
                .orElseThrow(() -> new NoSuchElementException("Realm not found: " + realmName));
    }

    public Realm createRealm(String realmName, String displayName) {
        if (realmRepository.findByName(realmName).isPresent()) {
            throw new IllegalArgumentException("Already existing realm: " + realmName);
        }

        KeyPair keyPair = RsaKeyGenerator.generate();
        Realm realm = new Realm();
        realm.setId(UUID.randomUUID().toString());
        realm.setName(realmName);
        realm.setDisplayName(displayName);
        realm.setEnabled(true);
        realm.setRsaKeyId(UUID.randomUUID().toString());
        realm.setRsaPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        realm.setRsaPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        return realmRepository.save(realm);
    }

    public Realm updateRealm(String realmName, String displayName, Boolean enabled) {
        Realm realm = this.getRealm(realmName);

        if (displayName != null)
            realm.setDisplayName(displayName);

        realmRepository.save(realm);
        return realm;
    }

    public void deleteRealm(String realmName){
        Realm realm = this.getRealm(realmName);

        boolean hasClients = !clientRepository.findByRealmName(realmName).isEmpty();
        boolean hasUsers = !appUserRepository.findByRealmName(realmName).isEmpty();
        boolean hasRoles = !roleRepository.findByRealmName(realmName).isEmpty();
        boolean hasPermissions = !permissionRepository.findByRealmName(realmName).isEmpty();

        if (hasClients || hasUsers || hasRoles || hasPermissions) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete realm: it still contains clients, users, roles or permissions.");
        }

        realmRepository.delete(realm);
    }
}