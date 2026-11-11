# Giải thích dự án Lumina IoT

Tài liệu đối chiếu với mã nguồn hiện có, không chỉ dựa vào README cũ. Không có file nguồn nào bị xóa trong lần rà soát này.

## 1. Dự án làm gì?

Hệ thống đọc nhiệt độ, độ ẩm, độ sáng từ ESP32; hiển thị trên website; điều khiển ba LED từ website; lưu lịch sử cảm biến và lệnh điều khiển.

```text
DHT22 + BH1750 → ESP32 → MQTT broker → Spring Boot → Database
                                           ↑            ↓
                                           └── REST API ── React

React → REST API → Spring Boot → MQTT broker → ESP32 → LED
ESP32 → bản tin trạng thái → MQTT broker → Spring Boot → React đọc kết quả
```

MQTT broker là máy chủ trung gian chuyển bản tin theo topic. Nó phải chạy riêng; firmware và backend đều là client của broker. Trình duyệt gọi backend qua HTTP, không kết nối trực tiếp ESP32.

## 2. Phần cứng và firmware

File `Firmware/IOT_Light_Sensor.ino` chứa toàn bộ chương trình nạp lên ESP32.

| Thành phần | Kết nối trong mã | Vai trò |
|---|---|---|
| DHT22 | GPIO15 | Đọc nhiệt độ °C và độ ẩm % |
| BH1750 | SDA GPIO19, SCL GPIO21 | Đo độ rọi theo lux qua I2C; thử địa chỉ 0x23 và 0x5C |
| LED đỏ | GPIO2 | Bật/tắt bằng lệnh RED |
| LED vàng | GPIO4 | Bật/tắt bằng lệnh YELLOW |
| LED xanh | GPIO5 | Bật/tắt bằng lệnh GREEN |

Các phần chính trong file:

| Hàm/nhóm biến | Tác dụng |
|---|---|
| WiFi/MQTT và chân GPIO | Khai báo mạng, broker, tài khoản và chân phần cứng |
| `setRed`, `setYellow`, `setGreen`, `setAllLed` | Cập nhật biến trạng thái và mức HIGH/LOW của đầu ra |
| `beginLightSensor` | Dò địa chỉ I2C và khởi tạo BH1750 |
| `updateLightSensor` | Đọc lux mỗi 250 ms; nếu chưa khởi tạo được thì thử lại mỗi 5 giây |
| `publishLedStatus` | Gửi trạng thái ba đầu ra LED qua MQTT |
| `handleCommand` | Nhận RED/YELLOW/GREEN/ALL ON hoặc OFF, đổi đầu ra rồi gửi trạng thái ngay |
| `callback` | Xử lý bản tin MQTT: lệnh LED hoặc cấu hình ngưỡng sáng |
| `maintainConnection` | Duy trì WiFi/MQTT, thử kết nối lại với khoảng chờ 5 giây, xử lý bản tin đến |
| `sensorField` | Định dạng giá trị đo; giá trị không hợp lệ được ghi ERR |
| `setup` | Chạy một lần: khởi tạo Serial, tắt LED, khởi tạo cảm biến, WiFi và MQTT |
| `loop` | Lặp liên tục: cập nhật lux, xử lý Serial/MQTT, mỗi 2 giây đọc DHT22 và gửi dữ liệu |

Topic đang sử dụng:

| Topic | Chiều truyền | Ví dụ nội dung |
|---|---|---|
| `home/esp32/sensors` | ESP32 → backend | `Temp:28.5C \| Hum:72.0% \| Lux:123.4 lux \| Light:SANG` |
| `home/esp32/status` | ESP32 → backend | `Red:ON \| Yel:OFF \| Gre:ON` |
| `home/esp32/command` | Backend → ESP32 | `RED ON`, `ALL OFF` |
| `home/esp32/config/light-threshold` | Client MQTT → ESP32 | `THRESHOLD:100` |

Ngưỡng mặc định là 100 lux: nhỏ hơn ngưỡng được phân loại `TOI`, còn lại là `SANG`. **Firmware hiện không tự bật/tắt đèn theo lux và không có LED4.** Ngưỡng được giữ trong RAM, chưa lưu bền qua khởi động lại. Có thể nhập lệnh LED trong Serial Monitor ở 115200 baud, kết thúc bằng xuống dòng.

