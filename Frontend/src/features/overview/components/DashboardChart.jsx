import { useEffect, useState } from 'react'
import { Thermometer, Droplets, Sun, Activity } from 'lucide-react'
import { iotApi } from '../../../services/iotApi'

const configs = {
  temperature: { title: 'Đồ thị nhiệt độ', icon: Thermometer, color: '#1496ff', unit: '°C' },
  humidity: { title: 'Đồ thị độ ẩm', icon: Droplets, color: '#05b33e', unit: '%' },
  light: { title: 'Đồ thị ánh sáng', icon: Sun, color: '#ffb000', unit: ' lux' },
  motion: { title: 'Đồ thị chuyển động', icon: Activity, color: '#803cff', unit: '' },
}

export default function DashboardChart({ metric }) {
  const [rows, setRows] = useState([])
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const result = await iotApi.getSensorHistory({ type: metric, page: 1, limit: 10, direction: 'desc' })
        if (active) { setRows(result.items.filter(row => row.type === metric).reverse()); setError('') }
      } catch { if (active) setError('Không tải được dữ liệu cảm biến') }
    }
    load()
    const timer = window.setInterval(load, 5000)
    return () => { active = false; window.clearInterval(timer) }
  }, [metric])
  const config = configs[metric]
  const Icon = config.icon
  const samples = rows
  const values = samples.map(row => Number(row.value))
  const low = values.length ? Math.min(...values) : 0
  const high = values.length ? Math.max(...values) : 1
  const padding = Math.max((high - low) * .25, metric === 'light' ? 5 : .5)
  const min = metric === 'motion' ? 0 : Math.max(0, low - padding)
  const max = metric === 'motion' ? 1 : high + padding
  const points = values.map((v, i) => [20 + i * 560 / Math.max(values.length - 1, 1), 118 - (v - min) / (max - min) * 94])
  const path = points.map(([x, y], i) => i === 0 ? `M${x},${y}` : metric === 'motion' ? `H${x}V${y}` : `L${x},${y}`).join(' ')
  const area = points.length ? `${path} L${points.at(-1)[0]},132 L${points[0][0]},132 Z` : ''
  const formatValue = v => `${Number(v.toFixed(1))}${config.unit}`
  return <section className={`sensor-panel sensor-panel-${metric}`} style={{ '--sensor-color': config.color }}>
    <header><span className="sensor-symbol"><Icon size={25} /></span><h2>{config.title}</h2></header>
    {values.length ? <div className="sensor-plot">
      <div className="sensor-axis">{(metric === 'motion' ? [1, 0] : [max, (max + min) / 2, min]).map((v, i) => <span key={i}>{formatValue(v)}{metric === 'motion' && <small>{v ? '(Có)' : '(Không)'}</small>}</span>)}</div>
      <div className="sensor-drawing"><svg viewBox="0 0 600 145" preserveAspectRatio="none" role="img" aria-label={config.title}>
        <defs><linearGradient id={`fill-${metric}`} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor={config.color} stopOpacity=".25" /><stop offset="100%" stopColor={config.color} stopOpacity=".02" /></linearGradient></defs>
        {[24, 71, 118].map(y => <line key={y} x1="0" x2="600" y1={y} y2={y} stroke="#e7efff" strokeDasharray="6 5" />)}
        <path d={area} fill={`url(#fill-${metric})`} /><path d={path} stroke={config.color} strokeWidth="2.5" fill="none" strokeLinejoin="round" />
        {metric !== 'motion' && points.map(([x, y], i) => <circle key={i} cx={x} cy={y} r="3.5" fill={config.color}><title>{formatValue(values[i])}</title></circle>)}
      </svg><div className="sensor-times">{samples.filter((_, i) => i % 2 === 0 || i === samples.length - 1).map((row, i) => <span key={i}>{new Date(row.recordedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>)}</div></div>
    </div> : <div className="sensor-wait" role="status">{error || (metric === 'motion' ? 'Đang chờ mẫu chuyển động giả lập từ backend' : 'Đang chờ dữ liệu MQTT từ ESP32')}</div>}
    {error && values.length > 0 && <small role="status">{error} · Đang hiển thị dữ liệu đã tải</small>}
  </section>
}
