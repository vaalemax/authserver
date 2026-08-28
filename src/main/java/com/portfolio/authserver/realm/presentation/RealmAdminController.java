package com.portfolio.authserver.realm.presentation;

import com.portfolio.authserver.realm.application.RealmService;
import com.portfolio.authserver.realm.presentation.dto.CreateRealmRequest;
import com.portfolio.authserver.realm.presentation.dto.RealmResponse;
import com.portfolio.authserver.realm.presentation.dto.UpdateRealmRequest;
import com.portfolio.authserver.realm.presentation.mapper.RealmMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/realms")
@RequiredArgsConstructor
public class RealmAdminController {

    private final RealmService realmService;
    private final RealmMapper realmMapper;

    @GetMapping
    public List<RealmResponse> findRealms() {
        return realmService.listRealms().stream().map(realmMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RealmResponse createRealm(@Valid @RequestBody CreateRealmRequest request) {
        try {
            return realmMapper.toResponse(realmService.createRealm(request.name(), request.displayName()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PatchMapping("/{realmName}")
    public RealmResponse updateRealm(@PathVariable String realmName, @RequestBody UpdateRealmRequest request) {
        try{
            return realmMapper.toResponse(realmService.updateRealm(realmName, request.displayName(), request.enabled()));
        }catch(NoSuchElementException ex){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}