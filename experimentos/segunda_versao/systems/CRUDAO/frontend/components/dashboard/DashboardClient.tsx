import { DashboardResponse } from "@/lib/dashboard";
import { formatarDuracao, ordenarPorLeadTimeDesc } from "@/lib/dashboard-logic";

/**
 * TL — Dashboard de lead-time (RF-007, TASK-07.6). Puramente apresentacional (sem interação/estado),
 * dados já vêm agregados do backend (`DashboardService`, TASK-06.1).
 */
export function DashboardClient({ dashboard }: { dashboard: DashboardResponse }) {
  const etapas = ordenarPorLeadTimeDesc(dashboard.leadTimeMedioPorEtapa);

  return (
    <div className="card">
      <p className="text-secondary" style={{ marginBottom: "var(--space-sm)" }}>
        {dashboard.totalTarefasConsideradas} tarefa(s) considerada(s) no cálculo.
      </p>

      {etapas.length === 0 ? (
        <div className="empty-state">Nenhum dado de lead-time disponível ainda para este projeto.</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Etapa</th>
              <th>Lead-time médio</th>
              <th>Tempo médio de impedimento</th>
            </tr>
          </thead>
          <tbody>
            {etapas.map((etapa) => (
              <tr key={etapa.etapaId}>
                <td>{etapa.etapaNome}</td>
                <td>{formatarDuracao(etapa.leadTimeMedioSegundos)}</td>
                <td>{formatarDuracao(etapa.tempoImpedimentoMedioSegundos)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
