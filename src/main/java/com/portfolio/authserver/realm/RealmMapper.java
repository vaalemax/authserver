package com.portfolio.authserver.realm;

import org.springframework.stereotype.Component;

@Component
public class RealmMapper {
    public RealmResponse toResponse(Realm realm) {
        return new RealmResponse(realm.getId(), realm.getName(), realm.getDisplayName(), realm.isEnabled());
    }
}
