import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 20,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<400"]
  }
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
  const headers = {
    "X-Tenant-ID": "tenant-load",
    "Content-Type": "application/json"
  };

  const workflow = http.get(`${baseUrl}/api/planning/v1/workflow/statuses`, { headers });
  check(workflow, {
    "workflow status 200": (response) => response.status === 200
  });

  const push = http.post(
    `${baseUrl}/api/sync/v1/sync/push`,
    JSON.stringify({
      deviceId: `device-${__VU}`,
      pendingChanges: 1,
      payloadVersion: "v1",
      payload: {
        projectId: "load-project",
        entity: "task",
        mutation: "ping"
      }
    }),
    { headers }
  );
  check(push, {
    "sync push 200": (response) => response.status === 200
  });

  sleep(1);
}
