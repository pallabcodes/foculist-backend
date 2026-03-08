export async function handler(request) {
  const body = await request.json();

  return Response.json({
    operation: "pdf-report",
    projectId: body.projectId,
    reportType: body.reportType ?? "project-status",
    includeSections: body.includeSections ?? ["summary", "tasks", "audit-log"],
    status: "queued"
  });
}
