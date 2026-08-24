package com.crudao.kanban.domain.projeto;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.common.VerificadorDeTarefasAtivas;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.security.AutorizacaoProjetoService;
import com.crudao.kanban.security.UsuarioContexto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjetoService {

  private static final String PERMISSAO = "projeto:gerenciar";

  private final ProjetoRepository projetoRepository;
  private final ConfiguracaoProjetoRepository configuracaoProjetoRepository;
  private final ProjetoMapper projetoMapper;
  private final VerificadorDeTarefasAtivas verificadorDeTarefasAtivas;
  private final AutorizacaoProjetoService autorizacaoProjetoService;
  private final UsuarioContexto usuarioContexto;

  @Transactional(readOnly = true)
  public List<ProjetoDTO> listar() {
    return projetoRepository.findAll().stream().map(projetoMapper::paraDTO).toList();
  }

  @Transactional(readOnly = true)
  public ProjetoDTO buscar(UUID id) {
    return projetoMapper.paraDTO(buscarEntidade(id));
  }

  /**
   * Criar projeto é ação global (não há {@code projetoId} ainda para escopar via {@link
   * AutorizacaoProjetoService}) — restrita a {@code admin} global, mesmo padrão usado para {@code
   * papel:gerenciar} (achado do code review da TASK-04.2: {@code project_admin} não pode criar
   * projetos arbitrários).
   */
  @Transactional
  public ProjetoDTO criar(ProjetoRequest request) {
    if (!usuarioContexto.usuarioAtual().isAdmin()) {
      throw new AcessoNegadoException("Apenas o admin global pode criar projetos.");
    }
    Projeto projeto = new Projeto();
    projeto.setNome(request.nome());
    projeto.setDescricao(request.descricao());
    Projeto salvo = projetoRepository.save(projeto);
    criarConfiguracaoPadrao(salvo);
    return projetoMapper.paraDTO(salvo);
  }

  /** RF-016: toggles do projeto criadas com os defaults (todos {@code false}) na criação. */
  private void criarConfiguracaoPadrao(Projeto projeto) {
    ConfiguracaoProjeto configuracao = new ConfiguracaoProjeto();
    configuracao.setProjetoId(projeto.getId());
    configuracaoProjetoRepository.save(configuracao);
  }

  @Transactional
  public ProjetoDTO editar(UUID id, ProjetoRequest request) {
    Projeto projeto = buscarEntidade(id);
    exigirPermissao(id);
    projeto.setNome(request.nome());
    projeto.setDescricao(request.descricao());
    projeto.setAtualizadoEm(Instant.now());
    return projetoMapper.paraDTO(projetoRepository.save(projeto));
  }

  /** RN-005: exclusão bloqueada se houver tarefas ativas vinculadas ao projeto. */
  @Transactional
  public void excluir(UUID id) {
    Projeto projeto = buscarEntidade(id);
    exigirPermissao(id);
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
    exigirPermissao(projetoId);
    projeto.setWorkflowAtivoId(workflowId);
    projetoRepository.save(projeto);
  }

  @Transactional(readOnly = true)
  public ConfiguracaoProjetoDTO buscarConfiguracao(UUID projetoId) {
    return paraDTO(buscarConfiguracaoEntidade(projetoId));
  }

  /** RF-016: apenas {@code projeto:gerenciar} no projeto altera os toggles. */
  @Transactional
  public ConfiguracaoProjetoDTO atualizarConfiguracao(
      UUID projetoId, ConfiguracaoProjetoDTO request) {
    buscarEntidade(projetoId);
    exigirPermissao(projetoId);
    ConfiguracaoProjeto configuracao = buscarConfiguracaoEntidade(projetoId);
    configuracao.setDevPodeExcluirTarefa(request.devPodeExcluirTarefa());
    configuracao.setDevPodeEditarTarefaIniciada(request.devPodeEditarTarefaIniciada());
    configuracao.setGestorVeBoard(request.gestorVeBoard());
    return paraDTO(configuracaoProjetoRepository.save(configuracao));
  }

  /** RN-015: torna o projeto somente leitura. Exige {@code projeto:gerenciar}. */
  @Transactional
  public void finalizar(UUID projetoId) {
    Projeto projeto = buscarEntidade(projetoId);
    exigirPermissao(projetoId);
    projeto.setDataFinalizacao(Instant.now());
    projetoRepository.save(projeto);
  }

  /**
   * Reabre um projeto finalizado. Usa {@link
   * AutorizacaoProjetoService#exigirPermissaoParaReabertura} — a única chamada que não bloqueia
   * sobre projeto finalizado (G-RBAC-08); demais métodos deste Service usam {@link
   * #exigirPermissao} normal, que bloqueia incondicionalmente.
   */
  @Transactional
  public void reabrir(UUID projetoId) {
    Projeto projeto = buscarEntidade(projetoId);
    autorizacaoProjetoService.exigirPermissaoParaReabertura(
        usuarioContexto.usuarioAtual(), projetoId);
    projeto.setDataFinalizacao(null);
    projetoRepository.save(projeto);
  }

  private ConfiguracaoProjeto buscarConfiguracaoEntidade(UUID projetoId) {
    return configuracaoProjetoRepository
        .findById(projetoId)
        .orElseThrow(
            () -> new RecursoNaoEncontradoException("Configuração não encontrada: " + projetoId));
  }

  private ConfiguracaoProjetoDTO paraDTO(ConfiguracaoProjeto configuracao) {
    return new ConfiguracaoProjetoDTO(
        configuracao.isDevPodeExcluirTarefa(),
        configuracao.isDevPodeEditarTarefaIniciada(),
        configuracao.isGestorVeBoard());
  }

  private Projeto buscarEntidade(UUID id) {
    return projetoRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado: " + id));
  }

  private void exigirPermissao(UUID projetoId) {
    Usuario usuario = usuarioContexto.usuarioAtual();
    autorizacaoProjetoService.exigirPermissao(usuario, projetoId, PERMISSAO);
  }
}
