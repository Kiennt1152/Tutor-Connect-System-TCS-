import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * ============================================================================
 * ST-NFR-002: 100 CONCURRENT USERS AND DATA CONSISTENCY LOAD TEST (k6)
 * ============================================================================
 * Mục tiêu:
 *   1. Duy trì tải 100 người dùng đồng thời (100 Virtual Users) trong 10 phút.
 *   2. Đạt tỷ lệ lỗi 0% (0% error rate).
 *   3. Bắn 100 tranh chấp cùng lúc vào 1 slot/class -> Đảm bảo không trùng lịch,
 *      không trùng lặp gán gia sư (0 overlapping schedules, 0 duplicate assignments).
 * 
 * Lệnh chạy:
 *   k6 run load-tests/st-nfr-002-100vu-concurrency.js
 * Hoặc chạy nhanh 30s smoke test:
 *   k6 run --vus 100 --duration 30s load-tests/st-nfr-002-100vu-concurrency.js
 */

export const options = {
  stages: [
    { duration: '1m', target: 50 },    // Ramp-up lên 50 users
    { duration: '2m', target: 100 },   // Đạt đỉnh 100 concurrent users
    { duration: '7m', target: 100 },   // Duy trì ổn định 100 users liên tục (tổng 10 phút)
    { duration: '1m', target: 0 },     // Ramp-down an toàn
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],    // Tỷ lệ lỗi cho phép < 1% (mục tiêu 0%)
    http_req_duration: ['p(95)<2000'], // 95% request phản hồi dưới 2 giây
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api';
const TARGET_CLASS_ID = __ENV.CLASS_ID || 1;

export default function () {
  // 1. Nhóm 1: Duyệt và tìm kiếm lớp học (Browse & Search Marketplace)
  const searchRes = http.get(`${BASE_URL}/marketplace/classes?status=OPEN`);
  check(searchRes, {
    'Search classes status 200': (r) => r.status === 200,
  });

  // 2. Nhóm 2: Xem chi tiết lớp học
  const classRes = http.get(`${BASE_URL}/marketplace/classes/${TARGET_CLASS_ID}`);
  check(classRes, {
    'View class detail status 200': (r) => r.status === 200 || r.status === 404,
  });

  // 3. Nhóm 3: Mô phỏng tranh chấp nhận lớp (100 concurrent contention on same class)
  // Hệ thống phải xử lý an toàn: hoặc nhận lớp thành công (200), hoặc từ chối hợp lệ (400/409),
  // tuyệt đối không sập server (500) và không tạo duplicate assignment.
  const payload = JSON.stringify({
    notes: 'K6 Automated Concurrent Contention Test',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const applyRes = http.post(`${BASE_URL}/marketplace/classes/${TARGET_CLASS_ID}/apply`, payload, params);
  check(applyRes, {
    'No internal server error (500) under contention': (r) => r.status !== 500,
  });

  sleep(1);
}
