package com.portfolio.authserver.client.domain;

import com.portfolio.authserver.realm.domain.Realm;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "oauth2_client")
@Getter
@Setter
@NoArgsConstructor
public class Client {

    @Id
    private String id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "client_id_issued_at")
    private Instant clientIdIssuedAt;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    @Column(name = "client_name")
    private String clientName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_auth_methods", joinColumns = @JoinColumn(name = "client_pk_id"))
    @Column(name = "authentication_method")
    private Set<String> clientAuthenticationMethods = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_grant_types", joinColumns = @JoinColumn(name = "client_pk_id"))
    @Column(name = "grant_type")
    private Set<String> authorizationGrantTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_redirect_uris", joinColumns = @JoinColumn(name = "client_pk_id"))
    @Column(name = "redirect_uri")
    private Set<String> redirectUris = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_scopes", joinColumns = @JoinColumn(name = "client_pk_id"))
    @Column(name = "scope")
    private Set<String> scopes = new HashSet<>();

    @Column(name = "client_settings")
    private String clientSettings;

    @Column(name = "token_settings")
    private String tokenSettings;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "realm_id", nullable = false)
    private Realm realm;
}
