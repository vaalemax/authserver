package com.portfolio.authserver.realm.application;

import com.portfolio.authserver.crypto.RsaKeyGenerator;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RealmService {

    private final RealmRepository realmRepository;

    public List<Realm> listRealms() {
        return realmRepository.findAll();
    }

    public Realm getRealm(String realmName) {
        return realmRepository.findByName(realmName)
                .orElseThrow(() -> new NoSuchElementException("Realm not found: " + realmName));
    }

    public Realm createRealm(String realmName, String displayName) {
        if (realmRepository.findByName(realmName).isPresent())
            throw new IllegalArgumentException("Already existing realm: " + realmName);

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

        if(displayName!=null)
            realm.setDisplayName(displayName);
        if(enabled!=null)
            realm.setEnabled(enabled);

        return realmRepository.save(realm);
    }

    public Realm toggleEnabled(String realmName) {
        Realm realm = getRealm(realmName);
        realm.setEnabled(!realm.isEnabled());
        return realmRepository.save(realm);
    }
}