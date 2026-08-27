package com.crudao.kanban.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** Usuário provisionado via login OIDC (Keycloak) — RF-014. */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "keycloak_sub", nullable = false, unique = true)
    private String keycloakSub;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Bootstrap do primeiro admin (RF-008, ADR-007) — bypassa toda checagem de RBAC escopado. */
    @Column(name = "admin_global", nullable = false)
    private boolean adminGlobal = false;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;
}
