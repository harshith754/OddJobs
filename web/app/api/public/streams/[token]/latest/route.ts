import { NextResponse } from "next/server";
import { ensureSeedData, getLatestFrame } from "../../../../../../lib/stream-store";

type RouteContext = {
  params: {
    token: string;
  };
};

export async function GET(_: Request, { params }: RouteContext) {
  ensureSeedData();
  const latest = getLatestFrame(params.token);
  if (latest == null) {
    return NextResponse.json({ error: "Stream not found" }, { status: 404 });
  }

  return NextResponse.json(latest);
}
