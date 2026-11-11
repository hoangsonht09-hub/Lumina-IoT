# Đấu thêm Đèn 4 (LED rời)

Tắt nguồn ESP32 trước khi đấu dây. Dùng LED rời và điện trở 330 Ω nối tiếp:

```text
ESP32 GPIO18 ── điện trở 330 Ω ── chân dài (+) LED
ESP32 GND    ─────────────────── chân ngắn (−) LED
```

GPIO18 là chân ghi 18 hoặc D18 trên ESP32 Dev Module; không phải chân thứ 18 tính từ đầu hàng. Dùng chung GND. Không nối LED trực tiếp vào GPIO mà thiếu điện trở. Hướng dẫn này dành cho LED nhỏ; đèn nguồn ngoài/220 V cần mạch điều khiển riêng, không đấu vào GPIO.

1. Mở đúng `Firmware/IOT_Light_Sensor.ino` và Upload lại ESP32.
2. Firmware dùng `LED_FOUR = 18`, lệnh `LED4 ON` / `LED4 OFF`, và gửi trạng thái `LED4:ON/OFF` trên `home/esp32/status`.
3. Dừng backend cũ bằng Ctrl+C, chạy lại `start-hardware.ps1` từ thư mục gốc với APP_EMAIL/APP_PASSWORD của bạn. Backend chuyển bản ghi LED_04 cũ thành “Đèn 4” thủ công, giữ lịch sử.
4. Chạy frontend (`cd Frontend`, `npm run dev`) và tải lại web. Đèn 4 có công tắc riêng và tham gia Bật tất cả / Tắt tất cả.
5. Có thể thử từ Serial Monitor 115200 baud bằng `LED4 ON`, rồi `LED4 OFF` (chọn New Line). Khi điều khiển qua web, kiểm tra `MQTT command status=OK`.

Chuyển động là dữ liệu giả lập 0/1 do backend tạo mỗi 5 giây và lưu database (MOTION_01). Biểu đồ và trang Lịch sử cảm biến cùng lấy các mẫu này; tên cảm biến có nhãn “giả lập”. Chưa có cảm biến chuyển động thật. Nhiệt độ, độ ẩm, ánh sáng vẫn lấy MQTT thật. Lệnh Đèn 4 được ghi trong Lịch sử bật/tắt như ba LED còn lại. Có thể tắt sinh mẫu giả bằng APP_SIMULATION_MOTION_ENABLED=false khi chạy backend.
