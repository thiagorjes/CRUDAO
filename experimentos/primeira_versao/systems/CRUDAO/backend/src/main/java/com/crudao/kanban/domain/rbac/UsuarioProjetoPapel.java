package com.crudao.kanban.domain.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Papel de um usuário escopado a um projeto (BDR-001, RN-008). O papel {@code admin} nunca aparece
 * aqui — é concedido via {@link Usuario#isAdmin()}. Permite múltiplos papéis do mesmo usuário no
 * mesmo projeto; permissões efetivas = união das permissões de todos os papéis atribuídos ali.
 *
 * <p>PK composta na ordem {@code (usuario_id, projeto_id, papel_id)} — cobre com prefixo a checagem
 * pontual de {@code AutorizacaoProjetoService} ({@code usuario_id + projeto_id}). Índice adicional
 * {@code idx_upp_projeto} necessário para listar membros de um projeto (não é prefixo da PK) — ver
 * data-model.md.
 */
@Entity
@Table(
    name = "usuario_projeto_papel",
    indexes = @Index(name = "idx_upp_projeto", columnList = "projeto_id"))
@IdClass(UsuarioProjetoPapelId.class)
@Getter
@Setter
@NoArgsConstructor
public class UsuarioProjetoPapel {

  @Id
  @Column(name = "usuario_id")
  private UUID usuarioId;

  @Id
  @Column(name = "projeto_id")
  private UUID projetoId;

  @Id
  @Column(name = "papel_id")
  private UUID papelId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "papel_id", insertable = false, updatable = false)
  private Papel papel;
}
