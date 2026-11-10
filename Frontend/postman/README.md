# Postman - Lumina IoT

## Import

Import hai file sau vào Postman:

1. `Lumina-IoT.postman_collection.json`
2. `Lumina-Local.postman_environment.json`

Chọn environment **Lumina IoT - Local**, sau đó điền biến `password` bằng giá trị `APP_PASSWORD` đang dùng khi chạy backend.

## Thứ tự kiểm thử

1. Chạy `Health Check` để xác nhận backend đang ở `http://localhost:8080`.
2. Chạy `Login - Tự lưu JWT`. Script của request sẽ tự lưu JWT vào biến `accessToken`.
3. Chạy các API Dashboard, Sensors, Devices, Actions và Reports.

## Test API lịch sử

Trong nhóm `03 - Dashboard & Sensors`:

- `Sensors History - All`: lấy dữ liệu và thử các filter nâng cao đang để disabled.
- `Sensors History - Filter Field & Value`: mô phỏng chọn trường rồi nhập giá trị ở FE. Đổi `sensorField` thành `all`, `temperature`, `humidity`, `light` hoặc `time`.
- `Sensors History - Search Time Text`: tìm theo thứ tự `YYYY-MM-DD HH:mm:ss`; có thể bỏ dần phần cuối để tìm đến tháng (`YYYY-MM`) hoặc năm (`YYYY`).

Trong nhóm `05 - Actions & Profile`:

- `Action History - All`: lấy toàn bộ lịch sử bật/tắt.
- `Action History - Filters`: lọc theo tên/ID thiết bị, hành động và trạng thái.
- `Action History - Search Time Text`: tìm thời gian bằng text; có thể bật thêm các filter đang disabled.

Các biến `sensorField`, `sensorValue`, `historyTime`, `deviceFilter`, `actionFilter`, `statusFilter`, `page`, `limit` và `direction` đã được thêm vào environment để thay đổi nhanh khi test.

`Turn Device ON/OFF` mặc định điều khiển `LED_01`. Đổi biến `deviceId` thành `LED_02` hoặc `LED_03` để thử đèn khác.

Các request điều khiển thiết bị sẽ publish lệnh MQTT nếu backend đang kết nối broker và `MQTT_ENABLED=true`.
