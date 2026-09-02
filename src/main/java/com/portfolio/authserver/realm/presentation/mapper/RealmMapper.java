package com.portfolio.authserver.realm.presentation.mapper;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.presentation.dto.RealmResponse;
import org.springframework.stereotype.Component;

@Component
public class RealmMapper {
    public RealmResponse toResponse(Realm realm) {
        return new RealmResponse(realm.getId(), realm.getName(), realm.getDisplayName(), realm.isEnabled());
    }
}
