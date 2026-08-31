"use client";

import type { LeadTimeEtapa } from "@/lib/types";

interface LeadTimePanelProps {
  etapas: LeadTimeEtapa[];
  tempoTotal: number;
  tempoImpedimento: number;
}

export default function LeadTimePanel({
  etapas,
  tempoTotal,
  tempoImpedimento,
}: LeadTimePanelProps) {
  const formatarTempo = (ms: number): string => {
    if (ms === 0) return "0s";
    const segundos = Math.floor(ms / 1000);
    const minutos = Math.floor(segundos / 60);
    const horas = Math.floor(minutos / 60);
    const dias = Math.floor(horas / 24);

    if (dias > 0) return `${dias}d ${horas % 24}h`;
    if (horas > 0) return `${horas}h ${minutos % 60}m`;
    if (minutos > 0) return `${minutos}m ${segundos % 60}s`;
    return `${segundos}s`;
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4 space-y-4">
      <div>
        <h3 className="text-sm font-semibold text-gray-900 mb-3">
          Lead-Time por Etapa
        </h3>
        <div className="space-y-2">
          {etapas.map((etapa) => (
            <div
              key={etapa.etapaId}
              className="flex items-center justify-between px-3 py-2 bg-gray-50 rounded border border-gray-200"
            >
              <span className="text-sm text-gray-700">{etapa.etapaNome}</span>
              <span className="text-sm font-medium text-gray-900">
                {formatarTempo(etapa.duracao)}
              </span>
            </div>
          ))}
        </div>
      </div>

      <hr className="border-gray-200" />

      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-xs text-gray-500 uppercase font-semibold">
            Lead-Time Total
          </p>
          <p className="text-lg font-bold text-blue-600 mt-1">
            {formatarTempo(tempoTotal)}
          </p>
        </div>

        <div>
          <p className="text-xs text-gray-500 uppercase font-semibold">
            Tempo Impedido
          </p>
          <p className="text-lg font-bold text-yellow-600 mt-1">
            {formatarTempo(tempoImpedimento)}
          </p>
        </div>
      </div>
    </div>
  );
}
