import { NextResponse } from "next/server";
import { getStreamRepository } from "../../../../../lib/stream-repository";
import { UploadFrameRequest } from "../../../../../lib/types";

type RouteContext = {
  params: {
    sessionId: string;
  };
};

export async function POST(request: Request, { params }: RouteContext) {
  const contentType = request.headers.get("content-type") ?? "";
  let image;

  if (contentType.includes("multipart/form-data")) {
    const formData = await request.formData();
    const imageFile = formData.get("image");
    if (!(imageFile instanceof File)) {
      return NextResponse.json({ error: "Missing image file" }, { status: 400 });
    }

    image = await getStreamRepository().uploadFrame({
      sessionId: params.sessionId,
      file: imageFile,
      sequenceNumber: Number(formData.get("sequenceNumber")) || undefined,
      width: Number(formData.get("width")) || undefined,
      height: Number(formData.get("height")) || undefined,
      fileSizeBytes: Number(formData.get("fileSizeBytes")) || imageFile.size
    });
  } else {
    const body = (await request.json().catch(() => ({}))) as UploadFrameRequest;
    image = await getStreamRepository().uploadFrame({
      sessionId: params.sessionId,
      imageUrl: body.imageUrl,
      sequenceNumber: body.sequenceNumber,
      width: body.width,
      height: body.height,
      fileSizeBytes: body.fileSizeBytes
    });
  }

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
