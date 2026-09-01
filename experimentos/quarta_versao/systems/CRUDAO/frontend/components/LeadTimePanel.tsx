import type { LeadTimeEtapa } from "@/lib/types";
import { formatarDuracao } from "@/lib/format";

interface LeadTimePanelProps {
  etapas: LeadTimeEtapa[];
  tempoImpedimentoTotalSegundos: number;
}

/** TL-04 — resumo de lead-time por etapa (RF-006). */
export default function LeadTimePanel({ etapas, tempoImpedimentoTotalSegundos }: LeadTimePanelProps) {
  const resumo = etapas.map((e) => `${e.etapaNome}: ${formatarDuracao(e.leadTimeSegundos)}`).join(" · ");

  return (
    <div className="form-field">
      <span>Lead-time por etapa</span>
      <p className="text-secondary">
        {resumo || "Sem histórico de etapas"} · Impedimento acumulado: {formatarDuracao(tempoImpedimentoTotalSegundos)}
      </p>
    </div>
  );
}
