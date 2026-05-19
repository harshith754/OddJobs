import { NextResponse } from "next/server";
import { ensureSeedData, mutateSessionStatus } from "../../../../../lib/stream-store";

type RouteContext = {
  params: {
    sessionId: string;
  };
};

export async function POST(_: Request, { params }: RouteContext) {
  ensureSeedData();
  const result = mutateSessionStatus(params.sessionId, "ended");

  if (result == null) {
    return NextResponse.json({ error: "Session not found" }, { status: 404 });
  }

  return NextResponse.json(result);
}

