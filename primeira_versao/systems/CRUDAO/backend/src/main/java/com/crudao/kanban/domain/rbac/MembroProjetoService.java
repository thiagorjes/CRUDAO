package com.crudao.kanban.domain.rbac;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.common.EntradaInvalidaException;
import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.security.UsuarioContexto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Associação de usuários a projetos com papéis (RF-015, BDR-001). Gerenciado por {@code admin}
 * global (qualquer projeto) ou {@code project_admin} do(s) próprio(s) projeto(s) — sempre papéis já
 * existentes no catálogo, nunca cria papel novo aqui.
 */
@Service
@RequiredArgsConstructor
public class MembroProjetoService {

  private static final String PAPEL_PROJECT_ADMIN = "project_admin";
  private static final String PERMISSAO_PAPEL_GERENCIAR = "papel:gerenciar";

  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  private final UsuarioRepository usuarioRepository;
  private final PapelRepository papelRepository;
  private final ProjetoRepository projetoRepository;
  private final UsuarioContexto usuarioContexto;

  @Transactional(readOnly = true)
  public List<MembroDTO> listar(UUID projetoId) {
    Usuario atual = usuarioContexto.usuarioAtual();
    exigirVinculoOuAdmin(atual, projetoId);

    List<UsuarioProjetoPapel> vinculos = usuarioProjetoPapelRepository.findByProjetoId(projetoId);
    Map<UUID, List<UsuarioProjetoPapel>> porUsuario =
        vinculos.stream().collect(Collectors.groupingBy(UsuarioProjetoPapel::getUsuarioId));

    return porUsuario.entrySet().stream()
        .map(
            entrada -> {
              Usuario usuario = buscarUsuario(entrada.getKey());
              List<String> papeis =
                  entrada.getValue().stream().map(v -> v.getPapel().getNome()).toList();
              return new MembroDTO(usuario.getId(), usuario.getNome(), papeis);
            })
        .toList();
  }

  /**
   * Substitui o conjunto de papéis do usuário no projeto (RF-015). Rejeita (422) papel {@code
   * admin}/protegido ou qualquer papel com {@code papel:gerenciar} — G-RBAC-07, fecha o vetor de
   * escalação de privilégio.
   */
  @Transactional
  public void atribuir(UUID projetoId, UUID usuarioId, AtribuirPapeisRequest request) {
    Usuario atual = usuarioContexto.usuarioAtual();
    Projeto projeto = buscarProjeto(projetoId);
    exigirAdminOuProjectAdminDoProjeto(atual, projetoId);
    if (projeto.getDataFinalizacao() != null) {
      throw new RegraDeNegocioException(
          "Projeto '%s' está finalizado e é somente leitura.".formatted(projeto.getNome()));
    }
    buscarUsuario(usuarioId);

    List<Papel> papeis = request.papeis().stream().map(this::buscarPapel).toList();
    papeis.forEach(this::exigirPapelAtribuivelPorProjeto);

    usuarioProjetoPapelRepository.deleteByUsuarioIdAndProjetoId(usuarioId, projetoId);
    papeis.forEach(
        papel -> {
          UsuarioProjetoPapel vinculo = new UsuarioProjetoPapel();
          vinculo.setUsuarioId(usuarioId);
          vinculo.setProjetoId(projetoId);
          vinculo.setPapelId(papel.getId());
          usuarioProjetoPapelRepository.save(vinculo);
        });
  }

  private void exigirPapelAtribuivelPorProjeto(Papel papel) {
    if (papel.isProtegido() || papel.temPermissao(PERMISSAO_PAPEL_GERENCIAR)) {
      throw new EntradaInvalidaException(
          "Papel '%s' não pode ser atribuído por projeto.".formatted(papel.getNome()));
    }
  }

  private void exigirVinculoOuAdmin(Usuario atual, UUID projetoId) {
    if (atual.isAdmin()) {
      return;
    }
    if (usuarioProjetoPapelRepository
        .findByUsuarioIdAndProjetoId(atual.getId(), projetoId)
        .isEmpty()) {
      throw new AcessoNegadoException("Usuário sem vínculo com o projeto.");
    }
  }

  private void exigirAdminOuProjectAdminDoProjeto(Usuario atual, UUID projetoId) {
    if (atual.isAdmin()) {
      return;
    }
    boolean projectAdmin =
        usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(atual.getId(), projetoId).stream()
            .anyMatch(v -> PAPEL_PROJECT_ADMIN.equalsIgnoreCase(v.getPapel().getNome()));
    if (!projectAdmin) {
      throw new AcessoNegadoException(
          "Requer admin global ou project_admin do projeto para gerenciar membros.");
    }
  }

  private Usuario buscarUsuario(UUID id) {
    return usuarioRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
  }

  private Papel buscarPapel(UUID id) {
    return papelRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Papel não encontrado: " + id));
  }

  private Projeto buscarProjeto(UUID id) {
    return projetoRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado: " + id));
  }
}
