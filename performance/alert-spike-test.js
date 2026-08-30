import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const applicationErrors = new Rate('application_errors');

export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '10s', target: 200 },
    { duration: '1m', target: 200 },
    { duration: '15s', target: 10 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    application_errors: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{name:emergency-route-spike}': ['p(95)<1500'],
  },
};

export function setup() {
  const nearest = http.get(`${BASE_URL}/api/junctions/nearest?lat=32.078&lon=34.774`);
  if (nearest.status !== 200 || !nearest.json('id')) {
    throw new Error(`Cannot prepare spike test: nearest junction returned ${nearest.status}`);
  }

  // The local simulator changes only FireRoute state; the test never calls Oref.
  const simulation = http.post(`${BASE_URL}/api/alerts/simulate?active=true`);
  if (simulation.status !== 200) {
    throw new Error('Start the server with fireroute.alerts.source=simulated');
  }
  sleep(2);
  return { sourceId: nearest.json('id') };
}

export default function (data) {
  const status = http.get(`${BASE_URL}/api/alerts/status`, {
    tags: { name: 'alert-status-spike' },
  });
  let passed = check(status, {
    'alert endpoint survives spike': (r) => r.status === 200,
  });

  const route = http.get(
    `${BASE_URL}/api/routes/emergency?sourceId=${encodeURIComponent(data.sourceId)}`,
    { tags: { name: 'emergency-route-spike' } }
  );
  passed = check(route, {
    'emergency route survives spike': (r) =>
      r.status === 200 && r.json('routeType') === 'EMERGENCY',
  }) && passed;

  applicationErrors.add(!passed);
  sleep(1);
}

export function teardown() {
  http.post(`${BASE_URL}/api/alerts/simulate?active=false`);
}
