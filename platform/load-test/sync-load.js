import http from 'k6/http';
import { check, sleep } from 'k6';
import { ws } from 'k6/ws';

export const options = {
    stages: [
        { duration: '1m', target: 100 },  // Ramp up to 100 users
        { duration: '3m', target: 500 },  // Stay at 500 users
        { duration: '1m', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'], // 95% of requests must complete below 200ms
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WS_URL = __ENV.WS_URL || 'ws://localhost:8083/v1/sync';

export default function () {
    // 1. Authenticate (Placeholder - assuming anonymous or pre-shared token)
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Tenant-ID': 'public',
        },
    };

    // 2. HTTP Pull
    const pullRes = http.post(`${BASE_URL}/v1/sync/pull`, JSON.stringify({
        deviceId: `k6-device-${__VU}`,
        lastSync: null
    }), params);

    check(pullRes, {
        'is status 200': (r) => r.status === 200,
        'has server time': (r) => r.json().serverTime !== undefined,
    });

    // 3. WebSocket Real-time Session
    const res = ws.connect(WS_URL, params, function (socket) {
        socket.on('open', () => {
            console.log(`VU ${__VU} connected to sync`);

            // Push a simulated CRDT operation
            socket.send(JSON.stringify({
                type: 'push',
                deviceId: `k6-device-${__VU}`,
                pendingChanges: 1,
                payload: {
                    type: 'CURSOR_UPDATE',
                    x: Math.random() * 1000,
                    y: Math.random() * 1000
                }
            }));
        });

        socket.on('message', (data) => {
            const msg = JSON.parse(data);
            if (msg.type === 'push_ack') {
                socket.close();
            }
        });

        socket.setTimeout(function () {
            socket.close();
        }, 5000);
    });

    check(res, { 'ws connected successfully': (r) => r && r.status === 101 });

    sleep(1);
}
