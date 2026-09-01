"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import type { TarefaDetalhe, AuditoriaEntry } from "@/lib/types";
import {
  obterTarefaDetalhe,
  obterAuditoria,
  editarTarefa,
  adicionarObservador,
  removerObservador,
  obterUsuariosProjeto,
} from "@/lib/api/tarefa";
import { excluirTarefa, marcarImpedimento, desmarcarImpedimento } from "@/lib/api/board";
import LeadTimePanel from "@/components/LeadTimePanel";
import AuditoriaPanel from "@/components/AuditoriaPanel";
import EditarTarefaForm from "@/components/EditarTarefaForm";
import ObservadoresPanel from "@/components/ObservadoresPanel";
import ConfirmarExclusaoModal from "@/components/board/ConfirmarExclusaoModal";

/** TL-04 — Detalhe da Tarefa: drawer sobre o board. */
export default function TarefaDetalhePage() {
  const params = useParams();
  const router = useRouter();
  const tarefaId = params.tarefaId as string;
  const projetoId = params.id as string;

  const [tarefa, setTarefa] = useState<TarefaDetalhe | null>(null);
  const [auditoria, setAuditoria] = useState<AuditoriaEntry[]>([]);
  const [usuarios, setUsuarios] = useState<{ id: string; nome: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [mostrarExclusao, setMostrarExclusao] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setLoading(true);
      const [tarefaData, auditoriaData, usuariosData] = await Promise.all([
        obterTarefaDetalhe(tarefaId),
        obterAuditoria(tarefaId),
        obterUsuariosProjeto(projetoId).catch(() => []),
      ]);
      setTarefa(tarefaData);
      setAuditoria(auditoriaData);
      setUsuarios(usuariosData);
      setErro(null);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar tarefa");
    } finally {
      setLoading(false);
    }
  }, [tarefaId, projetoId]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const fechar = () => router.push(`/projetos/${projetoId}/board`);

  const handleSalvar = async (dados: { descricaoEscopo?: string; responsavelId?: string }) => {
    if (!tarefa) return;
    setSalvando(true);
    try {
      await editarTarefa(tarefaId, { titulo: tarefa.titulo, ...dados });
      await carregar();
    } finally {
      setSalvando(false);
    }
  };

  const handleToggleImpedimento = async () => {
    if (!tarefa) return;
    setSalvando(true);
    try {
      if (tarefa.impedida) {
        await desmarcarImpedimento(tarefaId, projetoId);
      } else {
        await marcarImpedimento(tarefaId, projetoId);
      }
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao atualizar impedimento");
    } finally {
      setSalvando(false);
    }
  };

  const handleExcluir = async () => {
    await excluirTarefa(tarefaId, projetoId);
    fechar();
  };

  if (loading) {
    return (
      <div className="drawer-backdrop">
        <aside className="drawer">
          <div className="skeleton" style={{ height: 16, marginBottom: 8 }} />
          <div className="skeleton" style={{ height: 16, width: "60%" }} />
        </aside>
      </div>
    );
  }

  if (erro || !tarefa) {
    return (
      <div className="drawer-backdrop">
        <aside className="drawer">
          <div className="toast toast-error" role="alert">
            {erro || "Erro ao carregar tarefa"}
          </div>
          <button type="button" className="btn btn-outline" onClick={fechar} style={{ marginTop: "var(--space-md)" }}>
            Voltar ao board
          </button>
        </aside>
      </div>
    );
  }

  return (
    <div className="drawer-backdrop">
      <aside className="drawer" role="dialog" aria-modal="true" aria-labelledby="drawer-title">
        <div className="page-header">
          <h1 id="drawer-title" style={{ fontSize: 18 }}>
            {tarefa.titulo}
          </h1>
          <button type="button" className="btn btn-text" aria-label="Fechar" onClick={fechar}>
            ✕ Fechar
          </button>
        </div>

        <div className="row row--between">
          <span className="text-secondary">
            {tarefa.raiaNome} · Criada por {tarefa.criadoPorNome} em{" "}
            {new Date(tarefa.criadoEm).toLocaleDateString("pt-BR")}
          </span>
          <button type="button" className="btn btn-danger" onClick={() => setMostrarExclusao(true)}>
            Excluir
          </button>
        </div>

        <EditarTarefaForm
          tarefa={tarefa}
          usuariosDisponiveis={usuarios}
          onSalvar={handleSalvar}
          onToggleImpedimento={handleToggleImpedimento}
          loading={salvando}
        />

        <LeadTimePanel etapas={tarefa.historicoEtapas} tempoImpedimentoTotalSegundos={tarefa.tempoImpedimentoTotalSegundos} />

        <ObservadoresPanel
          observadores={tarefa.observadores}
          usuariosDisponiveis={usuarios}
          onAdicionar={async (usuarioId) => {
            await adicionarObservador(tarefaId, usuarioId);
            await carregar();
          }}
          onRemover={async (usuarioId) => {
            await removerObservador(tarefaId, usuarioId);
            await carregar();
          }}
        />

        <AuditoriaPanel entradas={auditoria} />
      </aside>

      {mostrarExclusao && (
        <ConfirmarExclusaoModal
          tituloTarefa={tarefa.titulo}
          onConfirmar={handleExcluir}
          onCancelar={() => setMostrarExclusao(false)}
        />
      )}
    </div>
  );
}
