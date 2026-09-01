import type { Dashboard } from "@/lib/types";
import { formatarDuracao } from "@/lib/format";

interface DashboardViewProps {
  dashboard: Dashboard;
}

/**
 * TL-07 — Dashboard (docs/design/.../tl-07-dashboard.html).
 *
 * Limitação de contrato (registrada em I4 da review de fidelidade): o backend
 * (`GET /api/projetos/{id}/dashboard`) não expõe um agregado "geral" nem filtro de período —
 * só a série por etapa e o total de tarefas consideradas. "Lead-time médio geral" e "Tempo médio
 * de impedimento" abaixo são a média simples entre as etapas (aproximação, não um agregado real
 * ponderado pelo backend); não há seletor de período nesta versão.
 */
export default function DashboardView({ dashboard }: DashboardViewProps) {
  const etapas = dashboard.leadTimeMedioPorEtapa ?? [];

  const media = (valores: number[]) =>
    valores.length === 0 ? 0 : valores.reduce((a, b) => a + b, 0) / valores.length;

  const leadTimeMedioGeral = media(etapas.map((e) => e.leadTimeMedioSegundos));
  const impedimentoMedioGeral = media(etapas.map((e) => e.tempoImpedimentoMedioSegundos));
  const maxLeadTime = Math.max(1, ...etapas.map((e) => e.leadTimeMedioSegundos));

  return (
    <div>
      <div className="page-header">
        <h1>Dashboard</h1>
      </div>

      {etapas.length === 0 ? (
        <div className="empty-state">
          Ainda não há movimentações suficientes para calcular lead-time neste projeto.
        </div>
      ) : (
        <>
          <section aria-label="KPIs">
            <div className="kpi-grid">
              <div className="card kpi-card">
                <div className="kpi-value">{formatarDuracao(leadTimeMedioGeral)}</div>
                <div className="kpi-label">Lead-time médio geral</div>
              </div>
              <div className="card kpi-card">
                <div className="kpi-value">{formatarDuracao(impedimentoMedioGeral)}</div>
                <div className="kpi-label">Tempo médio de impedimento</div>
              </div>
              <div className="card kpi-card">
                <div className="kpi-value">{dashboard.totalTarefasConsideradas}</div>
                <div className="kpi-label">Tarefas consideradas</div>
              </div>
            </div>

            <div className="card">
              <h2 style={{ fontSize: 14 }}>Lead-time médio por etapa</h2>
              <div
                className="bar-chart"
                role="img"
                aria-label={`Gráfico de lead-time médio por etapa: ${etapas
                  .map((e) => `${e.etapaNome} ${formatarDuracao(e.leadTimeMedioSegundos)}`)
                  .join(", ")}`}
              >
                {etapas.map((e) => (
                  <div key={e.etapaId} className="bar-wrap">
                    <div
                      className="bar"
                      style={{ height: `${Math.max(4, (e.leadTimeMedioSegundos / maxLeadTime) * 140)}px` }}
                    />
                    {e.etapaNome}
                  </div>
                ))}
              </div>
            </div>
          </section>

          <table style={{ marginTop: "var(--space-lg)" }}>
            <thead>
              <tr>
                <th>Etapa</th>
                <th className="text-right">Lead-time médio</th>
                <th className="text-right">Impedimento médio</th>
              </tr>
            </thead>
            <tbody>
              {etapas.map((etapa) => (
                <tr key={etapa.etapaId}>
                  <td>{etapa.etapaNome}</td>
                  <td className="text-right">{formatarDuracao(etapa.leadTimeMedioSegundos)}</td>
                  <td className="text-right">{formatarDuracao(etapa.tempoImpedimentoMedioSegundos)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}
