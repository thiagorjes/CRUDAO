import { obterMe } from "@/lib/me";
import { env } from "@/lib/env";
import { Sidebar } from "@/components/Sidebar";
import { Topbar } from "@/components/Topbar";

/** Shell de navegação (RF-014) — base para todas as telas autenticadas do Epic 07. */
export default async function ShellLayout({ children }: { children: React.ReactNode }) {
  const usuario = await obterMe();
  const admin = usuario.projetos.some((p) => p.papeis.includes("admin"));

  return (
    <div className="app-shell">
      <Sidebar mostrarAdmin={admin} />
      <Topbar
        nome={usuario.nome}
        usuarioId={usuario.id}
        backendPublicUrl={env.publicBackendUrl()}
      />
      <main className="main">{children}</main>
    </div>
  );
}
