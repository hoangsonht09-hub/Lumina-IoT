# Lumina IoT API Documentation

Tài liệu mô tả REST API dự kiến cho frontend Lumina IoT. Hệ thống hiện quản lý một phòng (`Phòng IoT 01`), ba đèn LED và ba loại cảm biến: nhiệt độ, độ ẩm, ánh sáng.

> Trạng thái: **Proposed Contract** — tài liệu dùng để thống nhất dữ liệu giữa frontend, backend và phần cứng trước khi triển khai.

## 1. Thông tin chung

| Mục | Giá trị |
|---|---|
| Base URL local | `http://localhost:8080/api` |
| Content-Type | `application/json` |
| Xác thực | Bearer Token |
| Múi giờ | `Asia/Ho_Chi_Minh` |
| Định dạng thời gian | ISO 8601, ví dụ `2026-08-18T10:45:00+07:00` |

Quy ước thiết kế:

- Endpoint dùng danh từ số nhiều: `/sensors`, `/devices`, `/actions`.
- JSON dùng `camelCase` để đồng nhất với frontend React.
- Trạng thái lệnh gồm `pending`, `success`, `failed`; trạng thái bật/tắt của LED được lưu riêng bằng `isOn`.
- API danh sách trả về `items` và `pagination` thay vì trộn metadata với dữ liệu.
- Backend là thành phần duy nhất giao tiếp MQTT; frontend chỉ gọi REST API.

Các API cần đăng nhập sử dụng header:

```http
Authorization: Bearer <access_token>
```

Response thành công có cấu trúc chung:

```json
{ 
  "success": true,
  "message": "Thành công",
  "data": {}
}
```

Response lỗi có cấu trúc chung:

```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "errorCode": "VALIDATION_ERROR",
  "errors": []
}
```

## 2. Danh sách API

| API ID | Use Case | Chức năng | Method | Endpoint | Auth | Dùng tại frontend |
|---|---|---|---|---|---|---|
| `API-01` | `UC01` | Đăng nhập | `POST` | `/api/auth/login` | Public | Trang đăng nhập |
| `API-02` | `UC02` | Thông tin tổng quan | `GET` | `/api/dashboard/summary` | Bearer | Trang Tổng quan |
| `API-03` | `UC02` | Cảm biến realtime | `GET` | `/api/sensors/realtime` | Bearer | Thẻ nhiệt độ, độ ẩm, ánh sáng |
| `API-04` | `UC04` | Lịch sử cảm biến | `GET` | `/api/sensors/history` | Bearer | Trang Lịch sử cảm biến |
| `API-05` | `UC03` | Danh sách đèn LED | `GET` | `/api/devices` | Bearer | Tổng quan và Điều khiển thiết bị |
| `API-06` | `UC03` | Điều khiển đèn LED | `POST` | `/api/devices/{id}/control` | Bearer | Bật/tắt và chỉnh độ sáng |
| `API-07` | `UC03` | Tắt tất cả đèn LED | `POST` | `/api/devices/control-all` | Bearer | Nút Tắt tất cả |
| `API-07A` | `UC03` | Kiểm tra trạng thái lệnh | `GET` | `/api/devices/commands/{commandId}` | Bearer | Theo dõi pending/success/failed |
| `API-08` | `UC05` | Lịch sử bật/tắt | `GET` | `/api/actions` | Bearer | Trang Lịch sử bật/tắt |
| `API-09` | `UC06` | Thông tin cá nhân | `GET` | `/api/profile` | Bearer | Trang Thông tin cá nhân |
| `API-10` | `UC06` | Cập nhật cá nhân | `PATCH` | `/api/profile` | Bearer | Chỉnh sửa hồ sơ |
| `API-11` | `UC07` | Xuất báo cáo | `GET` | `/api/reports` | Bearer | Nút Xuất dữ liệu/Tải báo cáo |

### 2.1. Use Case mapping

