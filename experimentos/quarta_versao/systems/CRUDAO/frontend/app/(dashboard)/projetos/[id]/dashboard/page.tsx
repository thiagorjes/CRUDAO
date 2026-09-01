"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import DashboardView from "@/components/dashboard/DashboardView";
import type { Dashboard } from "@/lib/types";

export default function DashboardPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    const carregar = async () => {
      try {
        setLoading(true);
        const res = await fetch(`/api/dashboard/${projetoId}`);
        if (!res.ok) {
          const err = await res.json();
          throw new Error(err.message || `Erro ${res.status}`);
        }
        setDashboard(await res.json());
        setErro(null);
      } catch (e) {
        setErro(e instanceof Error ? e.message : "Erro ao carregar dashboard");
      } finally {
        setLoading(false);
      }
    };
    carregar();
  }, [projetoId]);

  if (loading) {
    return (
      <div>
        <div className="page-header">
          <h1>Dashboard</h1>
        </div>
        <div className="kpi-grid">
          <div className="skeleton" style={{ height: 64 }} />
          <div className="skeleton" style={{ height: 64 }} />
          <div className="skeleton" style={{ height: 64 }} />
        </div>
      </div>
    );
  }

  if (erro || !dashboard) {
    return (
      <div>
        <div className="page-header">
          <h1>Dashboard</h1>
        </div>
        <div className="toast toast-error" role="alert">
          {erro || "Dashboard não disponível"}
        </div>
      </div>
    );
  }

  return <DashboardView dashboard={dashboard} />;
}
