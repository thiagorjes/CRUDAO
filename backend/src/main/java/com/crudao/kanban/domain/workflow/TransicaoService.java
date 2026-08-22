package com.crudao.kanban.domain.workflow;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransicaoService {

  private final TransicaoRepository transicaoRepository;
  private final EtapaRepository etapaRepository;
  private final WorkflowMapper workflowMapper;

  @Transactional
  public TransicaoDTO criar(TransicaoRequest request) {
    Etapa origem = buscarEtapa(request.etapaOrigemId());
    Etapa destino = buscarEtapa(request.etapaDestinoId());
    Transicao transicao = new Transicao();
    transicao.setEtapaOrigem(origem);
    transicao.setEtapaDestino(destino);
    transicao.setTipo(request.tipo());
    return workflowMapper.paraDTO(transicaoRepository.save(transicao));
  }

  @Transactional
  public void excluir(UUID id) {
    if (!transicaoRepository.existsById(id)) {
      throw new RecursoNaoEncontradoException("Transição não encontrada: " + id);
    }
    transicaoRepository.deleteById(id);
  }

  private Etapa buscarEtapa(UUID id) {
    return etapaRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa não encontrada: " + id));
  }
}
