export const sensorRows = [
  { id: 'SS-001', sensor: 'Cảm biến nhiệt độ', type: 'temperature', value: 28.5, unit: '°C', timestamp: '2026-08-18T10:45:32', time: '10:45:32', date: '18/08/2026', status: 'normal' },
  { id: 'SS-002', sensor: 'Cảm biến độ ẩm', type: 'humidity', value: 72, unit: '%', timestamp: '2026-08-18T10:42:18', time: '10:42:18', date: '18/08/2026', status: 'warning' },
  { id: 'SS-003', sensor: 'Cảm biến ánh sáng', type: 'light', value: 850, unit: 'lux', timestamp: '2026-08-18T10:40:05', time: '10:40:05', date: '18/08/2026', status: 'normal' },
  { id: 'SS-004', sensor: 'Cảm biến nhiệt độ', type: 'temperature', value: 27.8, unit: '°C', timestamp: '2026-08-18T10:35:47', time: '10:35:47', date: '18/08/2026', status: 'normal' },
  { id: 'SS-005', sensor: 'Cảm biến độ ẩm', type: 'humidity', value: 74, unit: '%', timestamp: '2026-08-18T10:30:21', time: '10:30:21', date: '18/08/2026', status: 'warning' },
  { id: 'SS-006', sensor: 'Cảm biến ánh sáng', type: 'light', value: 620, unit: 'lux', timestamp: '2026-08-18T10:22:09', time: '10:22:09', date: '18/08/2026', status: 'normal' },
  { id: 'SS-007', sensor: 'Cảm biến nhiệt độ', type: 'temperature', value: 28.1, unit: '°C', timestamp: '2026-08-18T10:10:56', time: '10:10:56', date: '18/08/2026', status: 'normal' },
]

export const initialDevices = [
  { id: 1, name: 'LED 01', room: 'Phòng IoT 01', on: true, brightness: 80, updated: '1 phút trước' },
  { id: 2, name: 'LED 02', room: 'Phòng IoT 01', on: true, brightness: 60, updated: '5 phút trước' },
  { id: 3, name: 'LED 03', room: 'Phòng IoT 01', on: false, brightness: 35, updated: '12 phút trước' },
]

export const activityRows = [
  { id: '#1042', device: 'LED 01', room: 'Phòng IoT 01', action: 'Bật thiết bị', status: 'success', timestamp: '2026-08-18T10:45:14', time: '10:45:14 AM', date: '18/08/2026', user: 'Nguyễn Trọng Toàn' },
  { id: '#1041', device: 'LED 02', room: 'Phòng IoT 01', action: 'Tắt thiết bị', status: 'success', timestamp: '2026-08-18T10:32:41', time: '10:32:41 AM', date: '18/08/2026', user: 'Nguyễn Trọng Toàn' },
  { id: '#1040', device: 'LED 03', room: 'Phòng IoT 01', action: 'Tắt thiết bị', status: 'success', timestamp: '2026-08-18T09:18:26', time: '09:18:26 AM', date: '18/08/2026', user: 'Nguyễn Trọng Toàn' },
  { id: '#1039', device: 'LED 01', room: 'Phòng IoT 01', action: 'Bật thiết bị', status: 'success', timestamp: '2026-08-18T08:30:08', time: '08:30:08 AM', date: '18/08/2026', user: 'Nguyễn Trọng Toàn' },
  { id: '#1038', device: 'LED 02', room: 'Phòng IoT 01', action: 'Mất kết nối', status: 'failed', timestamp: '2026-08-17T23:30:52', time: '11:30:52 PM', date: '17/08/2026', user: 'Nguyễn Trọng Toàn' },
]

export const chartSeries = {
  temperature: [25, 25.8, 27.4, 26.2, 28.1, 29.4, 28.5],
  humidity: [68, 70, 69, 73, 71, 75, 72],
  light: [310, 480, 420, 650, 590, 780, 850],
}

export const chartLabels = ['12/08', '13/08', '14/08', '15/08', '16/08', '17/08', '18/08']
