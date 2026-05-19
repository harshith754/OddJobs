import { NextResponse } from "next/server";
import { getStreamRepository } from "../../../../lib/stream-repository";

export async function GET() {
  return NextResponse.json(await getStreamRepository().listSessions());
}
