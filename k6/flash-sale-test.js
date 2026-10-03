import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const bookingSuccess = new Counter('booking_success');
const bookingInsufficientStock = new Counter('booking_insufficient_stock');
const bookingErrors = new Counter('booking_errors');
const bookingDuration = new Trend('booking_duration', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CONCERT_ID = 1; // phải seed sẵn 1 concert + category trước khi chạy
const CATEGORY_ID = 1;

export const options = {
    scenarios: {
        flash_sale: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 200 },  // ramp up dần, tránh sốc hệ thống ngay lập tức
                { duration: '3m', target: 500 },  // giữ ở mức tương đương yêu cầu đề bài (300-500 req/phút)
                { duration: '30s', target: 0 },    // ramp down
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'], // 95% request phải dưới 1s
        booking_errors: ['count<10'],       // lỗi hệ thống thật (không tính hết vé) phải rất thấp
    },
};

// Login 1 lần để lấy token dùng chung — trong thực tế nên tạo nhiều user test
// khác nhau để mô phỏng đúng nhiều khách hàng riêng biệt thay vì 1 user duy nhất
export function setup() {
    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
        email: 'alice@example.com',
        password: 'Password123!',
    }), { headers: { 'Content-Type': 'application/json' } });

    const token = loginRes.json('data.accessToken');
    return { token };
}

export default function (data) {
    const idempotencyKey = randomString(20);

    const payload = JSON.stringify({
        concertId: parseInt(CONCERT_ID),
        idempotencyKey: idempotencyKey,
        items: [{ ticketCategoryId: parseInt(CATEGORY_ID), quantity: 1 }],
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`,
        },
    };

    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/v1/bookings`, payload, params);
    bookingDuration.add(Date.now() - start);

    if (res.status === 201) {
        bookingSuccess.add(1);
    } else if (res.status === 409) {
        bookingInsufficientStock.add(1); // expected — hết vé, không phải lỗi hệ thống
    } else {
        bookingErrors.add(1); // 500, 401, timeout... đây mới là lỗi thật cần quan tâm
        console.error(`Unexpected status ${res.status}: ${res.body}`);
    }

    check(res, {
        'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
    });

    sleep(0.1);
}