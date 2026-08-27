package com.crudao.kanban.domain.workflow;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
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
public class TransicaoService {

  private static final String PERMISSAO = "workflow:gerenciar";

  private final TransicaoRepository transicaoRepository;
  private final EtapaRepository etapaRepository;
  private final WorkflowMapper workflowMapper;
  private final AutorizacaoProjetoService autorizacaoProjetoService;
  private final UsuarioContexto usuarioContexto;

  /** Usado pelo frontend para calcular colunas de destino válidas durante o drag (DDR-002). */
  @Transactional(readOnly = true)
  public List<TransicaoDTO> listarPorWorkflow(UUID workflowId) {
    return transicaoRepository.findByWorkflowId(workflowId).stream()
        .map(workflowMapper::paraDTO)
        .toList();
  }

  @Transactional
  public TransicaoDTO criar(TransicaoRequest request) {
    Etapa origem = buscarEtapa(request.etapaOrigemId());
    Etapa destino = buscarEtapa(request.etapaDestinoId());
    exigirPermissao(origem.getWorkflow().getProjetoId());
    Transicao transicao = new Transicao();
    transicao.setEtapaOrigem(origem);
    transicao.setEtapaDestino(destino);
    transicao.setTipo(request.tipo());
    return workflowMapper.paraDTO(transicaoRepository.save(transicao));
  }

  @Transactional
  public void excluir(UUID id) {
    Transicao transicao =
        transicaoRepository
            .findById(id)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Transição não encontrada: " + id));
    exigirPermissao(transicao.getEtapaOrigem().getWorkflow().getProjetoId());
    transicaoRepository.delete(transicao);
  }

  private Etapa buscarEtapa(UUID id) {
    return etapaRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa não encontrada: " + id));
  }

  private void exigirPermissao(UUID projetoId) {
    Usuario usuario = usuarioContexto.usuarioAtual();
    autorizacaoProjetoService.exigirPermissao(usuario, projetoId, PERMISSAO);
  }
}
