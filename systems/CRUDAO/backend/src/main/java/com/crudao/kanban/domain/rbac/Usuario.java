package com.crudao.kanban.domain.rbac;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuário interno (RF-013, RF-014). Autenticado via Keycloak ({@code keycloakSub} preenchido) ou
 * via fallback local ({@code senhaHash} preenchido, quando o Keycloak está indisponível — RF-014
 * Should Have, ADR-003).
 */
@Entity
@Table(
    name = "usuario",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = "keycloak_sub"),
      @UniqueConstraint(columnNames = "email")
    })
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** Nulo para usuários criados apenas para login local de fallback. */
  private String keycloakSub;

  private String email;

  private String nome;

  /** Hash BCrypt — presente apenas em usuários habilitados para o login local de fallback. */
  private String senhaHash;

  /**
   * @deprecated (BDR-001/ADR-006) vínculo único global de papel, substituído por {@code
   *     UsuarioProjetoPapel}. Mantido apenas até a migration de remoção de coluna (data-model.md,
   *     nota de migração Q-006) — não é mais consultado pelo enforcement de autorização.
   */
  @Deprecated
  @ManyToOne(optional = false)
  @JoinColumn(name = "papel_id")
  private Papel papel;

  /**
   * Papel {@code admin} global (BDR-001) — autorizado em qualquer projeto sem depender de {@code
   * UsuarioProjetoPapel}. Default {@code false}.
   */
  private boolean admin;
}
