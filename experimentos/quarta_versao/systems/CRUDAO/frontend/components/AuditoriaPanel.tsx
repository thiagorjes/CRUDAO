import type { AuditoriaEntry } from "@/lib/types";

interface AuditoriaPanelProps {
  entradas: AuditoriaEntry[];
}

const ROTULO_CAMPO: Record<string, string> = {
  titulo: "título",
  descricaoEscopo: "descrição",
  responsavelId: "responsável",
  etapaAtualId: "etapa",
  impedida: "impedimento",
};

function rotuloCampo(campo: string): string {
  return ROTULO_CAMPO[campo] ?? campo;
}

/** TL-04 — histórico de auditoria (RF-017). */
export default function AuditoriaPanel({ entradas }: AuditoriaPanelProps) {
  return (
    <section aria-label="Histórico de auditoria" style={{ marginTop: "var(--space-lg)" }}>
      <h2 style={{ fontSize: 14 }}>Histórico</h2>
      {entradas.length === 0 ? (
        <p className="text-secondary">Nenhuma alteração registrada ainda.</p>
      ) : (
        entradas.map((e) => (
          <div key={e.id} className="history-item">
            <strong>{e.autorNome}</strong> alterou {rotuloCampo(e.campo)}:{" "}
            {e.valorAnterior ?? "—"} → {e.valorNovo ?? "—"}{" "}
            <span className="text-secondary">{new Date(e.dataHora).toLocaleString("pt-BR")}</span>
          </div>
        ))
      )}
    </section>
  );
}
