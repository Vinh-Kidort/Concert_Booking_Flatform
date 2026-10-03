import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// Định nghĩa Custom Metrics để đo lường
const queueWaitDuration = new Trend('waiting_room_wait_time');
const bookingGatedDuration = new Trend('booking_gated_latency');
const successfulBookings = new Counter('successful_bookings');

export const options = {
    stages: [
        { duration: '10s', target: 100 }, // 100 users đổ vào phòng chờ
        { duration: '30s', target: 300 }, // Đỉnh điểm 300 users cùng xếp hàng
        { duration: '10s', target: 0 },   // Giảm tải
    ],
    thresholds: {
        // Độ trễ lúc ĐẶT VÉ THẬT (sau khi qua phòng chờ) phải cực nhanh < 150ms
        booking_gated_latency: ['p(95)<150'],
    },
};

const BASE_URL = 'http://localhost:8080/api/v1';
const CONCERT_ID = 1;

export default function () {
    // -------------------------------------------------------------
    // BƯỚC 1: ĐĂNG NHẬP LẤY JWT TOKEN
    // -------------------------------------------------------------
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({
            email: 'alice@example.com',
            password: 'Password123!',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    const token = loginRes.json('data.accessToken');
    const authHeaders = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
    };

    // -------------------------------------------------------------
    // BƯỚC 2: THAM GIA PHÒNG CHỜ (JOIN QUEUE)
    // -------------------------------------------------------------
    const joinRes = http.post(`${BASE_URL}/waiting-room/${CONCERT_ID}/join`, null, {
        headers: authHeaders,
    });
    check(joinRes, { 'joined waiting room successfully': (r) => r.status === 200 });

    // -------------------------------------------------------------
    // BƯỚC 3: POLL STATUS CHỜ ĐẾN LƯỢT (MAX 30 GIÂY)
    // -------------------------------------------------------------
    let isAdmitted = false;
    let attempts = 0;
    const startQueueTime = Date.now();

    while (!isAdmitted && attempts < 15) {
        sleep(2); // Cứ mỗi 2 giây hỏi lại vị trí hàng đợi 1 lần
        attempts++;

        const statusRes = http.get(`${BASE_URL}/waiting-room/${CONCERT_ID}/status`, {
            headers: authHeaders,
        });

        if (statusRes.status === 200) {
            isAdmitted = statusRes.json('data.admitted');
        }
    }

    // Ghi nhận thời gian người dùng phải chờ trong phòng chờ
    queueWaitDuration.add(Date.now() - startQueueTime);

    // Nếu quá thời gian chờ mà chưa được thả -> Thoát
    if (!isAdmitted) {
        return;
    }

    // -------------------------------------------------------------
    // BƯỚC 4: LẤY VÉ VÀO CỔNG (ADMISSION TOKEN)
    // -------------------------------------------------------------
    const tokenRes = http.post(`${BASE_URL}/waiting-room/${CONCERT_ID}/admission-token`, null, {
        headers: authHeaders,
    });
    const admissionToken = tokenRes.json('data');

    // -------------------------------------------------------------
    // BƯỚC 5: ĐẶT VÉ THẬT (GATED BOOKING API)
    // -------------------------------------------------------------
    const idempotencyKey = `wr-${__VU}-${__ITER}-${Date.now()}`;
    const bookingPayload = JSON.stringify({
        concertId: CONCERT_ID,
        idempotencyKey: idempotencyKey,
        items: [{ ticketCategoryId: 1, quantity: 1 }],
    });

    const gatedHeaders = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        'X-Admission-Token': admissionToken,
        'X-Concert-Id': String(CONCERT_ID),
    };

    const bookingStartTime = Date.now();
    const bookingRes = http.post(`${BASE_URL}/bookings`, bookingPayload, {
        headers: gatedHeaders,
    });

    bookingGatedDuration.add(Date.now() - bookingStartTime);

    const isSuccess = check(bookingRes, {
        'booking allowed (201 or 409)': (r) => r.status === 201 || r.status === 409,
        'not blocked by admission filter (not 403)': (r) => r.status !== 403,
    });

    if (bookingRes.status === 201) {
        successfulBookings.add(1);
    }
}