package com.portfolio.authserver.realm.application;

import com.portfolio.authserver.crypto.RsaKeyGenerator;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RealmService {

    private final RealmRepository realmRepository;

    public Realm createRealm(String name, String displayName) {
        if (realmRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Already existing realm: " + name);
        }

        KeyPair keyPair = RsaKeyGenerator.generate();
        Realm realm = new Realm();
        realm.setId(UUID.randomUUID().toString());
        realm.setName(name);
        realm.setDisplayName(displayName);
        realm.setEnabled(true);
        realm.setRsaKeyId(UUID.randomUUID().toString());
        realm.setRsaPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        realm.setRsaPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        return realmRepository.save(realm);
    }

    public Realm getOrCreateRealm(String name, String displayName) {
        return realmRepository.findByName(name).orElseGet(() -> createRealm(name, displayName));
    }

    public List<Realm> listRealms() {
        return realmRepository.findAll();
    }
}