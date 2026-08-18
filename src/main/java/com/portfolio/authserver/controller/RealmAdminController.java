package com.portfolio.authserver.controller;

import com.portfolio.authserver.model.Realm;
import com.portfolio.authserver.model.dto.CreateRealmRequest;
import com.portfolio.authserver.model.dto.RealmResponse;
import com.portfolio.authserver.service.RealmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/realms")
@RequiredArgsConstructor
public class RealmAdminController {

    private final RealmService realmService;

    @GetMapping
    public List<RealmResponse> findRealms() {
        return realmService.listRealms().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RealmResponse createRealm(@Valid @RequestBody CreateRealmRequest request) {
        try {
            return toResponse(realmService.createRealm(request.name(), request.displayName()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private RealmResponse toResponse(Realm realm) {
        return new RealmResponse(realm.getId(), realm.getName(), realm.getDisplayName(), realm.isEnabled());
    }
}