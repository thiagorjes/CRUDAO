import { forwardToBackend } from "@/lib/proxy";
import { NextRequest } from "next/server";

/** POST /api/ws-ticket — proxy autenticado para o ticket de curta duração (TASK-07.2). */
export async function POST(req: NextRequest) {
  return forwardToBackend(req, "/api/ws-ticket", "POST");
}
