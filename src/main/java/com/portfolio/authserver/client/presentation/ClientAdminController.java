package com.portfolio.authserver.client.presentation;

import com.portfolio.authserver.client.application.ClientService;
import com.portfolio.authserver.client.domain.Client;
import com.portfolio.authserver.client.presentation.dto.ClientResponse;
import com.portfolio.authserver.client.presentation.dto.CreateClientRequest;
import com.portfolio.authserver.client.presentation.dto.UpdateClientRequest;
import com.portfolio.authserver.client.presentation.mapper.ClientMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/realms/{realmName}/clients")
@RequiredArgsConstructor
public class ClientAdminController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    @GetMapping
    public List<ClientResponse> findClients(@PathVariable String realmName) {
        return clientService.listClients(realmName).stream().map(clientMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@PathVariable String realmName,
                                       @Valid @RequestBody CreateClientRequest request) {
        try {
            Client created = clientService.createClient(realmName, request.clientId(), request.clientSecret(),
                    request.redirectUris(), request.scopes(),
                    request.requireProofKey(), request.requireAuthorizationConsent());
            return clientMapper.toResponse(created);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PatchMapping("/{clientId}")
    public ClientResponse update(@PathVariable String realmName, @PathVariable String clientId,
                                 @RequestBody UpdateClientRequest request) {
        try {
            Client updated = clientService.updateClient(realmName, clientId, request.redirectUris(),
                    request.scopes(), Boolean.TRUE.equals(request.requireProofKey()),
                    Boolean.TRUE.equals(request.requireAuthorizationConsent()), request.newClientSecret());
            return clientMapper.toResponse(updated);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping
    @RequestMapping("/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable String realmName, @PathVariable String clientId){
        try{
            clientService.deleteClient(realmName, clientId);
            return ResponseEntity.noContent().build();
        }catch(NoSuchElementException ex){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}