"use client";

import { useState } from "react";

type Tab = "projeto" | "workflows" | "raias" | "papeis";

interface AdminLayoutClientProps {
  children: React.ReactNode;
  projetoId: string;
}

export default function AdminLayoutClient({ children, projetoId }: AdminLayoutClientProps) {
  const [activeTab, setActiveTab] = useState<Tab>("projeto");

  const tabs: { id: Tab; label: string; href: string }[] = [
    { id: "projeto", label: "Projeto", href: `/projetos/${projetoId}/admin/projeto` },
    { id: "workflows", label: "Workflows", href: `/projetos/${projetoId}/admin/workflows` },
    { id: "raias", label: "Raias", href: `/projetos/${projetoId}/admin/raias` },
    { id: "papeis", label: "Papéis", href: `/projetos/${projetoId}/admin/papeis` },
  ];

  return (
    <div className="flex flex-col h-full bg-gray-50 p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Administração</h1>
        <p className="text-sm text-gray-600 mt-1">Gerencie workflows, etapas, raias e configurações</p>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex gap-6">
          {tabs.map((tab) => (
            <a
              key={tab.id}
              href={tab.href}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 font-medium text-sm border-b-2 transition cursor-pointer ${
                activeTab === tab.id
                  ? "border-blue-600 text-blue-600"
                  : "border-transparent text-gray-600 hover:text-gray-900"
              }`}
            >
              {tab.label}
            </a>
          ))}
        </nav>
      </div>

      {/* Content */}
      <div className="flex-1">{children}</div>
    </div>
  );
}
