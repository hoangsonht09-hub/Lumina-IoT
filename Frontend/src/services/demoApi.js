import { activityRows, chartSeries, sensorRows } from '../mocks/iotData'

let devices = ['Đèn đỏ', 'Đèn vàng', 'Đèn xanh'].map((name, index) => ({ id: index + 1, name, isOn: false, brightness: 80 }))
let profile = JSON.parse(localStorage.getItem('lumina_demo_profile') || 'null') ?? {
  fullName: 'Nguyễn Hoàng Sơn', studentId: 'B23DCCN721',
  organization: 'Học viện Công nghệ Bưu chính Viễn thông', email: 'son@example.com',
}
const actions = activityRows.map((row, index) => ({
  id: row.id, deviceName: row.device, actionLabel: row.action,
  action: index === 4 ? 'mất kết nối' : row.action.startsWith('Bật') ? 'manual_turn_on' : 'manual_turn_off',
  status: row.status, createdAt: row.timestamp, performedBy: { name: 'Nguyễn Hoàng Sơn' },
}))

function paginate(rows, params, timeKey) {
  const sorted = [...rows].sort((a, b) => String(b[timeKey]).localeCompare(String(a[timeKey])))
  if (params.direction === 'asc') sorted.reverse()
  const limit = Number(params.limit ?? 10)
  const page = Number(params.page ?? 1)
  return { items: sorted.slice((page - 1) * limit, page * limit), pagination: { totalItems: rows.length, totalPages: Math.ceil(rows.length / limit) } }
}

function control(id, isOn) {
  devices = devices.map((device) => device.id === id ? { ...device, isOn } : device)
  const device = devices.find((item) => item.id === id)
  actions.unshift({ id: `demo-${Date.now()}-${id}`, deviceName: device.name,
    actionLabel: isOn ? 'Bật thiết bị' : 'Tắt thiết bị', action: isOn ? 'manual_turn_on' : 'manual_turn_off',
    status: 'success', createdAt: new Date().toISOString(), performedBy: { name: profile.fullName } })
  return { status: 'success' }
}

export const demoApi = {
  getDashboard: async () => ({ room: { name: 'Phòng IoT' }, sensors: {
    temperature: { value: 28.5, unit: '°C' }, humidity: { value: 72, unit: '%' }, light: { value: 850, unit: 'lux' },
  } }),
  getDevices: async () => devices,
  controlDevice: async (id, isOn) => control(id, isOn),
  controlAll: async (isOn) => ({ commands: devices.map((device) => control(device.id, isOn)) }),
  getSensorHistory: async (params = {}) => {
    let rows = sensorRows.map((row) => ({ ...row, sensorName: row.sensor, recordedAt: row.timestamp }))
    if (params.type && params.limit === 7) {
      rows = chartSeries[params.type].map((value, index) => ({ id: index, type: params.type, value, recordedAt: `2026-08-${12 + index}T10:45:00` }))
    }
    rows = rows.filter((row) => (!params.type || row.type === params.type)
      && (params.value === undefined || row.value === params.value)
      && (!params.search || `${row.sensorName} ${row.value} ${row.recordedAt}`.toLowerCase().includes(params.search.toLowerCase()))
      && (!params.timeQuery || row.recordedAt.replace('T', ' ').includes(params.timeQuery)))
    return paginate(rows, params, 'recordedAt')
  },
  getActions: async (params = {}) => paginate(actions.filter((row) =>
    (!params.device || row.deviceName === params.device) && (!params.action || row.action === params.action)
    && (!params.status || row.status === params.status)
    && (!params.timeQuery || row.createdAt.replace('T', ' ').includes(params.timeQuery))), params, 'createdAt'),
  getProfile: async () => profile,
  updateProfile: async (next) => {
    profile = { ...profile, ...next }
    localStorage.setItem('lumina_demo_profile', JSON.stringify(profile))
    return profile
  },
}
