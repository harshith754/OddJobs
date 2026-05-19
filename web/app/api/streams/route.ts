import { NextResponse } from "next/server";
import { createSession, ensureSeedData } from "../../../lib/stream-store";
import { CreateStreamSessionRequest } from "../../../lib/types";

export async function POST(request: Request) {
  ensureSeedData();
  const body = (await request.json().catch(() => ({}))) as CreateStreamSessionRequest;
  return NextResponse.json(createSession(body));
}
