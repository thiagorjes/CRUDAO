"use client";

import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { iniciais } from "@/lib/format";
import type { MeResponse } from "@/lib/types";
import NotificacoesSino from "@/components/notificacoes/NotificacoesSino";

interface DashboardShellProps {
  me: MeResponse;
  children?: React.ReactNode;
}

/**
 * Shell das telas internas (TL-02..TL-10) — sidebar + topbar do
 * docs/design/kanban-tarefas/prototypes/_shared.css.
 */
export default function DashboardShell({ me, children }: DashboardShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [menuAberto, setMenuAberto] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  const projetoAtualId = pathname.startsWith("/projetos/")
    ? pathname.split("/")[2]
    : undefined;
  const projetoAtual = me.projetos.find((p) => p.projetoId === projetoAtualId);
  const podeAdmin =
    !!projetoAtual &&
    (me.adminGlobal ||
      projetoAtual.papeis.some((r) => ["admin", "project_admin"].includes(r)));

  useEffect(() => {
    if (!menuAberto) return;
    const fora = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuAberto(false);
      }
    };
    document.addEventListener("mousedown", fora);
    return () => document.removeEventListener("mousedown", fora);
  }, [menuAberto]);

  useEffect(() => setMenuAberto(false), [pathname]);

  const logout = async () => {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } catch (e) {
      console.error("Logout falhou:", e);
    } finally {
      router.push("/login");
    }
  };

  const emProjetos = pathname === "/projetos" || pathname === "/";

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Navegação principal">
        <div className="sidebar__brand">Kanban</div>
        <nav aria-label="Menu">
          <Link href="/projetos" aria-current={emProjetos ? "page" : undefined}>
            Projetos
          </Link>
          {projetoAtualId && (
            <Link
              href={`/projetos/${projetoAtualId}/dashboard`}
              aria-current={pathname.endsWith("/dashboard") ? "page" : undefined}
            >
              Dashboard
            </Link>
          )}
          {projetoAtualId && podeAdmin && (
            <Link
              href={`/projetos/${projetoAtualId}/admin`}
              aria-current={pathname.includes("/admin") ? "page" : undefined}
            >
              Admin
            </Link>
          )}
        </nav>
        {projetoAtualId && (
          <div className="sidebar__project-active">
            Projeto ativo: {projetoAtualId.substring(0, 8)}
          </div>
        )}
      </aside>

      <header className="topbar">
        <div style={{ flex: 1 }} />
        <div className="topbar-actions">
          <NotificacoesSino usuarioId={me.id} />
          <div ref={menuRef} className="topbar__notif-wrapper">
            <button
              type="button"
              className="topbar__user"
              style={{ background: "none", border: "none", cursor: "pointer" }}
              onClick={() => setMenuAberto((v) => !v)}
              aria-label="Menu de usuário"
              aria-expanded={menuAberto}
            >
              <span className="avatar">{iniciais(me.nome)}</span>
              <span>{me.nome}</span>
            </button>
            {menuAberto && (
              <div className="topbar__notif-painel" role="menu">
                <p className="text-secondary" style={{ margin: "var(--space-sm)" }}>
                  {me.email}
                </p>
                <button
                  type="button"
                  className="topbar__logout"
                  style={{ margin: "var(--space-sm)" }}
                  onClick={logout}
                >
                  Sair
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="main">{children}</main>
    </div>
  );
}
