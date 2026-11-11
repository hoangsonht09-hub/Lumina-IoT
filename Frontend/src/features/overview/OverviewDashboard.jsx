import { Lightbulb, Power, Sun, Thermometer, Droplets, Activity } from 'lucide-react'
import Toggle from '../../components/common/Toggle'
import DashboardChart from './components/DashboardChart'
import '../../styles/pages/overview-refresh.css'

export default function OverviewDashboard({ summary, devices, onToggle, onAllOn, onAllOff, pendingDeviceIds = [] }) {
  const motion = summary?.sensors?.motion?.value
  const metrics = [
    { key: 'temperature', name: 'Nhiệt độ', icon: Thermometer, unit: '°C' },
    { key: 'humidity', name: 'Độ ẩm', icon: Droplets, unit: '%' },
    { key: 'light', name: 'Ánh sáng', icon: Sun, unit: 'lux' },
    { key: 'motion', name: 'Chuyển động', icon: Activity, unit: '' },
  ]
  return <div className="page overview-page overview-refresh">
    <div className="sensor-panels">{metrics.map(metric => <DashboardChart key={metric.key} metric={metric.key} />)}</div>
    <div className="overview-bottom">
      <section className="readings-panel" aria-label="Giá trị cảm biến hiện tại">{metrics.map(({ key, name, icon: Icon, unit }) => <article className={`reading-tile reading-${key}`} key={key}>
        <span className="sensor-symbol"><Icon size={28} /></span><div><p>{name}{key === 'motion' && <small> · Giả lập</small>}</p><strong>{key === 'motion' ? motion ?? '--' : summary?.sensors?.[key]?.value ?? '--'} <small>{summary?.sensors?.[key]?.unit ?? unit}</small></strong>{key === 'motion' && <span className={`motion-state ${motion ? 'detected' : ''}`}>{motion ? 'Có chuyển động' : 'Không chuyển động'}</span>}</div>
      </article>)}</section>
      <section className="controls-panel"><header><h2>Điều khiển thiết bị</h2><div className="quick-device-actions"><button className="quick-device-action on" disabled={pendingDeviceIds.length > 0 || !devices.length} onClick={onAllOn}><Power size={18} /> Bật tất cả</button><button className="quick-device-action off" disabled={pendingDeviceIds.length > 0 || !devices.length} onClick={onAllOff}><Power size={18} /> Tắt tất cả</button></div></header>
        <div className="four-device-grid">{devices.map(device => <article className={`control-tile control-${device.id}`} key={device.id}><Lightbulb size={34} strokeWidth={1.4} /><div><strong>{device.id === 'LED_04' ? 'Đèn 4' : device.name}</strong>{pendingDeviceIds.includes(device.id) && <small>Đang xử lý…</small>}</div><Toggle on={device.on} onChange={() => onToggle(device.id)} label={`${device.on ? 'Tắt' : 'Bật'} ${device.name}`} disabled={pendingDeviceIds.includes(device.id)} busy={pendingDeviceIds.includes(device.id)} /></article>)}</div>
        {!devices.some(device => device.id === 'LED_04') && <p className="device-setup-note">Khởi động lại backend để thêm Đèn 4 vào danh sách thiết bị.</p>}
      </section>
    </div>
  </div>
}
