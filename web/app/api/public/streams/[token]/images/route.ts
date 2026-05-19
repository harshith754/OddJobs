import { NextResponse } from "next/server";
import { getStreamRepository } from "../../../../../../lib/stream-repository";

type RouteContext = {
  params: Promise<{
    token: string;
  }>;
};

export async function GET(_: Request, { params }: RouteContext) {
  const { token } = await params;
  const history = await getStreamRepository().getHistory(token);
  if (history == null) {
    return NextResponse.json({ error: "Stream not found" }, { status: 404 });
  }

  return NextResponse.json(history);
}
