package com.portfolio.authserver.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "realm")
@Getter
@Setter
@NoArgsConstructor
public class Realm {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name; // slug, es. "aether" — finirà nel path degli endpoint

    @Column(name = "display_name")
    private String displayName;

    private boolean enabled = true;

    //@Lob
    @Column(name = "rsa_key_id")
    private String rsaKeyId;

    //@Lob
    @Column(name = "rsa_public_key")
    private String rsaPublicKey;   // Base64 X.509

    //@Lob
    @Column(name = "rsa_private_key")
    private String rsaPrivateKey;  // Base64 PKCS8
}