"use client";

import type { Dashboard } from "@/lib/types";

interface DashboardViewProps {
  dashboard: Dashboard;
}

export default function DashboardView({ dashboard }: DashboardViewProps) {
  const formatarTempo = (segundosTotais: number): string => {
    const s = Math.max(0, Math.floor(segundosTotais));
    if (s === 0) return "0s";
    const segundos = s % 60;
    const minutos = Math.floor(s / 60) % 60;
    const horas = Math.floor(s / 3600) % 24;
    const dias = Math.floor(s / 86400);

    if (dias > 0) return `${dias}d ${horas}h`;
    if (horas > 0) return `${horas}h ${minutos}m`;
    if (minutos > 0) return `${minutos}m ${segundos}s`;
    return `${segundos}s`;
  };

  const etapas = dashboard.leadTimeMedioPorEtapa ?? [];

  return (
    <div className="flex flex-col h-full bg-gray-50 p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-600 mt-1">
          Visão de gestão — lead-time e impedimento médios por etapa
          {" · "}
          {dashboard.totalTarefasConsideradas} tarefa(s) considerada(s)
        </p>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          Lead-Time e Impedimento Médios por Etapa
        </h2>
        {etapas.length > 0 ? (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 border-b border-gray-200">
                <th className="py-2 font-medium">Etapa</th>
                <th className="py-2 font-medium text-right">Lead-time médio</th>
                <th className="py-2 font-medium text-right">Impedimento médio</th>
              </tr>
            </thead>
            <tbody>
              {etapas.map((etapa) => (
                <tr key={etapa.etapaId} className="border-b border-gray-100 last:border-0">
                  <td className="py-3 font-medium text-gray-700">{etapa.etapaNome}</td>
                  <td className="py-3 text-right font-bold text-blue-600">
                    {formatarTempo(etapa.leadTimeMedioSegundos)}
                  </td>
                  <td className="py-3 text-right font-bold text-yellow-600">
                    {formatarTempo(etapa.tempoImpedimentoMedioSegundos)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="text-gray-600">Sem dados para o período.</p>
        )}
      </div>
    </div>
  );
}
