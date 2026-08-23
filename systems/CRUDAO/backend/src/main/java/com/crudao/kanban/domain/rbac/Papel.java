package com.crudao.kanban.domain.rbac;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Papel do RBAC interno (RF-013). O papel {@code admin} é protegido (RN-006): não pode ser criado,
 * editado ou excluído por um papel delegado.
 */
@Entity
@Table(name = "papel", uniqueConstraints = @UniqueConstraint(columnNames = "nome"))
@Getter
@Setter
@NoArgsConstructor
public class Papel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank private String nome;

  /** RN-006: papel protegido não pode ser alterado/excluído por papel delegado. */
  private boolean protegido;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "papel_permissao",
      joinColumns = @JoinColumn(name = "papel_id"),
      inverseJoinColumns = @JoinColumn(name = "permissao_id"))
  private Set<Permissao> permissoes = new HashSet<>();

  public boolean temPermissao(String chave) {
    return permissoes.stream().anyMatch(p -> p.getChave().equals(chave));
  }
}