## 3. Luồng xử lý thực tế

### Thu thập cảm biến

1. ESP32 đọc cảm biến rồi publish chuỗi dữ liệu.
2. `MqttGateway` nhận bản tin từ broker.
3. `IotPayloadParser` phân tích chuỗi thành `SensorSnapshot`.
4. `SensorService` nhận sự kiện, phân loại trạng thái và lưu `SensorReading` vào database.
5. React gọi REST API để lấy dữ liệu tổng quan/lịch sử và vẽ giao diện. `App.jsx` tải lại tổng quan và danh sách thiết bị mỗi 3 giây; đây là polling HTTP.

### Điều khiển LED

Ví dụ nhấn bật đèn đỏ:

1. `App.jsx` gọi `iotApi.controlDevice` → `POST /api/devices/LED_01/control`.
2. Backend kiểm tra thiết bị và tạo bản ghi lệnh `pending`.
3. `DeviceService` gửi `RED ON` qua MQTT.
4. ESP32 nhận lệnh, đặt GPIO2 lên HIGH, publish trạng thái.
5. Backend nhận `DeviceSnapshot`; `CommandLifecycleService` đối chiếu trạng thái với yêu cầu để đánh dấu `success`.
6. React hỏi kết quả lệnh mỗi 500 ms, tối đa 16 vòng chờ, sau đó cập nhật giao diện.

Backend mặc định cho lệnh khoảng 5 giây để nhận trạng thái phù hợp; tác vụ định kỳ đánh dấu lệnh quá hạn thành thất bại. Xác nhận dựa trên trạng thái đầu ra do firmware báo về, không có cảm biến riêng kiểm tra LED thực sự phát sáng. Firmware cũng chưa gửi commandId kèm phản hồi.

### Đăng nhập và hồ sơ

Backend kiểm tra tài khoản cấu hình, cấp JWT. React lưu token trong localStorage và đính kèm `Authorization: Bearer ...` khi gọi API. Khi API trả 401, giao diện xóa phiên đăng nhập.

`ProfilePage.jsx` lấy/sửa thông tin và liên kết bằng API; ảnh đại diện được lưu riêng trong localStorage của trình duyệt, không upload vào database.

## 4. Các file ở thư mục gốc

| File/thư mục | Tác dụng |
|---|---|
| `Firmware/` | Chương trình chạy trên ESP32 |
| `Backend/` | Ứng dụng Java xử lý API, MQTT và database |
| `Frontend/` | Website React |
| `start-hardware.ps1` | Đọc cấu hình broker từ firmware, đặt biến môi trường backend, ghi `Frontend/.env.local`, chạy backend; mặc định cổng 8081 và H2 `data/hardware` |
| `.gitignore` và các `.gitignore` con | Quy định file không đưa vào Git |
| `.git/` | Lịch sử và dữ liệu quản lý phiên bản; cần giữ |
| `.vscode/` | Cấu hình IDE, không phải thành phần chạy nghiệp vụ |
| `.github/` | Nơi chứa cấu hình GitHub nếu có; không phải firmware/backend/frontend |
| `Report.docx`, `BÀI 1_Sua.docx` | Tài liệu Word; không tham gia luồng chạy phần mềm. Nội dung Word chưa được rà soát trong tài liệu này |
| `hs_err_pid*.log` | Nhật ký lỗi JVM, dùng chẩn đoán Java bị crash |
| `replay_pid*.log` | Dữ liệu phục vụ chẩn đoán/tái hiện lỗi JVM |
| `~$...docx` | File khóa tạm do Word tạo; chỉ dọn sau khi đóng tài liệu tương ứng |

## 5. File frontend

Các đường dẫn trong bảng tính từ `Frontend/`.

