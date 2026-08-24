package com.crudao.kanban.domain.leadtime;

import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.workflow.Etapa;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre e fecha {@link RegistroEtapa} (permanência em etapa) e {@link Impedimento} (período impedido
 * dentro de uma permanência) — RF-006, RN-001, RN-002.
 */
@Service
@RequiredArgsConstructor
public class RegistroEtapaService {

  private final RegistroEtapaRepository registroEtapaRepository;
  private final ImpedimentoRepository impedimentoRepository;

  /** Abre um novo registro de permanência da tarefa na etapa informada. */
  @Transactional
  public RegistroEtapa abrirRegistro(Tarefa tarefa, Etapa etapa) {
    RegistroEtapa registro = new RegistroEtapa();
    registro.setTarefa(tarefa);
    registro.setEtapa(etapa);
    registro.setEntradaEm(Instant.now());
    return registroEtapaRepository.save(registro);
  }

  /**
   * Fecha o registro de permanência atual (etapa em andamento) da tarefa, se houver. Se a tarefa
   * estava impedida, fecha também o {@link Impedimento} em aberto, somando o tempo ao registro
   * antes de fechá-lo (RN-002).
   */
  @Transactional
  public void fecharRegistroAtual(Tarefa tarefa) {
    registroEtapaRepository
        .findByTarefaIdAndSaidaEmIsNull(tarefa.getId())
        .ifPresent(
            registro -> {
              fecharImpedimentoAberto(registro, Instant.now());
              registro.setSaidaEm(Instant.now());
              registroEtapaRepository.save(registro);
            });
  }

  /**
   * Abre um {@link Impedimento} vinculado ao registro de permanência atual da tarefa. Idempotente:
   * se já houver um impedimento em aberto para o registro atual, não cria outro.
   */
  @Transactional
  public void abrirImpedimento(Tarefa tarefa, String motivo) {
    registroEtapaRepository
        .findByTarefaIdAndSaidaEmIsNull(tarefa.getId())
        .ifPresent(
            registro -> {
              if (impedimentoRepository
                  .findByRegistroEtapaIdAndFimEmIsNull(registro.getId())
                  .isPresent()) {
                return;
              }
              Impedimento impedimento = new Impedimento();
              impedimento.setTarefa(tarefa);
              impedimento.setRegistroEtapa(registro);
              impedimento.setInicioEm(Instant.now());
              impedimento.setMotivo(motivo);
              impedimentoRepository.save(impedimento);
            });
  }

  /**
   * Fecha o {@link Impedimento} em aberto do registro de permanência atual da tarefa, somando a
   * duração ao {@code tempoImpedimentoSegundos} do registro (RN-002). Múltiplos ciclos de
   * marcar/desmarcar dentro da mesma etapa são acumulados corretamente.
   */
  @Transactional
  public void fecharImpedimentoAtual(Tarefa tarefa) {
    registroEtapaRepository
        .findByTarefaIdAndSaidaEmIsNull(tarefa.getId())
        .ifPresent(registro -> fecharImpedimentoAberto(registro, Instant.now()));
  }

  private void fecharImpedimentoAberto(RegistroEtapa registro, Instant agora) {
    impedimentoRepository
        .findByRegistroEtapaIdAndFimEmIsNull(registro.getId())
        .ifPresent(
            impedimento -> {
              impedimento.setFimEm(agora);
              long segundos = Duration.between(impedimento.getInicioEm(), agora).getSeconds();
              registro.setTempoImpedimentoSegundos(
                  registro.getTempoImpedimentoSegundos() + segundos);
              impedimentoRepository.save(impedimento);
              registroEtapaRepository.save(registro);
            });
  }

  @Transactional(readOnly = true)
  public List<RegistroEtapa> listarPorTarefa(UUID tarefaId) {
    return registroEtapaRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId);
  }
}
