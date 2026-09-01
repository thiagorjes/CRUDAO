"use client";

import { usePathname } from "next/navigation";

type Tab = "projeto" | "workflows" | "raias" | "papeis" | "usuarios";

interface AdminLayoutClientProps {
  projetoId: string;
  children: React.ReactNode;
}

/** TL-08 — Admin de Projeto: header + abas (docs/design/.../tl-08-admin-projeto.html). */
export default function AdminLayoutClient({ projetoId, children }: AdminLayoutClientProps) {
  const pathname = usePathname();

  const tabs: { id: Tab; label: string; href: string }[] = [
    { id: "projeto", label: "Projeto", href: `/projetos/${projetoId}/admin/projeto` },
    { id: "workflows", label: "Colunas", href: `/projetos/${projetoId}/admin/workflows` },
    { id: "raias", label: "Raias", href: `/projetos/${projetoId}/admin/raias` },
    { id: "papeis", label: "Papéis/Permissões", href: `/projetos/${projetoId}/admin/papeis` },
    { id: "usuarios", label: "Usuários", href: `/projetos/${projetoId}/admin/usuarios` },
  ];

  return (
    <div>
      <div className="page-header">
        <h1>Admin de Projeto</h1>
      </div>

      <div className="tabs" role="tablist" aria-label="Configuração do projeto">
        {tabs.map((tab) => (
          <a
            key={tab.id}
            href={tab.href}
            role="tab"
            aria-selected={pathname?.startsWith(tab.href)}
          >
            {tab.label}
          </a>
        ))}
      </div>

      <div>{children}</div>
    </div>
  );
}
