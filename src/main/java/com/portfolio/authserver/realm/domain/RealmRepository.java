package com.portfolio.authserver.realm.domain;

import java.util.Optional;
import java.util.List;

public interface RealmRepository {
    Optional<Realm> findByName(String name);

    List<Realm> findAll();

    Realm save(Realm realm);

    void delete(Realm realm);
}
