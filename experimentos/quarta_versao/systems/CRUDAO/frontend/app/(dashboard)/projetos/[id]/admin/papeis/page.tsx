"use client";

import { useParams } from "next/navigation";
import PapeisView from "@/components/admin/PapeisView";

/** TL-09 — Papéis e Permissões. */
export default function PapeisAdminPage() {
  const params = useParams();
  return <PapeisView projetoId={params.id as string} />;
}
