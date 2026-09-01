"use client";

import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { iniciais } from "@/lib/format";
import type { MeResponse } from "@/lib/types";
import { useEffect, useRef, useState } from "react";
import NotificacoesSino from "@/components/notificacoes/NotificacoesSino";

interface DashboardShellProps {
  me: MeResponse;
  children?: React.ReactNode;
}

export default function DashboardShell({ me, children }: DashboardShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [showUserMenu, setShowUserMenu] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Detecta se está em um projeto específico (ex: /projetos/123/board)
  const projetoAtualId = pathname.split("/")[2];
  const projetoAtual = me.projetos.find((p) => p.projetoId === projetoAtualId);

  // Fechar menu ao clicar fora (I2)
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setShowUserMenu(false);
      }
    };

    if (showUserMenu) {
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
    }
  }, [showUserMenu]);

  // Fechar menu ao navegar
  useEffect(() => {
    setShowUserMenu(false);
  }, [pathname]);

  const handleLogout = async () => {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
      router.push("/login");
    } catch (error) {
      console.error("Logout failed:", error);
      // Fallback: redirecionar mesmo assim
      setTimeout(() => router.push("/login"), 2000);
    }
  };

  return (
    <div className="app-shell">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar__brand">
          📋 Kanban
        </div>

        {/* Seção: Projetos */}
        <nav>
          <div className="sidebar-section-title">
            Meus Projetos
          </div>
          {me.projetos.length > 0 ? (
            me.projetos.map((projeto) => (
              <Link
                key={projeto.projetoId}
                href={`/projetos/${projeto.projetoId}/board`}
                aria-current={projetoAtualId === projeto.projetoId ? "page" : undefined}
              >
                Projeto {projeto.projetoId.substring(0, 8)}
              </Link>
            ))
          ) : (
            <div className="text-secondary" style={{ padding: "var(--space-sm)" }}>
              Nenhum projeto
            </div>
          )}
        </nav>

        {/* Projeto ativo: Links rápidos */}
        {projetoAtual && (
          <div className="sidebar__project-active" style={{ borderTop: "1px solid var(--color-border)", marginTop: "auto" }}>
            <div className="sidebar-section-title">
              Ações
            </div>
            <nav>
              <Link href={`/projetos/${projetoAtualId}/board`} aria-current={pathname.endsWith("/board") ? "page" : undefined}>
                📊 Board
              </Link>
              <Link href={`/projetos/${projetoAtualId}/dashboard`} aria-current={pathname.endsWith("/dashboard") ? "page" : undefined}>
                📈 Dashboard
              </Link>
              {/* Admin only — validação no servidor */}
              {projetoAtual.papeis.some((r) => ["admin", "project_admin"].includes(r)) && (
                <Link href={`/projetos/${projetoAtualId}/admin`} aria-current={pathname.endsWith("/admin") ? "page" : undefined}>
                  ⚙️ Admin
                </Link>
              )}
            </nav>
          </div>
        )}
      </aside>

      {/* Topbar */}
      <header className="topbar">
        {/* Placeholder para título da página — deixar vazio por enquanto */}
        <div style={{ flex: 1 }} />

        {/* Ações: notificações (TASK-07.7) e usuário */}
        <div className="topbar-actions">
          {/* Sino de notificações (TASK-07.7 / RF-005) */}
          <NotificacoesSino usuarioId={me.id} />

          {/* Menu de usuário */}
          <div ref={menuRef} className="topbar__notif-wrapper">
            <button
              className="topbar__user"
              onClick={() => setShowUserMenu(!showUserMenu)}
              aria-label="Menu de usuário"
              aria-expanded={showUserMenu}
            >
              <span className="avatar">{iniciais(me.nome)}</span>
              <span>{me.nome}</span>
            </button>

            {showUserMenu && (
              <div className="topbar__notif-painel">
                <div style={{ padding: "var(--space-sm)" }}>
                  <div className="text-secondary" style={{ marginBottom: "var(--space-sm)" }}>
                    {me.email}
                  </div>
                  <button
                    onClick={handleLogout}
                    className="topbar__logout"
                    style={{ width: "100%" }}
                  >
                    Logout
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main content area */}
      <main className="main">
        {children}
      </main>
    </div>
  );
}