| Use Case | Tên | Mô tả ngắn |
|---|---|---|
| `UC01` | Đăng nhập | Xác thực người dùng và cấp access token |
| `UC02` | Theo dõi phòng IoT | Xem dashboard và dữ liệu cảm biến mới nhất |
| `UC03` | Điều khiển LED | Xem trạng thái, bật/tắt và chỉnh độ sáng LED |
| `UC04` | Tra cứu cảm biến | Tìm kiếm, lọc và phân trang lịch sử cảm biến |
| `UC05` | Tra cứu thao tác | Xem lịch sử lệnh điều khiển và kết quả từ phần cứng |
| `UC06` | Quản lý hồ sơ | Xem và cập nhật thông tin cá nhân |
| `UC07` | Xuất báo cáo | Tải dữ liệu cảm biến hoặc lịch sử thao tác |

## 3. Authentication

### 3.1. Đăng nhập

```http
POST /api/auth/login
```

Request body:

```json
{
  "email": "b23dccn833@stu.ptit.edu.vn",
  "password": "12345678"
}
```

Response `200 OK`:

```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600,
    "user": {
      "id": "B23DCCN833",
      "fullName": "Nguyễn Trọng Toàn",
      "email": "b23dccn833@stu.ptit.edu.vn",
      "role": "student"
    }
  }
}
```

Lỗi thường gặp:

| Status | Error code | Ý nghĩa |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Thiếu email hoặc mật khẩu |
| `401` | `INVALID_CREDENTIALS` | Email hoặc mật khẩu không đúng |

Response `401 Unauthorized`:

```json
{
  "success": false,
  "message": "Email hoặc mật khẩu không đúng",
  "errorCode": "INVALID_CREDENTIALS",
  "errors": []
}
```

## 4. Dashboard

### 4.1. Lấy dữ liệu tổng quan

```http
GET /api/dashboard/summary
```

Response `200 OK`:

```json
{
  "success": true,
  "data": {
    "room": {
      "name": "Phòng IoT 01",
      "status": "online"
    },
    "sensors": {
      "temperature": { "value": 28.5, "unit": "°C" },
      "humidity": { "value": 72, "unit": "%" },
      "light": { "value": 850, "unit": "lux" }
    },
    "energyToday": { "value": 3.2, "unit": "kWh" },
    "devices": {
      "total": 3,
      "active": 2,
      "offline": 0
    },
    "updatedAt": "2026-08-18T10:45:00+07:00",
    "led4OnCountToday": 3,
    "led4IsOn": true,
    "led4UpdatedAt": "2026-08-18T10:44:00+07:00"
  }
}
```

## 5. Sensors

Giá trị `type` được hỗ trợ:

| Type | Ý nghĩa | Đơn vị |
|---|---|---|
| `temperature` | Nhiệt độ | `°C` |
| `humidity` | Độ ẩm | `%` |
| `light` | Cường độ ánh sáng | `lux` |

### 5.1. Lấy dữ liệu cảm biến realtime

```http
GET /api/sensors/realtime
```

Response `200 OK`:

```json
{
  "success": true,
  "data": {
    "readings": [
      {
        "sensorId": "TEMP_01",
        "type": "temperature",
        "value": 28.5,
        "unit": "°C",
        "status": "normal",
        "recordedAt": "2026-08-18T10:45:00+07:00"
      },
      {
        "sensorId": "HUM_01",
        "type": "humidity",
        "value": 72,
        "unit": "%",
        "status": "warning",
        "recordedAt": "2026-08-18T10:45:00+07:00"
      },
      {
        "sensorId": "LIGHT_01",
        "type": "light",
        "value": 850,
        "unit": "lux",
        "status": "normal",
        "recordedAt": "2026-08-18T10:45:00+07:00"
      }
    ]
  }
}
```

Frontend có thể gọi API này mỗi 5–10 giây. Khi backend hỗ trợ WebSocket hoặc MQTT gateway, có thể thay polling bằng kết nối realtime.

### 5.2. Lấy lịch sử cảm biến

