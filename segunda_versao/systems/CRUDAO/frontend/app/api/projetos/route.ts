import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/projetos — proxy de criação de projeto (RF-008, exige adminGlobal — ADR-007). */
export async function POST(req: NextRequest) {
  return forwardToBackend(req, "/api/projetos", "POST");
}
