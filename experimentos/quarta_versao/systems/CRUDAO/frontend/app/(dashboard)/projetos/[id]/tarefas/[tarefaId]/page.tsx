"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import type { TarefaDetalhe, AuditoriaEntry } from "@/lib/types";
import {
  obterTarefaDetalhe,
  obterAuditoria,
  editarTarefa,
  adicionarObservador,
  removerObservador,
} from "@/lib/api/tarefa";
import LeadTimePanel from "@/components/LeadTimePanel";
import AuditoriaPanel from "@/components/AuditoriaPanel";
import EditarTarefaForm from "@/components/EditarTarefaForm";
import ObservadoresPanel from "@/components/ObservadoresPanel";

export default function TarefaDetalhePage() {
  const params = useParams();
  const router = useRouter();
  const tarefaId = params.tarefaId as string;

  const [tarefa, setTarefa] = useState<TarefaDetalhe | null>(null);
  const [auditoria, setAuditoria] = useState<AuditoriaEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  // Carregar dados iniciais
  useEffect(() => {
    const carregar = async () => {
      try {
        setLoading(true);
        const [tarefaData, auditoriaData] = await Promise.all([
          obterTarefaDetalhe(tarefaId),
          obterAuditoria(tarefaId),
        ]);
        setTarefa(tarefaData);
        setAuditoria(auditoriaData);
        setErro(null);
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao carregar tarefa";
        setErro(msg);
      } finally {
        setLoading(false);
      }
    };

    carregar();
  }, [tarefaId]);

  const handleSalvarTarefa = useCallback(
    async (dados: { titulo: string; descricaoEscopo?: string }) => {
      if (!tarefa) return;

      setSalvando(true);
      try {
        const atualizada = await editarTarefa(tarefaId, dados);
        setTarefa(atualizada);
        setToastMsg("Tarefa salva com sucesso!");
        setTimeout(() => setToastMsg(null), 3000);
      } catch (e) {
        throw e;
      } finally {
        setSalvando(false);
      }
    },
    [tarefaId, tarefa]
  );

  const handleAdicionarObservador = useCallback(
    async (usuarioId: string) => {
      if (!tarefa) return;

      try {
        await adicionarObservador(tarefaId, usuarioId);
        // Recarregar tarefa para refletir novo observador
        const atualizada = await obterTarefaDetalhe(tarefaId);
        setTarefa(atualizada);
        setToastMsg("Observador adicionado!");
        setTimeout(() => setToastMsg(null), 3000);
      } catch (e) {
        throw e;
      }
    },
    [tarefaId, tarefa]
  );

  const handleRemoverObservador = useCallback(
    async (usuarioId: string) => {
      if (!tarefa) return;

      try {
        await removerObservador(tarefaId, usuarioId);
        // Recarregar tarefa
        const atualizada = await obterTarefaDetalhe(tarefaId);
        setTarefa(atualizada);
        setToastMsg("Observador removido!");
        setTimeout(() => setToastMsg(null), 3000);
      } catch (e) {
        throw e;
      }
    },
    [tarefaId, tarefa]
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-600">Carregando tarefa...</p>
      </div>
    );
  }

  if (erro || !tarefa) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4">
        <p className="text-red-600">{erro || "Erro ao carregar tarefa"}</p>
        <button
          onClick={() => router.back()}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          Voltar
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-gray-50 p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <button
            onClick={() => router.back()}
            className="text-blue-600 hover:text-blue-700 mb-2 text-sm"
          >
            ← Voltar ao board
          </button>
          <h1 className="text-3xl font-bold text-gray-900">{tarefa.titulo}</h1>
          <p className="text-sm text-gray-600 mt-1">
            {tarefa.raiaNome} • {tarefa.etapaNome}
          </p>
        </div>
      </div>

      {/* Toast */}
      {toastMsg && (
        <div className="mb-4 p-4 bg-green-100 border border-green-400 text-green-700 rounded-lg">
          {toastMsg}
        </div>
      )}

      {/* Grid de painéis */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Coluna esquerda — Edição + Observadores */}
        <div className="lg:col-span-2 space-y-6">
          {/* Editar Tarefa */}
          <EditarTarefaForm
            tarefa={tarefa}
            onSalvar={handleSalvarTarefa}
            loading={salvando}
          />

          {/* Lead-Time */}
          <LeadTimePanel
            etapas={tarefa.leadTimePorEtapa}
            tempoTotal={tarefa.leadTimeTotal}
            tempoImpedimento={tarefa.tempoImpedimento}
          />

          {/* Auditoria */}
          <AuditoriaPanel entradas={auditoria} />
        </div>

        {/* Coluna direita — Info + Observadores */}
        <div className="space-y-6">
          {/* Infos gerais */}
          <div className="bg-white rounded-lg border border-gray-200 p-4 space-y-3">
            <h3 className="text-sm font-semibold text-gray-900 mb-3">
              Informações
            </h3>

            {tarefa.responsavelNome && (
              <div>
                <p className="text-xs text-gray-500 uppercase font-semibold">
                  Responsável
                </p>
                <p className="text-sm font-medium text-gray-900 mt-1">
                  {tarefa.responsavelNome}
                </p>
              </div>
            )}

            <div>
              <p className="text-xs text-gray-500 uppercase font-semibold">
                Criado em
              </p>
              <p className="text-sm text-gray-600 mt-1">
                {new Date(tarefa.criadaEm).toLocaleString("pt-BR")}
              </p>
            </div>

            <div>
              <p className="text-xs text-gray-500 uppercase font-semibold">
                Criado por
              </p>
              <p className="text-sm text-gray-600 mt-1">
                {tarefa.criadoPorNome}
              </p>
            </div>

            {tarefa.impedida && tarefa.impedidaDesde && (
              <div className="p-2 bg-yellow-50 rounded border border-yellow-200">
                <p className="text-xs text-yellow-700">
                  ⚠️ Impedido desde{" "}
                  {new Date(tarefa.impedidaDesde).toLocaleString("pt-BR")}
                </p>
              </div>
            )}
          </div>

          {/* Observadores */}
          <ObservadoresPanel
            observadores={tarefa.observadores}
            onAdicionar={handleAdicionarObservador}
            onRemover={handleRemoverObservador}
            usuariosDisponiveis={[]} // TODO: popular com usuários do projeto
          />
        </div>
      </div>
    </div>
  );
}
