"use client";

import type { AuditoriaEntry } from "@/lib/types";

interface AuditoriaPanelProps {
  entradas: AuditoriaEntry[];
}

export default function AuditoriaPanel({ entradas }: AuditoriaPanelProps) {
  const formatarData = (iso: string): string => {
    const data = new Date(iso);
    return data.toLocaleString("pt-BR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const obterIconeTipo = (tipo: string): string => {
    const mapa: Record<string, string> = {
      CRIACAO: "✨",
      MOVIDA: "➡️",
      IMPEDIDA: "⚠️",
      DESIMPEDIDA: "✓",
      EDITADA: "✏️",
      EXCLUIDA: "🗑️",
      OBSERVADOR_ADICIONADO: "👁️",
      OBSERVADOR_REMOVIDO: "🚫",
    };
    return mapa[tipo] || "📝";
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4">
      <h3 className="text-sm font-semibold text-gray-900 mb-4">
        Histórico de Auditoria
      </h3>

      {entradas.length === 0 ? (
        <p className="text-sm text-gray-500 text-center py-6">
          Nenhum evento registrado
        </p>
      ) : (
        <div className="space-y-4">
          {entradas.map((entrada) => (
            <div
              key={entrada.id}
              className="flex gap-3 pb-4 border-b border-gray-200 last:border-0"
            >
              {/* Ícone */}
              <div className="flex-shrink-0 text-lg">
                {obterIconeTipo(entrada.tipo)}
              </div>

              {/* Conteúdo */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2 mb-1">
                  <p className="text-sm font-medium text-gray-900">
                    {entrada.descricao}
                  </p>
                  <span className="text-xs text-gray-500 whitespace-nowrap">
                    {formatarData(entrada.timestamp)}
                  </span>
                </div>
                <p className="text-xs text-gray-600">
                  Por: <span className="font-medium">{entrada.usuarioNome}</span>
                </p>

                {/* Mudanças de dados (se houver) */}
                {entrada.dadosAntigos || entrada.dadosNovos ? (
                  <div className="mt-2 text-xs bg-gray-50 p-2 rounded border border-gray-200 font-mono text-gray-700">
                    {entrada.dadosAntigos && (
                      <div className="text-red-600">
                        De: {JSON.stringify(entrada.dadosAntigos)}
                      </div>
                    )}
                    {entrada.dadosNovos && (
                      <div className="text-green-600">
                        Para: {JSON.stringify(entrada.dadosNovos)}
                      </div>
                    )}
                  </div>
                ) : null}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
