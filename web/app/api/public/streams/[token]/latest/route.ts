import { NextResponse } from "next/server";
import { getStreamRepository } from "../../../../../../lib/stream-repository";

type RouteContext = {
  params: {
    token: string;
  };
};

export async function GET(_: Request, { params }: RouteContext) {
  const latest = await getStreamRepository().getLatestFrame(params.token);
  if (latest == null) {
    return NextResponse.json({ error: "Stream not found" }, { status: 404 });
  }

  return NextResponse.json(latest);
}
