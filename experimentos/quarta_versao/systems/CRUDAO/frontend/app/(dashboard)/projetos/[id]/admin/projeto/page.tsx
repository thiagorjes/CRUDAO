"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import ProjetoAdminForm from "@/components/admin/ProjetoAdminForm";
import type { ProjtoDetalhe } from "@/lib/types";

export default function ProjetoAdminPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [projeto, setProjeto] = useState<ProjtoDetalhe | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    const carregar = async () => {
      try {
        setLoading(true);
        const res = await fetch(`/api/admin/projeto/${projetoId}`);
        if (!res.ok) {
          const err = await res.json();
          throw new Error(err.message || "Erro ao carregar projeto");
        }
        const data = await res.json();
        setProjeto(data);
        setErro(null);
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao carregar projeto";
        setErro(msg);
      } finally {
        setLoading(false);
      }
    };

    carregar();
  }, [projetoId]);

  if (loading) {
    return <div className="text-center text-gray-600">Carregando...</div>;
  }

  if (erro || !projeto) {
    return <div className="text-center text-red-600">{erro || "Erro ao carregar projeto"}</div>;
  }

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Informações do Projeto</h2>
        <ProjetoAdminForm projeto={projeto} projetoId={projetoId} />
      </div>
    </div>
  );
}