```http
GET /api/sensors/history?search=TEMP&type=temperature&status=normal&from=2026-08-18T10:00:00%2B07:00&to=2026-08-18T11:00:00%2B07:00&page=1&limit=10&direction=desc
```

Query parameters:

| Tham số | Bắt buộc | Mô tả |
|---|---|---|
| `search` | Không | Từ khóa tìm trên mã/tên cảm biến, loại, trạng thái, đơn vị, mã bản ghi hoặc giá trị đo |
| `type` | Không | `temperature`, `humidity`, `light` |
| `value` | Không | Tìm giá trị gần theo phần đầu đã nhập. Ví dụ `25` khớp `[25, 26)`, `25.1` khớp `[25.1, 25.2)`; kết hợp với `type` để giới hạn cảm biến |
| `status` | Không | `normal`, `warning`, `low` |
| `timeQuery` | Không | Tìm theo thứ tự `YYYY-MM-DD HH:mm:ss`; có thể bỏ dần phần cuối để tìm theo phút, giờ, ngày, tháng (`YYYY-MM`) hoặc năm (`YYYY`) |
| `from` | Không | Thời gian bắt đầu, ISO-8601 có múi giờ, ví dụ `2026-08-18T10:00:00+07:00` |
| `to` | Không | Thời gian kết thúc, ISO-8601 có múi giờ, ví dụ `2026-08-18T11:00:00+07:00` |
| `page` | Không | Trang hiện tại, mặc định `1` |
| `limit` | Không | Số bản ghi mỗi trang, mặc định `10`, tối đa `100` |
| `direction` | Không | Thứ tự thời gian `asc` hoặc `desc`, mặc định `desc` |

Response `200 OK`:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "SS-001",
        "sensorId": "TEMP_01",
        "sensorName": "Cảm biến nhiệt độ",
        "type": "temperature",
        "value": 28.5,
        "unit": "°C",
        "status": "normal",
        "recordedAt": "2026-08-18T10:45:00+07:00"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 10,
      "totalItems": 1245,
      "totalPages": 125
    }
  }
}
```

## 6. LED Devices

### 6.1. Lấy danh sách đèn LED

```http
GET /api/devices
```

Response `200 OK`:

```json
{
  "success": true,
  "data": [
    {
      "id": "LED_01",
      "name": "LED 01",
      "type": "led",
      "isOn": true,
      "brightness": 80,
      "connectionStatus": "online",
      "updatedAt": "2026-08-18T10:44:00+07:00"
    },
    {
      "id": "LED_02",
      "name": "LED 02",
      "type": "led",
      "isOn": true,
      "brightness": 60,
      "connectionStatus": "online",
      "updatedAt": "2026-08-18T10:40:00+07:00"
    },
    {
      "id": "LED_03",
      "name": "LED 03",
      "type": "led",
      "isOn": false,
      "brightness": 35,
      "connectionStatus": "online",
      "updatedAt": "2026-08-18T10:33:00+07:00"
    }
  ]
}
```

### 6.2. Điều khiển một đèn LED

```http
POST /api/devices/{id}/control
```

Ví dụ bật `LED 01` và đặt độ sáng 80%:

```http
POST /api/devices/LED_01/control
```

```json
{
  "isOn": true,
  "brightness": 80
}
```

Quy tắc dữ liệu:

- `isOn`: boolean, bắt buộc khi bật/tắt.
- `brightness`: số nguyên từ `0` đến `100`, không bắt buộc.
- Backend tạo bản ghi `pending`, publish MQTT và trả `commandId` ngay cho frontend.
- Khi phần cứng phản hồi, backend cập nhật lệnh thành `success` hoặc `failed` và ghi vào lịch sử thao tác.

Response `200 OK`:

```json
{
  "success": true,
  "message": "Đã gửi lệnh tới LED 01",
  "data": {
    "commandId": "CMD-20260818-104500-001",
    "status": "pending",
    "device": {
      "id": "LED_01",
      "name": "LED 01",
      "isOn": true,
      "brightness": 80,
      "updatedAt": "2026-08-18T10:45:00+07:00"
    }
  }
}
```

Lỗi thường gặp:

| Status | Error code | Ý nghĩa |
|---|---|---|
| `404` | `DEVICE_NOT_FOUND` | Không tìm thấy LED |
| `409` | `DEVICE_COMMAND_PENDING` | LED đang xử lý một lệnh khác |
| `503` | `MQTT_OFFLINE` | Backend chưa kết nối được MQTT broker |
| `503` | `MQTT_PUBLISH_FAILED` | Không thể publish lệnh MQTT |

Khi quá thời gian chờ, trạng thái lệnh được cập nhật thành `failed`:

```json
{
  "commandId": "CMD-20260818-104500-001",
  "status": "failed",
  "failureReason": "HARDWARE_ACK_TIMEOUT"
}
```

### 6.3. Kiểm tra trạng thái lệnh

```http
GET /api/devices/commands/{commandId}
```

Response:

```json
{
  "success": true,
  "data": {
    "commandId": "CMD-20260818-104500-001",
    "deviceId": "LED_01",
    "status": "success",
    "requestedOn": true,
    "createdAt": "2026-08-18T10:45:00+07:00",
    "completedAt": "2026-08-18T10:45:01+07:00",
    "failureReason": null
  }
}
```

### 6.4. Luồng điều khiển qua MQTT

Frontend không kết nối trực tiếp tới MQTT broker. Toàn bộ publish/subscribe được backend xử lý:

```text
Frontend
  → POST /api/devices/LED_01/control
