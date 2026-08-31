import { Suspense } from "react";
import DashboardShell from "@/components/DashboardShell";
import { obterMe } from "@/lib/me";

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // Carrega dados do usuário no servidor (RF-014: GET /api/me)
  const me = await obterMe();

  return (
    <DashboardShell me={me}>
      <Suspense fallback={<div style={{ padding: "var(--space-lg)" }}>Carregando...</div>}>
        {children}
      </Suspense>
    </DashboardShell>
  );
}
