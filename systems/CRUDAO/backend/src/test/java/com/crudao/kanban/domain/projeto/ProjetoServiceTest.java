package com.crudao.kanban.domain.projeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.common.VerificadorDeTarefasAtivas;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.security.AutorizacaoProjetoService;
import com.crudao.kanban.security.UsuarioContexto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** TASK-01.3 — toggles (RF-016) e finalização de projeto (RN-015). */
@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

  @Mock private ProjetoRepository projetoRepository;
  @Mock private ConfiguracaoProjetoRepository configuracaoProjetoRepository;
  @Mock private VerificadorDeTarefasAtivas verificadorDeTarefasAtivas;
  @Mock private AutorizacaoProjetoService autorizacaoProjetoService;
  @Mock private UsuarioContexto usuarioContexto;

  private ProjetoService projetoService;
  private Projeto projetoA;
  private Projeto projetoB;
  private ConfiguracaoProjeto configuracaoA;
  private ConfiguracaoProjeto configuracaoB;

  @BeforeEach
  void setUp() {
    ProjetoMapper mapper = new ProjetoMapperImpl();
    projetoService =
        new ProjetoService(
            projetoRepository,
            configuracaoProjetoRepository,
            mapper,
            verificadorDeTarefasAtivas,
            autorizacaoProjetoService,
            usuarioContexto);

    projetoA = new Projeto();
    projetoA.setId(UUID.randomUUID());
    projetoB = new Projeto();
    projetoB.setId(UUID.randomUUID());

    configuracaoA = new ConfiguracaoProjeto();
    configuracaoA.setProjetoId(projetoA.getId());
    configuracaoB = new ConfiguracaoProjeto();
    configuracaoB.setProjetoId(projetoB.getId());

    lenient().when(projetoRepository.findById(projetoA.getId())).thenReturn(Optional.of(projetoA));
    lenient().when(projetoRepository.findById(projetoB.getId())).thenReturn(Optional.of(projetoB));
    lenient()
        .when(configuracaoProjetoRepository.findById(projetoA.getId()))
        .thenReturn(Optional.of(configuracaoA));
    lenient()
        .when(configuracaoProjetoRepository.findById(projetoB.getId()))
        .thenReturn(Optional.of(configuracaoB));
    lenient()
        .when(configuracaoProjetoRepository.save(any(ConfiguracaoProjeto.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    lenient()
        .when(projetoRepository.save(any(Projeto.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    lenient().when(usuarioContexto.usuarioAtual()).thenReturn(new Usuario());
  }

  @Test
  void deveLigarToggleDeUmProjetoSemAfetarOutro() {
    doNothing().when(autorizacaoProjetoService).exigirPermissao(any(), any(), any());

    projetoService.atualizarConfiguracao(
        projetoA.getId(), new ConfiguracaoProjetoDTO(true, true, true));

    assertThat(configuracaoA.isDevPodeExcluirTarefa()).isTrue();
    assertThat(configuracaoB.isDevPodeExcluirTarefa()).isFalse();
    assertThat(configuracaoB.isDevPodeEditarTarefaIniciada()).isFalse();
    assertThat(configuracaoB.isGestorVeBoard()).isFalse();
  }

  @Test
  void finalizarDeveMarcarDataFinalizacao() {
    doNothing().when(autorizacaoProjetoService).exigirPermissao(any(), any(), any());

    projetoService.finalizar(projetoA.getId());

    assertThat(projetoA.getDataFinalizacao()).isNotNull();
  }

  @Test
  void editarDevePropagarBloqueioDeProjetoFinalizado() {
    doThrow(new RegraDeNegocioException("Projeto finalizado."))
        .when(autorizacaoProjetoService)
        .exigirPermissao(any(), any(), any());

    assertThatThrownBy(
            () -> projetoService.editar(projetoA.getId(), new ProjetoRequest("novo", null)))
        .isInstanceOf(RegraDeNegocioException.class);
  }

  @Test
  void reabrirDeveLimparDataFinalizacao() {
    projetoA.setDataFinalizacao(java.time.Instant.now());
    doNothing().when(autorizacaoProjetoService).exigirPermissaoParaReabertura(any(), any());

    projetoService.reabrir(projetoA.getId());

    assertThat(projetoA.getDataFinalizacao()).isNull();
  }

  @Test
  void reabrirDeveUsarExigirPermissaoParaReaberturaENaoOMetodoGeral_TASK_01_3() {
    // exigirPermissao (geral) bloqueia incondicionalmente projeto finalizado — se reabrir chamasse
    // esse método em vez de exigirPermissaoParaReabertura, nunca seria possível reabrir.
    projetoA.setDataFinalizacao(java.time.Instant.now());
    lenient()
        .doThrow(new RegraDeNegocioException("não deveria ser chamado"))
        .when(autorizacaoProjetoService)
        .exigirPermissao(any(), any(), any());
    doNothing().when(autorizacaoProjetoService).exigirPermissaoParaReabertura(any(), any());

    assertThatCode(() -> projetoService.reabrir(projetoA.getId())).doesNotThrowAnyException();
  }
}