| File | Tác dụng |
|---|---|
| `package.json` | Khai báo thư viện và lệnh dev/build/lint/preview |
| `package-lock.json` | Khóa phiên bản thư viện để cài đặt nhất quán |
| `index.html` | Trang HTML gốc để React gắn giao diện |
| `eslint.config.js` | Quy tắc kiểm tra mã JavaScript/React |
| `README.md` | Hướng dẫn frontend; có một số đường dẫn cũ cần cập nhật |
| `.env.local` nếu có | Cấu hình API và chế độ demo tại máy hiện tại |
| `src/main.jsx` | Điểm khởi động React và nạp CSS |
| `src/App.jsx` | Quản lý phiên đăng nhập, route, tải tổng quan, điều khiển LED và chờ kết quả |
| `src/config/navigation.js` | Danh sách menu điều hướng |
| `src/services/apiClient.js` | HTTP client, đăng nhập, lưu JWT, xử lý lỗi và hết phiên |
| `src/services/iotApi.js` | Các hàm API theo nghiệp vụ; chọn backend thật hoặc demo |
| `src/services/demoApi.js` | Mô phỏng API cho chế độ demo |
| `src/mocks/iotData.js` | Dữ liệu giả mà demoApi sử dụng |
| `src/utils/dateTime.js` | Tiện ích xử lý/hiển thị thời gian |
| `src/features/auth/LoginPage.jsx` | Màn hình đăng nhập |
| `src/features/overview/OverviewPage.jsx` | Tổng quan cảm biến và điều khiển đèn |
| `src/features/overview/components/MetricCard.jsx` | Thẻ chỉ số trên tổng quan |
| `src/features/overview/components/SensorChart.jsx` | Biểu đồ cảm biến |
| `src/features/sensors/SensorHistoryPage.jsx` | Bảng lịch sử cảm biến và bộ lọc |
| `src/features/activity/ActivityHistoryPage.jsx` | Bảng lịch sử thao tác và bộ lọc |
| `src/features/profile/ProfilePage.jsx` | Xem/sửa hồ sơ, ảnh đại diện và liên kết dự án |
| `src/features/profile/profile.jpg` | Ảnh đại diện mặc định, đang được import |
| `src/components/layout/DashboardLayout.jsx` | Khung chung của các trang sau đăng nhập |
| `src/components/layout/Sidebar.jsx` | Thanh menu bên trái |
| `src/components/layout/Topbar.jsx` | Thanh trên cùng |
| `src/components/layout/Brand.jsx` | Nhãn/logo ứng dụng |
| `src/components/common/Toggle.jsx` | Công tắc giao diện |
| `src/components/common/Toast.jsx` | Thông báo ngắn |
| `src/components/common/PageIntro.jsx` | Tiêu đề và phần giới thiệu trang |
| `src/components/common/DataTable.jsx` | Bảng dữ liệu dùng chung |
| `src/components/common/CopyableDateTime.jsx` | Hiển thị thời gian có khả năng sao chép |
| `src/styles/index.css` | Tập hợp import các file CSS |
| `src/styles/globals.css` | Biến giao diện và định dạng chung |
| `src/styles/reference-theme.css` | Lớp định dạng bổ sung; đang được index.css import |
| `src/styles/README.md` | Hướng dẫn tổ chức CSS |
| `src/styles/layout/shell.css` | Bố cục khung ứng dụng |
| `src/styles/layout/sidebar.css` | Kiểu thanh menu |
| `src/styles/layout/topbar.css` | Kiểu thanh trên cùng |
| `src/styles/components/page.css` | Định dạng thành phần trang dùng chung |
| `src/styles/components/filters.css` | Định dạng bộ lọc |
| `src/styles/components/table.css` | Định dạng bảng |
| `src/styles/components/feedback.css` | Định dạng thông báo và phản hồi giao diện |
| `src/styles/pages/dashboard.css` | Kiểu trang tổng quan |
| `src/styles/pages/sensor-history.css` | Kiểu lịch sử cảm biến |
| `src/styles/pages/activity-history.css` | Kiểu lịch sử thao tác |
| `src/styles/pages/profile.css` | Kiểu trang hồ sơ |
| `src/styles/pages/login.css` | Kiểu trang đăng nhập |
| `public/docs/API_DOCUMENTATION.md` | Tài liệu REST API có thể phục vụ như tài nguyên tĩnh |
| `postman/README.md` | Hướng dẫn thử API bằng Postman |
| `postman/Lumina-IoT.postman_collection.json` | Tập hợp request thử API |
| `postman/Lumina-Local.postman_environment.json` | Biến môi trường Postman local |
| `scripts/append_api_docs_to_srs.ps1` | Tiện ích ghép tài liệu API vào tài liệu Word |
| `scripts/append_api_docs_openxml.ps1` | Tiện ích ghép tài liệu bằng OpenXML |

