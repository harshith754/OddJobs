import { NextResponse } from "next/server";
import { ensureSeedData, uploadFrame } from "../../../../../lib/stream-store";
import { UploadFrameRequest } from "../../../../../lib/types";

type RouteContext = {
  params: {
    sessionId: string;
  };
};

export async function POST(request: Request, { params }: RouteContext) {
  ensureSeedData();
  const body = (await request.json().catch(() => ({}))) as UploadFrameRequest;
  const image = uploadFrame(params.sessionId, body);

  if (image == null) {
    return NextResponse.json({ error: "Session not found" }, { status: 404 });
  }

  return NextResponse.json({
    success: true,
    imageId: image.id,
    imageUrl: image.imageUrl,
    sequenceNumber: image.sequenceNumber
  });
}

