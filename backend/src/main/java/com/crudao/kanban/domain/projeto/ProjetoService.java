package com.crudao.kanban.domain.projeto;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.common.VerificadorDeTarefasAtivas;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjetoService {

  private final ProjetoRepository projetoRepository;
  private final ProjetoMapper projetoMapper;
  private final VerificadorDeTarefasAtivas verificadorDeTarefasAtivas;

  @Transactional(readOnly = true)
  public List<ProjetoDTO> listar() {
    return projetoRepository.findAll().stream().map(projetoMapper::paraDTO).toList();
  }

  @Transactional(readOnly = true)
  public ProjetoDTO buscar(UUID id) {
    return projetoMapper.paraDTO(buscarEntidade(id));
  }

  @Transactional
  public ProjetoDTO criar(ProjetoRequest request) {
    Projeto projeto = new Projeto();
    projeto.setNome(request.nome());
    projeto.setDescricao(request.descricao());
    return projetoMapper.paraDTO(projetoRepository.save(projeto));
  }

  @Transactional
  public ProjetoDTO editar(UUID id, ProjetoRequest request) {
    Projeto projeto = buscarEntidade(id);
    projeto.setNome(request.nome());
    projeto.setDescricao(request.descricao());
    projeto.setAtualizadoEm(Instant.now());
    return projetoMapper.paraDTO(projetoRepository.save(projeto));
  }

  /** RN-005: exclusão bloqueada se houver tarefas ativas vinculadas ao projeto. */
  @Transactional
  public void excluir(UUID id) {
    Projeto projeto = buscarEntidade(id);
    if (verificadorDeTarefasAtivas.existemTarefasNoProjeto(id)) {
      throw new RegraDeNegocioException(
          "Não é possível excluir o projeto '%s': há tarefas ativas vinculadas. Migre as tarefas antes."
              .formatted(projeto.getNome()));
    }
    projetoRepository.delete(projeto);
  }

  @Transactional
  public void definirWorkflowAtivo(UUID projetoId, UUID workflowId) {
    Projeto projeto = buscarEntidade(projetoId);
    projeto.setWorkflowAtivoId(workflowId);
    projetoRepository.save(projeto);
  }

  private Projeto buscarEntidade(UUID id) {
    return projetoRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado: " + id));
  }
}
