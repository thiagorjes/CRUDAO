package com.crudao.kanban.domain.rbac;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed dos papéis padrão do RBAC (RF-013): {@code admin} (protegido, RN-006, com todas as
 * permissões) e {@code user} (sem permissões administrativas). Idempotente — não recria o que já
 * existe pelo nome/chave.
 */
@Component
@RequiredArgsConstructor
public class RbacSeeder implements CommandLineRunner {

  /** Granularidade final definida na TASK-04.1 (Q-003 da techspec). */
  private static final List<String> CHAVES_PERMISSAO =
      List.of(
          "projeto:gerenciar",
          "workflow:gerenciar",
          "tarefa:gerenciar",
          "impedimento:marcar",
          "papel:gerenciar",
          "dashboard:visualizar");

  private final PapelRepository papelRepository;
  private final PermissaoRepository permissaoRepository;

  @Override
  @Transactional
  public void run(String... args) {
    Set<Permissao> todasAsPermissoes =
        CHAVES_PERMISSAO.stream().map(this::buscarOuCriarPermissao).collect(Collectors.toSet());

    buscarOuCriarPapel("admin", true, todasAsPermissoes);
    buscarOuCriarPapel("user", false, Set.of());
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
