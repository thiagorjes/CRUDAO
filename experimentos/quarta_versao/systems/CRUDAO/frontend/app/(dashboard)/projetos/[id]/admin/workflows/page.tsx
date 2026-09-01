"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import WorkflowsList from "@/components/admin/WorkflowsList";
import type { Workflow } from "@/lib/types";

export default function WorkflowsAdminPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    const carregar = async () => {
      try {
        setLoading(true);
        const res = await fetch(`/api/admin/workflows?projetoId=${projetoId}`);
        if (!res.ok) {
          const err = await res.json();
          throw new Error(err.message || "Erro ao carregar workflows");
        }
        setWorkflows(await res.json());
        setErro(null);
      } catch (e) {
        setErro(e instanceof Error ? e.message : "Erro ao carregar workflows");
      } finally {
        setLoading(false);
      }
    };
    carregar();
  }, [projetoId]);

  if (loading) {
    return <div className="skeleton" style={{ height: 16, width: "80%" }} />;
  }

  return (
    <div>
      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "var(--space-md)" }}>
          {erro}
        </div>
      )}
      <WorkflowsList projetoId={projetoId} workflows={workflows} onRefresh={() => window.location.reload()} />
    </div>
  );
}
