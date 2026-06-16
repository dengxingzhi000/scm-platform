import http from 'k6/http';
import { check, sleep } from 'k6';
import { config } from './config.js';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  // Login
  const loginRes = http.post(`${config.authURL}/api/v1/auth/login`, JSON.stringify({
    username: config.username,
    password: config.password,
  }), { headers: { 'Content-Type': 'application/json' } });

  check(loginRes, { 'login successful': (r) => r.status === 200 });

  const token = loginRes.json('data.token');
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // Get orders
  const ordersRes = http.get(`${config.baseURL}/api/v1/orders?page=1&size=10`, { headers });
  check(ordersRes, { 'orders retrieved': (r) => r.status === 200 });

  sleep(1);
}
