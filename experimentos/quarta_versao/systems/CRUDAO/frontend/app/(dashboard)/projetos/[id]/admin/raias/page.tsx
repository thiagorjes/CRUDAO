"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import RaiasList from "@/components/admin/RaiasList";
import type { Raia } from "@/lib/types";

export default function RaiasAdminPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [raias, setRaias] = useState<Raia[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    const carregar = async () => {
      try {
        setLoading(true);
        const res = await fetch(`/api/admin/raias?projetoId=${projetoId}`);
        if (!res.ok) {
          const err = await res.json();
          throw new Error(err.message || "Erro ao carregar raias");
        }
        const data = await res.json();
        setRaias(data);
        setErro(null);
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao carregar raias";
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

  return (
    <div className="space-y-6">
      {erro && <div className="text-red-600 p-4 bg-red-50 rounded border border-red-200">{erro}</div>}
      <RaiasList projetoId={projetoId} raias={raias} onRefresh={() => window.location.reload()} />
    </div>
  );
}