Backend
  → Tạo command với status = pending
  → Publish MQTT command
Hardware
  → Nhận lệnh, điều khiển LED
  → Publish trạng thái phản hồi
Backend
  → Đối chiếu trạng thái thực tế với lệnh pending của từng thiết bị
  → Cập nhật status = success hoặc failed
Frontend
  → Poll GET /api/devices/commands/{commandId}
```

Topic backend publish:

```text
home/esp32/command
```

Payload hiện tại giữ tương thích firmware:

```text
RED ON
```

Topic phần cứng phản hồi:

```text
home/esp32/status
```

Payload trạng thái hiện tại:

```text
Red:ON | Yel:OFF | Gre:ON | Mode:MANUAL
```

### 6.5. Tắt tất cả đèn LED

```http
POST /api/devices/control-all
```

Request body:

```json
{
  "isOn": false
}
```

Response `200 OK`:

```json
{
  "success": true,
  "message": "Đã tắt tất cả đèn LED",
  "data": {
    "updatedDevices": 3,
    "failedDevices": [],
    "commands": [
      {
        "commandId": "CMD-20260818-104500-001",
        "deviceId": "LED_01",
        "status": "pending"
      }
    ]
  }
}
```

## 7. Action History

### 7.1. Lấy lịch sử bật/tắt

Các lần đèn 4 (`LED_04`) tự động bật/tắt theo ánh sáng cũng xuất hiện tại đây với `actionLabel` là `Bật tự động` hoặc `Tắt tự động`, `status` là `success`, và `performedBy.name` là `Hệ thống tự động`. Có thể lọc bằng `deviceId=LED_04`.

```http
GET /api/actions?search=LED%2001&deviceId=LED_01&action=turn_on&status=success&from=2026-08-18T10:00:00&to=2026-08-18T11:00:00&page=1&limit=10
```

Query parameters:

| Tham số | Bắt buộc | Mô tả |
|---|---|---|
| `search` | Không | Tìm theo mã hoặc tên LED |
| `deviceId` | Không | Lọc theo LED |
| `device` | Không | Lọc theo tên hoặc mã LED |
| `action` | Không | `turn_on`, `turn_off` (mọi đèn); `manual_turn_on`, `manual_turn_off` (đèn thủ công); `auto_turn_on`, `auto_turn_off` (đèn 4); `change_brightness` |
| `status` | Không | `success`, `failed` |
| `timeQuery` | Không | Tìm theo thứ tự `YYYY-MM-DD HH:mm:ss`; có thể bỏ dần phần cuối để tìm theo phút, giờ, ngày, tháng (`YYYY-MM`) hoặc năm (`YYYY`) |
| `from`, `to` | Không | Khoảng thời gian cần lọc, định dạng `YYYY-MM-DDTHH:mm:ss` |
| `page` | Không | Trang hiện tại, mặc định `1` |
| `limit` | Không | Số bản ghi mỗi trang, mặc định `10`, tối đa `100` |
| `direction` | Không | Thứ tự thời gian `asc` hoặc `desc`, mặc định `desc` |

Response `200 OK`:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "ACT-1042",
        "commandId": "CMD-20260818-104500-001",
        "deviceId": "LED_01",
        "deviceName": "LED 01",
        "action": "turn_on",
        "actionLabel": "Bật thiết bị",
        "status": "success",
        "deviceState": {
          "isOn": true,
          "brightness": 80
        },
        "performedBy": {
          "id": "B23DCCN833",
          "name": "Nguyễn Trọng Toàn"
        },
        "createdAt": "2026-08-18T10:45:00+07:00"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 10,
      "totalItems": 42,
      "totalPages": 5
    }
  }
}
```

