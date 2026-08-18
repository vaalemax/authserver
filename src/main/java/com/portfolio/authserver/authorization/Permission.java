package com.portfolio.authserver.authorization;

import com.portfolio.authserver.realm.Realm;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
public class Permission {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "realm_id", nullable = false)
    private Realm realm;

    private String name;
    private String subject;

    @Column(name = "subject_label")
    private String subjectLabel;

    private String action;

    @Column(name = "action_label")
    private String actionLabel;

    @Column(name = "condition_template")
    private String conditionTemplate;

    @Column(name = "condition_label")
    private String conditionLabel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}