package com.crudao.kanban.domain.rbac;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed do catálogo de papéis padrão do RBAC por projeto (RF-013, BDR-001): {@code admin}
 * (protegido, global, RN-006) e os papéis atribuíveis via {@link UsuarioProjetoPapel} —{@code
 * project_admin}, {@code product_owner}, {@code dev}, {@code gestor}, {@code user} (legado, sem
 * permissões). Idempotente — não recria o que já existe pelo nome/chave. Permissões default por
 * papel conforme a tabela do PRD v1.3 (RF-013).
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RbacSeeder implements CommandLineRunner {

  /** Granularidade final definida na TASK-04.1 (Q-003), ampliada na TASK-04.2 (data-model v1.2). */
  private static final List<String> CHAVES_PERMISSAO =
      List.of(
          "projeto:gerenciar",
          "workflow:gerenciar",
          "tarefa:gerenciar",
          "tarefa:atribuir",
          "tarefa:finalizar",
          "impedimento:marcar",
          "papel:gerenciar",
          "dashboard:visualizar");

  private final PapelRepository papelRepository;
  private final PermissaoRepository permissaoRepository;

  @Override
  @Transactional
  public void run(String... args) {
    CHAVES_PERMISSAO.forEach(this::buscarOuCriarPermissao);

    buscarOuCriarPapel("admin", true, chaves(CHAVES_PERMISSAO));

    // project_admin: tudo, exceto papel:gerenciar (G-RBAC-07 — nunca atribuível via projeto).
    buscarOuCriarPapel(
        "project_admin",
        false,
        chaves(
            "projeto:gerenciar",
            "workflow:gerenciar",
            "tarefa:gerenciar",
            "tarefa:atribuir",
            "tarefa:finalizar",
            "impedimento:marcar",
            "dashboard:visualizar"));

    // product_owner: gerencia/atribui/finaliza tarefas e marca impedimento (RF-013).
    buscarOuCriarPapel(
        "product_owner",
        false,
        chaves(
            "tarefa:gerenciar",
            "tarefa:atribuir",
            "tarefa:finalizar",
            "impedimento:marcar",
            "dashboard:visualizar"));

    // dev: cria/edita/move tarefa e marca impedimento; não atribui a outros nem finaliza (RN-011,
    // RN-012).
    buscarOuCriarPapel("dev", false, chaves("tarefa:gerenciar", "impedimento:marcar"));

    // gestor: só dashboard por padrão — sem impedimento:marcar (RN-013).
    buscarOuCriarPapel("gestor", false, chaves("dashboard:visualizar"));

    // user (legado): fallback sem permissão nenhuma (RN-014).
    buscarOuCriarPapel("user", false, Set.of());
  }

  private Set<Permissao> chaves(String... chaves) {
    return chaves(List.of(chaves));
  }

  private Set<Permissao> chaves(List<String> chaves) {
    return chaves.stream().map(this::buscarOuCriarPermissao).collect(Collectors.toSet());
  }

  private Permissao buscarOuCriarPermissao(String chave) {
    return permissaoRepository
        .findByChave(chave)
        .orElseGet(() -> permissaoRepository.save(new Permissao(null, chave)));
  }

  private void buscarOuCriarPapel(String nome, boolean protegido, Set<Permissao> permissoes) {
    if (papelRepository.findByNomeIgnoreCase(nome).isPresent()) {
      return;
    }
    Papel papel = new Papel();
    papel.setNome(nome);
    papel.setProtegido(protegido);
    papel.setPermissoes(permissoes);
    papelRepository.save(papel);
  }
}
