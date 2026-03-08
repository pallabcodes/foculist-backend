export async function handler(request) {
  const body = await request.json();

  return Response.json({
    operation: "ai-summarize",
    model: body.model ?? "vertex-gemini",
    projectId: body.projectId ?? null,
    summaryStyle: body.summaryStyle ?? "executive",
    promptPreview: String(body.prompt ?? "").slice(0, 120),
    status: "queued"
  });
}
