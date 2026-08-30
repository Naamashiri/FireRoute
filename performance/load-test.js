import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SMOKE = (__ENV.K6_SMOKE || '').toLowerCase() === 'true';
const applicationErrors = new Rate('application_errors');

export const options = {
  stages: SMOKE
    ? [{ duration: '10s', target: 1 }]
    : [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '30s', target: 0 },
      ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    application_errors: ['rate<0.01'],
    http_req_duration: [SMOKE ? 'p(95)<1500' : 'p(95)<500'],
    'http_req_duration{name:emergency-route}': [SMOKE ? 'p(95)<2000' : 'p(95)<750'],
  },
};

export function setup() {
  const response = http.get(`${BASE_URL}/api/junctions/nearest?lat=32.078&lon=34.774`, {
    tags: { name: 'nearest-junction-setup' },
  });
  const valid = check(response, {
    'setup finds a junction': (r) => r.status === 200 && Boolean(r.json('id')),
  });
  if (!valid) {
    throw new Error(`Cannot prepare performance test: nearest junction returned ${response.status}`);
  }
  return { sourceId: response.json('id') };
}

export default function (data) {
  group('emergency user flow', () => {
    const alert = http.get(`${BASE_URL}/api/alerts/status`, {
      tags: { name: 'alert-status' },
    });
    record(alert, 'alert status is available', (r) =>
      r.status === 200 && Boolean(r.json('status'))
    );

    const nearest = http.get(`${BASE_URL}/api/junctions/nearest?lat=32.078&lon=34.774`, {
      tags: { name: 'nearest-junction' },
    });
    record(nearest, 'nearest junction is available', (r) =>
      r.status === 200 && Boolean(r.json('id'))
    );

    const route = http.get(
      `${BASE_URL}/api/routes/emergency?sourceId=${encodeURIComponent(data.sourceId)}`,
      { tags: { name: 'emergency-route' } }
    );
    record(route, 'emergency route has a business result', (r) =>
      r.status === 200 && r.json('routeType') === 'EMERGENCY' &&
      (r.json('found') === true || r.json('failureReason') === 'NO_REACHABLE_SHELTER')
    );
  });

  // Models a client polling once per second instead of an artificial tight loop.
  sleep(1);
}

function record(response, label, predicate) {
  const passed = check(response, { [label]: predicate });
  applicationErrors.add(!passed);
}
