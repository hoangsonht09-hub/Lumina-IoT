import { useEffect, useMemo, useState } from 'react'
import { iotApi } from '../../../services/iotApi'
import { formatApiDateTime } from '../../../utils/dateTime'
import { authSession } from '../../../services/apiClient'

const METRIC_CONFIG = {
  temperature: { label: 'Nhiệt độ', unit: '°C', color: '#b9ef38', min: 23, max: 31 },
  humidity: { label: 'Độ ẩm', unit: '%', color: '#33aa78', min: 60, max: 80 },
  light: { label: 'Ánh sáng', unit: ' lux', color: '#dbff70', min: 0, max: 1000 },
}

const METRIC_LABELS = {
  temperature: 'Nhiệt độ',
  humidity: 'Độ ẩm',
  light: 'Ánh sáng',
}

export default function SensorChart() {
  const [metric, setMetric] = useState('temperature')
  const [rows, setRows] = useState([])
  const config = METRIC_CONFIG[metric]

  useEffect(() => {
    let active = true
    const load = () => iotApi.getSensorHistory({
      type: metric,
      page: 1,
      limit: 7,
      direction: 'desc',
    }).then((result) => {
      if (active) setRows(result.items)
    }).catch(() => {})
    load()
    const timer = window.setInterval(load, 5000)
    return () => { active = false; window.clearInterval(timer) }
  }, [metric])

  const samples = useMemo(() => rows
    .filter((row) => row.type === metric)
    .slice(0, 7)
    .reverse(), [rows, metric])
  const values = samples.map((row) => row.value)
  const labels = samples.map((row) => authSession.isDemo() ? `${row.recordedAt.slice(8, 10)}/${row.recordedAt.slice(5, 7)}` : formatApiDateTime(row.recordedAt).time.slice(0, 5))
  const rangeMin = values.length ? Math.min(...values) : config.min
  const rangeMax = values.length ? Math.max(...values) : config.max
  const padding = rangeMax === rangeMin ? Math.max(Math.abs(rangeMax) * 0.1, 1) : (rangeMax - rangeMin) * 0.15
  const min = rangeMin - padding
  const max = rangeMax + padding
  const points = values.map((value, index) => {
    const x = values.length === 1 ? 328 : 36 + (index * 584) / (values.length - 1)
    const y = 174 - ((value - min) / (max - min)) * 126
    return `${x},${y}`
  }).join(' ')
  const areaPoints = `36,190 ${points} 620,190`

  return (
    <section className="card chart-card">
      <div className="section-head">
        <div><h2>Biểu đồ cảm biến</h2><p>{authSession.isDemo() ? '7 mẫu dữ liệu giả lập gần nhất' : '7 mẫu MQTT gần nhất'}</p></div>
        <div className="segmented">
          {Object.keys(METRIC_CONFIG).map((key) => (
            <button
              className={metric === key ? 'active' : ''}
              key={key}
              onClick={() => setMetric(key)}
            >
              {METRIC_LABELS[key]}
            </button>
          ))}
        </div>
      </div>

      {values.length ? <div className="chart-wrap">
        <div className="chart-y-labels">
          <span>{Math.round(max * 10) / 10}{config.unit}</span>
          <span>{Math.round(((max + min) / 2) * 10) / 10}{config.unit}</span>
          <span>{Math.round(min * 10) / 10}{config.unit}</span>
        </div>
        <svg viewBox="0 0 656 210" preserveAspectRatio="none" role="img" aria-label={`Biểu đồ ${config.label}`}>
          <defs>
            <linearGradient id="chartArea" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor={config.color} stopOpacity="0.28" />
              <stop offset="1" stopColor={config.color} stopOpacity="0" />
            </linearGradient>
          </defs>
          {[48, 111, 174].map((y) => <line key={y} x1="36" x2="620" y1={y} y2={y} className="grid-line" />)}
          <polygon points={areaPoints} fill="url(#chartArea)" />
          <polyline points={points} fill="none" stroke={config.color} strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />
          {points.split(' ').map((point, index) => {
            const [cx, cy] = point.split(',')
            return (
              <circle
                key={point}
                cx={cx}
                cy={cy}
                r={index === values.length - 1 ? 5 : 3.2}
                fill={config.color}
                stroke="#fff"
                strokeWidth="2"
              />
            )
          })}
        </svg>
        <div className="chart-labels">
          {labels.map((label, index) => <span key={`${label}-${index}`}>{label}</span>)}
        </div>
      </div> : <div className="empty-state"><strong>Đang chờ dữ liệu MQTT</strong><span>Biểu đồ sẽ xuất hiện khi ESP32 gửi mẫu đầu tiên.</span></div>}
    </section>
  )
}
