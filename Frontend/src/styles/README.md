# Cấu trúc CSS

Ứng dụng chỉ import `index.css`. File này chịu trách nhiệm nạp các stylesheet theo đúng thứ tự.

## Thư mục

- `globals.css`: biến màu, typography, reset và thiết lập toàn cục.
- `layout/`: khung ứng dụng, sidebar và topbar.
- `components/`: các phần được dùng lại như tiêu đề trang, filter, table và toast.
- `pages/`: CSS chỉ thuộc về một trang cụ thể.

## Cách chọn file để sửa

- Sửa sidebar hoặc menu: `layout/sidebar.css`.
- Sửa topbar hoặc thông báo: `layout/topbar.css`.
- Sửa input, select hoặc ô tìm kiếm: `components/filters.css`.
- Sửa bảng, badge hoặc phân trang: `components/table.css`.
- Sửa Dashboard: `pages/dashboard.css`.
- Sửa lịch sử cảm biến: `pages/sensor-history.css`.
- Sửa lịch sử bật/tắt: `pages/activity-history.css`.
- Sửa thông tin cá nhân: `pages/profile.css`.

Responsive của mỗi phần được đặt ở cuối chính file đó. Không import trực tiếp từng file CSS trong component; hãy thêm stylesheet mới vào `index.css` để giữ đúng thứ tự cascade.