`backend-command-stage/` là mã Java đặt trong frontend, không thuộc luồng build React/Vite thông thường và không phải nguồn backend chính. Bên trong:

- `README.md`: hướng dẫn của bộ mã này.
- `replacements/DeviceService.java`, `replacements/DeviceController.java`: bản thay thế phục vụ tích hợp.
- `src/main/resources/application.yml`: cấu hình của bộ mã staging.
- `src/main/java/.../config/MqttProperties.java`: thuộc tính MQTT.
- `.../controller/DeviceController.java`: API điều khiển.
- `.../dto/DeviceDtos.java`: cấu trúc request/response điều khiển.
- `.../model/ActionLog.java`: dữ liệu nhật ký lệnh.
- `.../repository/DeviceRepository.java`, `ActionLogRepository.java`: truy cập database.
- `.../service/DeviceService.java`, `CommandLifecycleService.java`: gửi và quản lý vòng đời lệnh.
- `src/test/java/.../IotLightSensorApplicationTests.java`, `.../service/CommandLifecycleServiceTests.java`: kiểm thử tương ứng.

Các file này có tên trùng với backend chính nhưng không được coi là giống hệt nội dung. Có thể chuyển cả thư mục ra nơi lưu trữ ngoài dự án nếu không còn cần bộ mã tích hợp cũ.

## 6. File backend

Các đường dẫn Java dưới đây tính từ `Backend/src/main/java/com/example/demo/`.

### Khởi động và cấu hình

| File | Tác dụng |
|---|---|
| `IotLightSensorApplication.java` | Khởi động Spring Boot |
| `config/MqttProperties.java` | Ánh xạ cấu hình MQTT thành đối tượng Java |
| `config/AuthProperties.java` | Ánh xạ cấu hình tài khoản và JWT |
| `config/DataInitializer.java` | Tạo dữ liệu nền như thiết bị, cảm biến và hồ sơ cần thiết |
| `config/OpenApiConfig.java` | Cấu hình tài liệu OpenAPI/Swagger |
| `security/SecurityConfig.java` | Quy định bảo vệ API, xác thực và CORS |
| `security/JwtService.java` | Tạo/kiểm tra token JWT |

### MQTT

| File | Tác dụng |
|---|---|
| `feature/mqtt/MqttGateway.java` | Kết nối broker, subscribe, nhận và gửi MQTT |
| `feature/mqtt/MqttPublisher.java` | Interface trừu tượng hóa việc gửi lệnh MQTT |
| `feature/mqtt/IotPayloadParser.java` | Phân tích payload cảm biến/trạng thái; hỗ trợ lux và dữ liệu LDR cũ |
| `feature/mqtt/MqttEvents.java` | Các kiểu sự kiện SensorSnapshot/DeviceSnapshot truyền trong backend |

### API, nghiệp vụ và DTO

Controller nhận HTTP request; service thực hiện xử lý; DTO quy định dữ liệu vào/ra. Mỗi file dưới đây có trách nhiệm riêng:

| Controller | Service | DTO | Chức năng |
|---|---|---|---|
| `controller/AuthController.java` | `service/AuthService.java` | `dto/AuthDtos.java` | Đăng nhập và dữ liệu phiên |
| `controller/DeviceController.java` | `service/DeviceService.java` | `dto/DeviceDtos.java` | Danh sách đèn, điều khiển một/tất cả đèn và truy vấn lệnh |
| `controller/DashboardController.java` | `service/DashboardService.java` | `dto/DashboardDtos.java` | Tổng hợp dữ liệu dashboard |
| `controller/SensorController.java` | `service/SensorService.java` | `dto/SensorDtos.java` | Nhận/lưu dữ liệu đo, truy vấn hiện tại và lịch sử |
| `controller/ActionController.java` | `service/ActionService.java` | `dto/ActionDtos.java` | Lọc và phân trang nhật ký thao tác |
| `controller/ProfileController.java` | `service/ProfileService.java` | `dto/ProfileDtos.java` | Đọc và cập nhật hồ sơ |
| `controller/ReportController.java` | `service/ReportService.java` | Không có file DTO riêng trong nhóm này | Xuất dữ liệu cảm biến/thao tác thành CSV |

