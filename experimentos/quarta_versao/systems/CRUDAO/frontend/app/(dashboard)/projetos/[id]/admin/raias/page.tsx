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
        setRaias(await res.json());
        setErro(null);
      } catch (e) {
        setErro(e instanceof Error ? e.message : "Erro ao carregar raias");
      } finally {
        setLoading(false);
      }
    };
    carregar();
  }, [projetoId]);

  if (loading) {
    return <div className="skeleton" style={{ height: 16, width: "80%" }} />;
  }

  if (erro) {
    return (
      <div className="toast toast-error" role="alert">
        {erro}
      </div>
    );
  }

  return <RaiasList projetoId={projetoId} raias={raias} onRefresh={() => window.location.reload()} />;
}
