export async function handler(request) {
  const body = await request.json();
  const width = Number(body.width ?? 512);
  const height = Number(body.height ?? 512);

  return Response.json({
    operation: "image-resize",
    sourceUrl: body.sourceUrl,
    target: {
      width,
      height,
      format: body.format ?? "webp"
    },
    status: "accepted"
  });
}
