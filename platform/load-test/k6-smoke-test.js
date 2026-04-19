import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 50 }, // Ramp up to 50 users
    { duration: '1m', target: 100 }, // Stay at 100 users
    { duration: '30s', target: 0 },  // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests must be under 200ms
  },
};

const BASE_URL = 'http://localhost:8080/api/sync/v1/sync';

export default function () {
  let payload = JSON.stringify({
    deviceId: 'k6-device-id',
    lastSync: '2026-04-06T12:00:00Z'
  });

  let params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-ID': 'public'
    },
  };

  let res = http.post(`${BASE_URL}/pull`, payload, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'latency is acceptable': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
