import {
  Lightbulb,
  Power,
  Sun,
  Thermometer,
  Droplets,
} from 'lucide-react'
import Toggle from '../../components/common/Toggle'
import MetricCard from './components/MetricCard'
import SensorChart from './components/SensorChart'

export default function OverviewPage({
  summary,
  devices,
  onToggle,
  onAllOn,
  onAllOff,
  pendingDeviceIds = [],
}) {
  const temperature = summary?.sensors?.temperature
  const humidity = summary?.sensors?.humidity
  const light = summary?.sensors?.light

  return (
    <div className="page overview-page">
      <div className="metrics-grid">
        <MetricCard label="Nhiệt độ" value={temperature?.value ?? '--'} unit={temperature?.unit ?? '°C'} icon={Thermometer} tone="lime" />
        <MetricCard label="Độ ẩm" value={humidity?.value ?? '--'} unit={humidity?.unit ?? '%'} icon={Droplets} tone="green" />
        <MetricCard label="Ánh sáng" value={light?.value ?? '--'} unit={light?.unit ?? 'digital'} icon={Sun} tone="yellow" />
      </div>

      <div className="dashboard-grid">
        <SensorChart />
        <section className="card quick-devices">
          <div className="section-head">
            <div><h2>Thiết bị</h2><p>{devices.filter((device) => device.on).length}/{devices.length} đang hoạt động</p></div>
            <div className='quick-device-actions'>
              <button className='quick-device-action on' onClick={onAllOn} disabled={pendingDeviceIds.length > 0}>
                <Power size={13} /> Bật tất cả
              </button>
              <button className='quick-device-action off' onClick={onAllOff} disabled={pendingDeviceIds.length > 0}>
                <Power size={13} /> Tắt tất cả
              </button>
            </div>
          </div>
          <div className="quick-list">
            {devices.slice(0, 4).map((device) => {
              const pending = pendingDeviceIds.includes(device.id)
              return (
              <div className={`quick-device ${pending ? 'pending' : ''}`} key={device.id}>
                <span className={`device-icon ${device.on ? 'active' : ''}`}><Lightbulb size={18} /></span>
                <div><strong>{device.name}</strong><span>{pending ? 'Đang xử lý...' : device.room}</span></div>
                <Toggle
                  on={device.on}
                  onChange={() => onToggle(device.id)}
                  label={`${device.on ? 'Tắt' : 'Bật'} ${device.name}`}
                  disabled={pending}
                  busy={pending}
                />
              </div>
              )
            })}
          </div>
        </section>

      </div>
    </div>
  )
}
