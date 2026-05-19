import { NextResponse } from "next/server";
import { getStreamRepository } from "../../../lib/stream-repository";
import { CreateStreamSessionRequest } from "../../../lib/types";

export async function POST(request: Request) {
  const body = (await request.json().catch(() => ({}))) as CreateStreamSessionRequest;
  return NextResponse.json(await getStreamRepository().createSession(body));
}
