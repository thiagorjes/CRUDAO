package com.crudao.kanban.domain.raia;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.common.VerificadorDeTarefasAtivas;
import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.security.AutorizacaoProjetoService;
import com.crudao.kanban.security.UsuarioContexto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RaiaService {

  private static final String PERMISSAO = "projeto:gerenciar";

  private final RaiaRepository raiaRepository;
  private final ProjetoRepository projetoRepository;
  private final RaiaMapper raiaMapper;
  private final VerificadorDeTarefasAtivas verificadorDeTarefasAtivas;
  private final AutorizacaoProjetoService autorizacaoProjetoService;
  private final UsuarioContexto usuarioContexto;

  /** Board de um projeto sem raias próprias retorna as raias default globais — RF-011. */
  @Transactional(readOnly = true)
  public List<RaiaDTO> listarParaProjeto(UUID projetoId) {
    List<Raia> raiasDoProjeto = raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId);
    List<Raia> raiasDefaultGlobais = raiaRepository.findByProjetoIsNullOrderByOrdemAsc();
    return RaiaResolver.resolver(raiasDoProjeto, raiasDefaultGlobais).stream()
        .map(raiaMapper::paraDTO)
        .toList();
  }

  @Transactional
  public RaiaDTO criar(RaiaRequest request) {
    exigirPermissao(request.projetoId());
    Raia raia = new Raia();
    raia.setProjeto(buscarProjetoOuNulo(request.projetoId()));
    raia.setNome(request.nome());
    raia.setOrdem(request.ordem());
    return raiaMapper.paraDTO(raiaRepository.save(raia));
  }

  @Transactional
  public RaiaDTO editar(UUID id, RaiaRequest request) {
    Raia raia = buscarEntidade(id);
    exigirPermissao(raiaProjetoId(raia));
    raia.setProjeto(buscarProjetoOuNulo(request.projetoId()));
    raia.setNome(request.nome());
    raia.setOrdem(request.ordem());
    return raiaMapper.paraDTO(raiaRepository.save(raia));
  }

  /** RN-005: exclusão bloqueada se houver tarefas na raia. */
  @Transactional
  public void excluir(UUID id) {
    Raia raia = buscarEntidade(id);
    exigirPermissao(raiaProjetoId(raia));
    if (verificadorDeTarefasAtivas.existemTarefasNaRaia(id)) {
      throw new RegraDeNegocioException(
          "Não é possível excluir a raia '%s': há tarefas nela. Migre as tarefas antes."
              .formatted(raia.getNome()));
    }
    raiaRepository.delete(raia);
  }

  private Projeto buscarProjetoOuNulo(UUID projetoId) {
    if (projetoId == null) {
      return null;
    }
    return projetoRepository
        .findById(projetoId)
        .orElseThrow(
            () -> new RecursoNaoEncontradoException("Projeto não encontrado: " + projetoId));
  }

  private Raia buscarEntidade(UUID id) {
    return raiaRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Raia não encontrada: " + id));
  }

  private UUID raiaProjetoId(Raia raia) {
    return raia.getProjeto() != null ? raia.getProjeto().getId() : null;
  }

  /**
   * Raia default global ({@code projetoId == null}) não tem projeto para escopar a permissão —
   * gerenciada só por {@code admin} global. Raia de projeto usa {@code AutorizacaoProjetoService}
   * normalmente (RN-015 incluído).
   */
  private void exigirPermissao(UUID projetoId) {
    Usuario usuario = usuarioContexto.usuarioAtual();
    if (projetoId == null) {
      if (!usuario.isAdmin()) {
        throw new AcessoNegadoException(
            "Apenas o admin global pode gerenciar raias default globais.");
      }
      return;
    }
    autorizacaoProjetoService.exigirPermissao(usuario, projetoId, PERMISSAO);
  }
}
