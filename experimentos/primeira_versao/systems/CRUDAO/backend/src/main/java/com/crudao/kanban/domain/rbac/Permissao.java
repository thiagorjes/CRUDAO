package com.crudao.kanban.domain.rbac;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Permissão granular do RBAC interno (RF-013) — ex.: {@code projeto:gerenciar}, {@code
 * tarefa:gerenciar}. Chaves finais definidas na TASK-04.1 (Q-003 da techspec).
 */
@Entity
@Table(name = "permissao", uniqueConstraints = @UniqueConstraint(columnNames = "chave"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Permissao {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @NotBlank private String chave;
}
