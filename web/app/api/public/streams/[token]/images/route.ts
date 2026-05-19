import { NextResponse } from "next/server";
import { ensureSeedData, getHistory } from "../../../../../../lib/stream-store";

type RouteContext = {
  params: {
    token: string;
  };
};

export async function GET(_: Request, { params }: RouteContext) {
  ensureSeedData();
  const history = getHistory(params.token);
  if (history == null) {
    return NextResponse.json({ error: "Stream not found" }, { status: 404 });
  }

  return NextResponse.json(history);
}
