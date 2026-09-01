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
        const data = await res.json();
        setDashboard(data);
        setErro(null);
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao carregar dashboard";
        setErro(msg);
      } finally {
        setLoading(false);
      }
    };

    carregar();
  }, [projetoId]);

  if (loading) {
    return <div className="text-center text-gray-600 p-6">Carregando dashboard...</div>;
  }

  if (erro) {
    return <div className="text-red-600 p-6 bg-red-50 rounded border border-red-200">{erro}</div>;
  }

  if (!dashboard) {
    return <div className="text-gray-600 p-6">Dashboard não disponível</div>;
  }

  return <DashboardView dashboard={dashboard} />;
}