Hai service bổ sung:

- `service/CommandLifecycleService.java`: tạo pending, ngăn lệnh chồng nhau, đối chiếu phản hồi trạng thái, ghi success/failed và xử lý timeout.
- `service/AutoLightStatsService.java`: ghi nhận chuyển trạng thái LED4 và thống kê số lần bật trong ngày khi bản tin có LED4. Firmware hiện tại không gửi trường này.

### Dữ liệu và truy cập database

Model mô tả đối tượng/bảng; repository dùng Spring Data JPA để truy vấn và lưu nó.

| Model | Repository | Dữ liệu quản lý |
|---|---|---|
| `model/Device.java` | `repository/DeviceRepository.java` | Mã đèn, màu, trạng thái, lần liên lạc cuối |
| `model/Sensor.java` | `repository/SensorRepository.java` | Thông tin cảm biến |
| `model/SensorReading.java` | `repository/SensorReadingRepository.java` | Mỗi giá trị đo và thời gian ghi nhận |
| `model/ActionLog.java` | `repository/ActionLogRepository.java` | Lệnh điều khiển, người thực hiện và kết quả |
| `model/UserProfile.java` | `repository/UserProfileRepository.java` | Hồ sơ người dùng |
| `model/AutoLightEvent.java` | `repository/AutoLightEventRepository.java` | Sự kiện LED4 phục vụ thống kê |

`mapper/IotMapper.java` chuyển các entity thành DTO phù hợp để trả về frontend.

### Kiểu trạng thái và tiện ích chung

| File | Tác dụng |
|---|---|
| `enums/CommandStatus.java` | Trạng thái pending/success/failed của lệnh |
| `enums/DeviceAction.java` | Loại thao tác bật/tắt/thay đổi độ sáng |
| `enums/DeviceConnectionStatus.java` | Trạng thái kết nối thiết bị |
| `enums/SensorType.java` | Loại nhiệt độ/độ ẩm/ánh sáng |
| `enums/SensorStatus.java` | Nhãn phân loại trạng thái cảm biến |
| `common/ApiResponse.java` | Định dạng response API thống nhất |
| `common/PagedResponse.java` | Dữ liệu kèm thông tin phân trang |
| `common/BusinessException.java` | Lỗi nghiệp vụ có mã lỗi và HTTP status |
| `common/GlobalExceptionHandler.java` | Chuyển exception thành response lỗi |
| `common/HistoryTimeFilter.java` | Phân tích bộ lọc thời gian cho lịch sử |

### File build, cấu hình và kiểm thử

Các đường dẫn tính từ `Backend/`:

| File | Tác dụng |
|---|---|
| `pom.xml` | Khai báo Java/Maven, Spring Boot, JPA, MQTT, H2/MySQL và thư viện khác |
| `mvnw` | Chạy Maven wrapper trên Unix/Linux |
| `mvnw.cmd` | Chạy Maven wrapper trên Windows |
| `.mvn/wrapper/maven-wrapper.properties` | Cấu hình phiên bản/phân phối Maven wrapper |
| `README.md` | Hướng dẫn backend, nhưng mô tả LED4 hiện chưa khớp firmware |
| `src/main/resources/application.yml` | Cổng HTTP, database, JWT, CORS, MQTT và timeout |
| `src/test/resources/application-test.yml` | Cấu hình riêng khi kiểm thử |
| `src/test/java/.../IotLightSensorApplicationTests.java` | Kiểm thử ứng dụng trong lớp test này |
| `src/test/java/.../feature/mqtt/IotPayloadParserTest.java` | Kiểm thử phân tích payload MQTT |
| `src/test/java/.../service/CommandLifecycleServiceTests.java` | Kiểm thử vòng đời lệnh |
| `src/test/java/.../service/AutoLightStatsServiceTest.java` | Kiểm thử thống kê đèn tự động |

## 7. Những điểm dễ hiểu nhầm

