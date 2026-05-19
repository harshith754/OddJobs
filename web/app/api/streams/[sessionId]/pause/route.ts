import { NextResponse } from "next/server";
import { getStreamRepository } from "../../../../../lib/stream-repository";

type RouteContext = {
  params: Promise<{
    sessionId: string;
  }>;
};

export async function POST(_: Request, { params }: RouteContext) {
  const { sessionId } = await params;
  const result = await getStreamRepository().mutateSessionStatus(
    sessionId,
    "paused"
  );

  if (result == null) {
    return NextResponse.json({ error: "Session not found" }, { status: 404 });
  }

  return NextResponse.json(result);
}