## 8. Profile

### 8.1. Lấy thông tin cá nhân

```http
GET /api/profile
```

Response `200 OK`:

```json
{
  "success": true,
  "data": {
    "studentId": "B23DCCN833",
    "fullName": "Nguyễn Trọng Toàn",
    "email": "b23dccn833@stu.ptit.edu.vn",
    "organization": "PTIT",
    "practiceRoom": "Phòng IoT 01",
    "role": "student",
    "managedDevices": 3
  }
}
```

### 8.2. Cập nhật thông tin cá nhân

```http
PATCH /api/profile
```

Request body:

```json
{
  "fullName": "Nguyễn Trọng Toàn",
  "email": "b23dccn833@stu.ptit.edu.vn"
}
```

## 9. Reports

### 9.1. Xuất báo cáo

```http
GET /api/reports?type=sensors&from=2026-08-01&to=2026-08-18&format=csv
```

Query parameters:

| Tham số | Giá trị hỗ trợ |
|---|---|
| `type` | `sensors`, `actions` |
| `from`, `to` | Ngày bắt đầu và kết thúc |
| `format` | `csv`, `xlsx`, `pdf` |

Response thành công trả về file với header phù hợp, ví dụ:

```http
Content-Type: text/csv
Content-Disposition: attachment; filename="sensor-report-2026-08.csv"
```

## 10. HTTP Status Codes

| Status | Ý nghĩa |
|---|---|
| `200 OK` | Request thành công |
| `201 Created` | Tạo mới thành công |
| `400 Bad Request` | Request không hợp lệ |
| `401 Unauthorized` | Chưa đăng nhập hoặc token hết hạn |
| `403 Forbidden` | Không có quyền thực hiện |
| `404 Not Found` | Không tìm thấy dữ liệu |
| `409 Conflict` | Trạng thái thiết bị không cho phép thao tác |
| `422 Unprocessable Entity` | Dữ liệu đúng định dạng nhưng sai quy tắc |
| `500 Internal Server Error` | Lỗi backend |
| `504 Gateway Timeout` | Thiết bị IoT không phản hồi |

## 11. Gợi ý luồng frontend

1. Gọi `POST /api/auth/login` và lưu access token.
2. Khi mở Tổng quan, gọi song song `/api/dashboard/summary` và `/api/devices`.
3. Poll `/api/sensors/realtime` mỗi 5–10 giây để cập nhật các thẻ cảm biến.
4. Khi bật/tắt hoặc đổi độ sáng, gọi `/api/devices/{id}/control` và chỉ cập nhật UI sau khi backend phản hồi thành công.
5. Khi mở các trang lịch sử, truyền filter và pagination qua query parameters.
6. Khi token hết hạn (`401`), xóa phiên đăng nhập và chuyển về trang Login.