- README backend mô tả LED4 tự động nhưng firmware chỉ có ba LED. Backend vẫn khởi tạo LED4, phân tích trường LED4 nếu có và cung cấp thống kê. Chưa thể xem đây là tính năng phần cứng đang hoạt động.
- Firmware dùng `digitalWrite` để bật/tắt; dù API có trường brightness, phần cứng chưa có điều khiển độ sáng PWM.
- README frontend có nhắc `features/devices/DeviceControlPage.jsx`, `hooks/`, `styles/responsive.css`; các đường dẫn đó không có trong danh sách nguồn hiện tại. Điều khiển đèn hiện nằm ở trang tổng quan.
- `application.yml` yêu cầu `DB_URL`, `JWT_SECRET`, `APP_PASSWORD` qua môi trường. Không nên dựa vào câu README nói mặc định tự có H2; script `start-hardware.ps1` mới đặt DB_URL H2 cụ thể.
- Chạy backend trực tiếp mặc định cổng 8080; script phần cứng mặc định 8081 và ghi frontend theo cổng đó. Link Swagger mặc định trong ProfilePage đang cố định 8081.
- Có hai cách mô phỏng khác nhau: frontend demo qua `VITE_ENABLE_DEMO=true`; backend bỏ MQTT qua `MQTT_ENABLED=false`. Khi backend bỏ MQTT, lệnh được hoàn tất trong database mà không điều khiển ESP32.
- Firmware có thông tin kết nối ghi trực tiếp trong mã. Khi chia sẻ dự án nên đưa thông tin riêng sang cấu hình không commit.

## 8. File nào có thể dọn?

| Nhóm | Đề xuất và điều kiện |
|---|---|
| `hs_err_pid*.log`, `replay_pid*.log` | Có thể xóa nếu không còn cần phân tích lỗi Java; không phải mã nguồn |
| `Frontend/backend-command-stage/` | Có thể chuyển ra thư mục lưu trữ nếu không cần bộ mã tích hợp cũ; ứng dụng chính dùng Backend/ |
| `Frontend/scripts/append_api_docs_*.ps1` | Không cần để chạy ứng dụng; giữ nếu còn dùng tạo báo cáo |
| `Frontend/postman/` | Không cần khi chạy; hữu ích để kiểm tra API |
| Các file `.docx` | Có thể chuyển sang nơi chứa tài liệu nếu muốn gọn; cần giữ bản gốc ở nơi khác |
| `~$...docx` | Để Word tự dọn hoặc chỉ xóa sau khi đã đóng Word |
| `Frontend/node_modules/`, `Frontend/dist/`, `Backend/target/` nếu có | Thư viện/kết quả build có thể tạo lại; xóa sẽ cần cài/build lại |
| `Backend/data/` hoặc các file database nếu có | Không xóa tùy tiện: chứa lịch sử và hồ sơ được lưu |

Không xóa riêng `demoApi.js`/`iotData.js`: đang có chuỗi import từ `iotApi.js`; muốn bỏ demo phải sửa liên kết mã trước. Không xóa `reference-theme.css` hay `profile.jpg`: đang được sử dụng. Không xóa nhóm LED4 đơn lẻ vì dashboard, khởi tạo dữ liệu, repository và kiểm thử còn phụ thuộc.

## 9. Thứ tự đọc để hiểu nhanh

1. `Firmware/IOT_Light_Sensor.ino`: hiểu thiết bị đo/gửi/nhận gì.
2. `Backend/.../feature/mqtt/MqttGateway.java` và `IotPayloadParser.java`: hiểu cầu nối phần cứng.
3. `SensorService.java`, `DeviceService.java`, `CommandLifecycleService.java`: hiểu lưu dữ liệu và xác nhận điều khiển.
4. `Frontend/src/services/iotApi.js` và `apiClient.js`: hiểu giao tiếp HTTP.
5. `Frontend/src/App.jsx`, `OverviewPage.jsx`: hiểu cách hiển thị và thao tác.
6. Các model/repository, trang lịch sử và ProfilePage để hiểu các chức năng còn lại.

Phạm vi rà soát: đọc mã nguồn và cấu hình, kiểm tra tham chiếu các nhóm nghi dư thừa. Chưa chạy hệ thống với ESP32/broker thật; chưa kiểm chứng kết nối hoặc đo đạc phần cứng.
