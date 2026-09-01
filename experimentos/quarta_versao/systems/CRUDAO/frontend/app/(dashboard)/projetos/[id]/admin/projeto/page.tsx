"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import ProjetoAdminForm from "@/components/admin/ProjetoAdminForm";
import type { ProjetoResumo } from "@/lib/types";

export default function ProjetoAdminPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [projeto, setProjeto] = useState<ProjetoResumo | null>(null);
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
        setProjeto(await res.json());
        setErro(null);
      } catch (e) {
        setErro(e instanceof Error ? e.message : "Erro ao carregar projeto");
      } finally {
        setLoading(false);
      }
    };
    carregar();
  }, [projetoId]);

  if (loading) {
    return (
      <div>
        <div className="skeleton" style={{ height: 16, marginBottom: 8 }} />
        <div className="skeleton" style={{ height: 16, width: "80%" }} />
      </div>
    );
  }

  if (erro || !projeto) {
    return (
      <div className="toast toast-error" role="alert">
        {erro || "Erro ao carregar projeto"}
      </div>
    );
  }

  return <ProjetoAdminForm projeto={projeto} projetoId={projetoId} />;
}
