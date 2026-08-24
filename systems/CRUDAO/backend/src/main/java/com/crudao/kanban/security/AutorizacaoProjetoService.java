package com.crudao.kanban.security;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.domain.rbac.UsuarioProjetoPapelRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autorização escopada a projeto (BDR-001, ADR-006). Chamada explicitamente pelos Services de
 * domínio — não mais via AOP genérico — resolvendo {@code projetoId} sempre a partir da entidade já
 * carregada, nunca de dado do payload do cliente (RNF-003).
 *
 * <p>Ordem de checagem: (1) projeto finalizado bloqueia toda escrita, inclusive para {@code
 * admin}/{@code project_admin} (RN-015) — exceto a própria permissão {@code projeto:gerenciar}, que
 * é o que permite reabrir o projeto (G-RBAC-08, checagem única, não replicada por Service); (2)
 * {@code Usuario.admin} autoriza sem consultar {@code UsuarioProjetoPapel}; (3) união das
 * permissões de todos os papéis do usuário naquele projeto.
 */
@Service
@RequiredArgsConstructor
public class AutorizacaoProjetoService {

  private static final String PERMISSAO_REABERTURA = "projeto:gerenciar";

  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  private final ProjetoRepository projetoRepository;

  @Transactional(readOnly = true)
  public void exigirPermissao(Usuario usuario, UUID projetoId, String permissao) {
    Projeto projeto = buscarProjeto(projetoId);

    if (projeto.getDataFinalizacao() != null) {
      throw new RegraDeNegocioException(
          "Projeto '%s' está finalizado e é somente leitura.".formatted(projeto.getNome()));
    }

    autorizar(usuario, projeto, permissao);
  }

  /**
   * Única forma de "escrever" em um projeto finalizado: reabri-lo. Não reusa {@link
   * #exigirPermissao} pois a checagem de RN-015 ali é incondicional — o desacoplamento é proposital
   * (evita reintroduzir um bypass geral por string de permissão, ver achado do code review da
   * TASK-01.3).
   */
  @Transactional(readOnly = true)
  public void exigirPermissaoParaReabertura(Usuario usuario, UUID projetoId) {
    Projeto projeto = buscarProjeto(projetoId);
    autorizar(usuario, projeto, PERMISSAO_REABERTURA);
  }

  /**
   * Checagem não-lançadora de uma permissão específica do usuário no projeto — usada para regras
   * que variam conforme o nível de permissão (ex.: trava de edição de tarefa iniciada, RN-009/010)
   * em vez de simplesmente autorizar/negar uma ação.
   */
  @Transactional(readOnly = true)
  public boolean temPermissao(Usuario usuario, UUID projetoId, String permissao) {
    if (usuario.isAdmin()) {
      return true;
    }
    return usuarioProjetoPapelRepository
        .findByUsuarioIdAndProjetoId(usuario.getId(), projetoId)
        .stream()
        .anyMatch(vinculo -> vinculo.getPapel().temPermissao(permissao));
  }

  /**
   * Checagem isolada de RN-015 (projeto finalizado é somente leitura), sem exigir nenhuma permissão
   * — usada por fluxos que não passam por {@link #exigirPermissao} (ex.: autoatribuição livre de
   * tarefa, RN-012) mas ainda precisam respeitar a mesma regra (G-RBAC-08: ponto único).
   */
  @Transactional(readOnly = true)
  public void exigirProjetoNaoFinalizado(UUID projetoId) {
    Projeto projeto = buscarProjeto(projetoId);
    if (projeto.getDataFinalizacao() != null) {
      throw new RegraDeNegocioException(
          "Projeto '%s' está finalizado e é somente leitura.".formatted(projeto.getNome()));
    }
  }

  /** {@code true} se o usuário é admin global ou possui qualquer papel vinculado ao projeto. */
  @Transactional(readOnly = true)
  public boolean usuarioTemAcessoAoProjeto(Usuario usuario, UUID projetoId) {
    if (usuario.isAdmin()) {
      return true;
    }
    return !usuarioProjetoPapelRepository
        .findByUsuarioIdAndProjetoId(usuario.getId(), projetoId)
        .isEmpty();
  }

  private void autorizar(Usuario usuario, Projeto projeto, String permissao) {
    if (usuario.isAdmin()) {
      return;
    }

    boolean autorizado =
        usuarioProjetoPapelRepository
            .findByUsuarioIdAndProjetoId(usuario.getId(), projeto.getId())
            .stream()
            .anyMatch(vinculo -> vinculo.getPapel().temPermissao(permissao));
    if (!autorizado) {
      throw new AcessoNegadoException(
          "Usuário sem a permissão '%s' no projeto '%s'.".formatted(permissao, projeto.getNome()));
    }
  }

  private Projeto buscarProjeto(UUID projetoId) {
    return projetoRepository
        .findById(projetoId)
        .orElseThrow(
            () -> new RecursoNaoEncontradoException("Projeto não encontrado: " + projetoId));
  }
}
