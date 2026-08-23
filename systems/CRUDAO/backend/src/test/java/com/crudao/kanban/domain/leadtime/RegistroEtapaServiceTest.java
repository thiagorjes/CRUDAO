package com.crudao.kanban.domain.leadtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.workflow.Etapa;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistroEtapaServiceTest {

  @Mock private RegistroEtapaRepository registroEtapaRepository;
  @Mock private ImpedimentoRepository impedimentoRepository;

  private RegistroEtapaService service;
  private Tarefa tarefa;
  private Etapa etapa;

  @BeforeEach
  void setUp() {
    service = new RegistroEtapaService(registroEtapaRepository, impedimentoRepository);
    tarefa = new Tarefa();
    tarefa.setId(UUID.randomUUID());
    etapa = new Etapa();
    etapa.setId(UUID.randomUUID());

    lenient()
        .when(registroEtapaRepository.save(any(RegistroEtapa.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    lenient()
        .when(impedimentoRepository.save(any(Impedimento.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void deveAbrirRegistroComEntradaAgoraESaidaNula() {
    RegistroEtapa registro = service.abrirRegistro(tarefa, etapa);

    assertThat(registro.getTarefa()).isEqualTo(tarefa);
    assertThat(registro.getEtapa()).isEqualTo(etapa);
    assertThat(registro.getEntradaEm()).isNotNull();
    assertThat(registro.getSaidaEm()).isNull();
    assertThat(registro.getTempoImpedimentoSegundos()).isZero();
  }

  @Test
  void deveFecharRegistroAtualDefinindoSaida() {
    RegistroEtapa registroAberto = registroAberto(Instant.now().minusSeconds(60));
    when(registroEtapaRepository.findByTarefaIdAndSaidaEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(registroAberto));
    when(impedimentoRepository.findByRegistroEtapaIdAndFimEmIsNull(registroAberto.getId()))
        .thenReturn(Optional.empty());

    service.fecharRegistroAtual(tarefa);

    assertThat(registroAberto.getSaidaEm()).isNotNull();
  }

  @Test
  void naoDeveFalharAoFecharQuandoNaoHaRegistroAberto() {
    when(registroEtapaRepository.findByTarefaIdAndSaidaEmIsNull(tarefa.getId()))
        .thenReturn(Optional.empty());

    service.fecharRegistroAtual(tarefa);

    verify(registroEtapaRepository, never()).save(any());
  }

  @Test
  void deveAbrirImpedimentoVinculadoAoRegistroAtual() {
    RegistroEtapa registroAberto = registroAberto(Instant.now());
    when(registroEtapaRepository.findByTarefaIdAndSaidaEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(registroAberto));
    when(impedimentoRepository.findByRegistroEtapaIdAndFimEmIsNull(registroAberto.getId()))
        .thenReturn(Optional.empty());

    service.abrirImpedimento(tarefa, "bloqueio externo");

    ArgumentCaptor<Impedimento> captor = ArgumentCaptor.forClass(Impedimento.class);
    verify(impedimentoRepository).save(captor.capture());
    assertThat(captor.getValue().getRegistroEtapa()).isEqualTo(registroAberto);
    assertThat(captor.getValue().getMotivo()).isEqualTo("bloqueio externo");
    assertThat(captor.getValue().getFimEm()).isNull();
  }

  @Test
  void naoDeveAbrirSegundoImpedimentoSeJaHouverUmEmAberto() {
    RegistroEtapa registroAberto = registroAberto(Instant.now());
    Impedimento impedimentoExistente = new Impedimento();
    when(registroEtapaRepository.findByTarefaIdAndSaidaEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(registroAberto));
    when(impedimentoRepository.findByRegistroEtapaIdAndFimEmIsNull(registroAberto.getId()))
        .thenReturn(Optional.of(impedimentoExistente));

    service.abrirImpedimento(tarefa, "motivo");

    verify(impedimentoRepository, never()).save(any(Impedimento.class));
  }

  @Test
  void deveSomarTempoImpedidoAoRegistroAoFecharImpedimento() {
    RegistroEtapa registroAberto = registroAberto(Instant.now().minusSeconds(300));
    Impedimento impedimentoAberto = new Impedimento();
    impedimentoAberto.setId(UUID.randomUUID());
    impedimentoAberto.setInicioEm(Instant.now().minusSeconds(100));

    when(registroEtapaRepository.findByTarefaIdAndSaidaEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(registroAberto));
    when(impedimentoRepository.findByRegistroEtapaIdAndFimEmIsNull(registroAberto.getId()))
        .thenReturn(Optional.of(impedimentoAberto));

    service.fecharImpedimentoAtual(tarefa);

    assertThat(impedimentoAberto.getFimEm()).isNotNull();
    assertThat(registroAberto.getTempoImpedimentoSegundos()).isGreaterThanOrEqualTo(99);
  }

  @Test
  void deveAcumularMultiplosPeriodosDeImpedimentoIntercaladosNaMesmaEtapa() {
    RegistroEtapa registro = registroAberto(Instant.now().minusSeconds(1000));
    when(registroEtapaRepository.findByTarefaIdAndSaidaEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(registro));

    // Primeiro ciclo de impedimento: 100s.
    Impedimento primeiro = new Impedimento();
    primeiro.setId(UUID.randomUUID());
    primeiro.setInicioEm(Instant.now().minusSeconds(500));
    when(impedimentoRepository.findByRegistroEtapaIdAndFimEmIsNull(registro.getId()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(primeiro));
    service.abrirImpedimento(tarefa, null);
    service.fecharImpedimentoAtual(tarefa);
    long apósPrimeiro = registro.getTempoImpedimentoSegundos();

    // Segundo ciclo de impedimento: mais 50s.
    Impedimento segundo = new Impedimento();
    segundo.setId(UUID.randomUUID());
    segundo.setInicioEm(Instant.now().minusSeconds(50));
    when(impedimentoRepository.findByRegistroEtapaIdAndFimEmIsNull(registro.getId()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(segundo));
    service.abrirImpedimento(tarefa, null);
    service.fecharImpedimentoAtual(tarefa);

    assertThat(apósPrimeiro).isGreaterThanOrEqualTo(499);
    assertThat(registro.getTempoImpedimentoSegundos()).isGreaterThan(apósPrimeiro);
    // 2 aberturas + 2 fechamentos = 4 saves ao longo dos dois ciclos de impedimento.
    verify(impedimentoRepository, times(4)).save(any(Impedimento.class));
  }

  private RegistroEtapa registroAberto(Instant entradaEm) {
    RegistroEtapa registro = new RegistroEtapa();
    registro.setId(UUID.randomUUID());
    registro.setTarefa(tarefa);
    registro.setEtapa(etapa);
    registro.setEntradaEm(entradaEm);
    return registro;
  }
}
