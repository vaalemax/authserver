package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.application.PermissionService;
import com.portfolio.authserver.authorization.presentation.dto.CreatePermissionRequest;
import com.portfolio.authserver.authorization.presentation.dto.PermissionResponse;
import com.portfolio.authserver.authorization.presentation.dto.UpdatePermissionRequest;
import com.portfolio.authserver.authorization.presentation.mapper.AuthorizationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/realms/{realmName}/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final AuthorizationMapper authorizationMapper;
    private final PermissionService permissionService;

    @GetMapping
    public List<PermissionResponse> findPermissions(@PathVariable String realmName) {
        return permissionService.listPermissions(realmName).stream().map(authorizationMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse createPermission(@PathVariable String realmName,
                                               @Valid @RequestBody CreatePermissionRequest request) {
        try {
            return authorizationMapper.toResponse(permissionService.createPermission(realmName, request.name(),
                    request.subject(), request.subjectLabel(), request.action(), request.actionLabel(),
                    request.conditionTemplate(), request.conditionLabel()));
        }catch(NoSuchElementException ex){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }catch(IllegalArgumentException ex){
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PatchMapping("/{permissionId}")
    public PermissionResponse updatePermission(@PathVariable String realmName, @PathVariable String permissionId,
                                               @Valid @RequestBody UpdatePermissionRequest request){
        try{
            return authorizationMapper.toResponse(permissionService.updatePermission(realmName, permissionId,
                    request.name(), request.subjectLabel(), request.actionLabel(), request.conditionTemplate(),
                    request.conditionLabel()));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> deletePermission(@PathVariable String realmName, @PathVariable String permissionId){
        try{
            permissionService.deletePermission(realmName, permissionId);
            return ResponseEntity.noContent().build();
        }catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }
}
