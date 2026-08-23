package com.crudao.kanban.domain.raia;

import com.crudao.kanban.domain.projeto.Projeto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Raia (Swimlane) — RF-011. Pode ser específica de um {@link Projeto} ou default global (quando
 * {@code projeto} é nulo).
 */
@Entity
@Table(name = "raia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Raia {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** Nulo = raia default global (usada por projetos sem raias próprias). */
  @ManyToOne
  @JoinColumn(name = "projeto_id")
  private Projeto projeto;

  @NotBlank private String nome;

  private int ordem;
}
