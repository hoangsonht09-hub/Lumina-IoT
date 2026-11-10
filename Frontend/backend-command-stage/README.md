# Lumina IoT Backend

Spring Boot backend kết nối React với ESP32 qua MQTT. Dự án giữ cấu trúc package:

```text
src/main/java/com/example/demo/
├── common/       # response chung và exception handler
├── config/       # properties và dữ liệu khởi tạo
├── controller/   # REST API
├── dto/          # request/response records
├── enums/        # trạng thái và loại dữ liệu
├── feature/mqtt/ # MQTT gateway, parser và event
├── mapper/       # entity -> DTO
├── model/        # JPA entities
├── repository/   # Spring Data repositories
├── security/     # JWT và Spring Security
├── service/      # nghiệp vụ
└── IotLightSensorApplication.java
```

## Luồng dữ liệu

```text
ESP32 -- home/esp32/sensors --> Backend --> Database --> REST API --> React
ESP32 -- home/esp32/status  --> Backend --> Database --> REST API --> React
React --> POST /api/devices --> Backend -- home/esp32/command --> ESP32
```

Backend tương thích trực tiếp firmware hiện tại:

- Sensor: `Temp:28.5C | Hum:72.0% | LDR:1 | Light:TOI`
- Status: `Red:ON | Yel:OFF | Gre:ON | Mode:MANUAL`
- Command: `RED ON`, `RED OFF`, `YELLOW ON`, `GREEN OFF`, `ALL ON`, `ALL OFF`

Ánh xạ phần cứng: `LED_01 = RED`, `LED_02 = YELLOW`, `LED_03 = GREEN`. LDR trong firmware là digital nên backend lưu giá trị raw `0/1`, không giả lập thành lux.

## Chạy local

Yêu cầu Java 21 trở lên. Maven đã có wrapper trong dự án. Tạo các biến môi trường bằng giá trị riêng của bạn:

```powershell
$env:MQTT_BROKER_URI='tcp://172.20.10.10:1884'
$env:MQTT_USERNAME='<mqtt-username>'
$env:MQTT_PASSWORD='<mqtt-password>'
$env:MQTT_COMMAND_TIMEOUT_SECONDS='5'
$env:JWT_SECRET='<chuoi-bi-mat-it-nhat-32-ky-tu>'
$env:APP_EMAIL='<email-dang-nhap>'
$env:APP_PASSWORD='<mat-khau-dang-nhap>'
.\mvnw.cmd spring-boot:run
```

Nếu chỉ cần chạy giao diện/API không có broker, đặt thêm `$env:MQTT_ENABLED='false'`.

Mặc định local dùng H2 file tại `data/lumina`. Để dùng MySQL, đặt:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/lumina_iot?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Bangkok'
$env:DB_USERNAME='<database-username>'
$env:DB_PASSWORD='<database-password>'
```

## Kiểm thử

```powershell
.\mvnw.cmd test
```

Health check: `GET http://localhost:8080/actuator/health`.
