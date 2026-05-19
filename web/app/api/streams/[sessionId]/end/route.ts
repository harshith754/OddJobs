import { NextResponse } from "next/server";
import { getStreamRepository } from "../../../../../lib/stream-repository";

type RouteContext = {
  params: {
    sessionId: string;
  };
};

export async function POST(_: Request, { params }: RouteContext) {
  const result = await getStreamRepository().mutateSessionStatus(
    params.sessionId,
    "ended"
  );

  if (result == null) {
    return NextResponse.json({ error: "Session not found" }, { status: 404 });
  }

  return NextResponse.json(result);
}
