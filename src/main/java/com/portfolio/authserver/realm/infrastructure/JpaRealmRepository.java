package com.portfolio.authserver.realm.infrastructure;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRealmRepository extends RealmRepository, JpaRepository<Realm, String> {
}
