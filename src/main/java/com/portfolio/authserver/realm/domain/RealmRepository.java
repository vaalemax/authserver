package com.portfolio.authserver.realm.domain;

import java.util.Optional;

public interface RealmRepository {
    Optional<Realm> findByName(String name);
}
