import { useEffect, useMemo, useState } from 'react'
import { Lightbulb, Search } from 'lucide-react'
import PageIntro from '../../components/common/PageIntro'
import {
  DataTableHead,
  EmptyState,
  StatusBadge,
  TableFooter,
} from '../../components/common/DataTable'
import CopyableDateTime from '../../components/common/CopyableDateTime'
import { iotApi } from '../../services/iotApi'
import { formatApiDateTime } from '../../utils/dateTime'

const getActionClassName = (actionName) => {
  if (actionName === 'Mất kết nối') return 'action-chip disconnected'
  if (actionName.startsWith('Tắt')) return 'action-chip off'
  return 'action-chip'
}

export default function ActivityHistoryPage() {
  const [activityRows, setActivityRows] = useState([])
  const [loadError, setLoadError] = useState('')
  const [totalItems, setTotalItems] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [allDevices, setAllDevices] = useState([])
  const [device, setDevice] = useState('all')
  const [action, setAction] = useState('all')
  const [status, setStatus] = useState('all')
  const [timeQuery, setTimeQuery] = useState('')
  const [appliedTimeQuery, setAppliedTimeQuery] = useState('')
  const [sortDirection, setSortDirection] = useState('desc')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(5)

  useEffect(() => {
    let active = true
    const load = () => iotApi.getActions({
      page,
      limit: pageSize,
      direction: sortDirection,
      ...(device !== 'all' ? { device } : {}),
      ...(action !== 'all' ? { action } : {}),
      ...(status !== 'all' ? { status } : {}),
      ...(appliedTimeQuery ? { timeQuery: appliedTimeQuery } : {}),
    }).then((result) => {
      if (!active) return
      setLoadError('')
      setActivityRows(result.items.map((row) => ({
        id: row.id,
        device: row.deviceName,
        room: 'Phòng IoT 01',
        action: row.actionLabel,
        status: row.status,
        user: row.performedBy.name,
        ...formatApiDateTime(row.createdAt),
      })))
      setTotalItems(result.pagination.totalItems)
      setTotalPages(Math.max(1, result.pagination.totalPages))
    }).catch((error) => {
      if (!active) return
      setActivityRows([])
      setLoadError(error.message || 'Không thể tải lịch sử hoạt động.')
      setTotalItems(0)
      setTotalPages(1)
    })
    load()
    const timer = window.setInterval(load, 5000)
    return () => { active = false; window.clearInterval(timer) }
  }, [action, appliedTimeQuery, device, page, pageSize, sortDirection, status])

  useEffect(() => {
    let active = true
    iotApi.getDevices().then((devices) => {
      if (active) setAllDevices([...devices.map((item) => item.name), 'Đèn 4 (tự động)'])
    }).catch(() => {})
    return () => { active = false }
  }, [])

  const deviceOptions = useMemo(() => [...new Set(allDevices)], [allDevices])

  const updateFilter = (setter) => (event) => {
    setter(event.target.value)
    setPage(1)
  }

  const toggleTimeSort = () => {
    setSortDirection((currentDirection) => (currentDirection === 'desc' ? 'asc' : 'desc'))
    setPage(1)
  }

  const handleTimeSearch = (event) => {
    event.preventDefault()
    setAppliedTimeQuery(timeQuery.trim())
    setPage(1)
  }

  const handlePageSizeChange = (nextPageSize) => {
    setPageSize(nextPageSize)
    setPage(1)
  }

  return (
    <div className="page">
      <PageIntro
        eyebrow="Nhật ký hệ thống"
        title="Lịch sử bật/tắt"
      />

      <section className="card table-card">
        <form className="filters history-filters" onSubmit={handleTimeSearch}>
          <div className="search-control with-hint">
            <label className="search-field">
              <Search size={17} />
              <input
                type="text"
                aria-label="Nhập thời gian cần tìm"
                aria-describedby="activity-time-hint"
                value={timeQuery}
                onChange={(event) => setTimeQuery(event.target.value)}
                placeholder="VD: 2026-08-18 10:45:30"
              />
            </label>
            <small className="time-input-hint" id="activity-time-hint">
              Thứ tự: YYYY-MM-DD HH:mm:ss; có thể bỏ dần giây, phút, giờ, ngày hoặc tháng
            </small>
          </div>
          <label className="select-field">
            <select aria-label="Lọc theo tên thiết bị" value={device} onChange={updateFilter(setDevice)}>
              <option value="all">Tất cả thiết bị</option>
              {deviceOptions.map((deviceName) => (
                <option value={deviceName} key={deviceName}>{deviceName}</option>
              ))}
            </select>
          </label>
          <label className="select-field">
            <select aria-label="Lọc theo hành động" value={action} onChange={updateFilter(setAction)}>
              <option value="all">Tất cả hành động</option>
              <option value="manual_turn_on">Bật thiết bị</option>
              <option value="manual_turn_off">Tắt thiết bị</option>
              <option value="auto_turn_on">Bật tự động</option>
              <option value="auto_turn_off">Tắt tự động</option>
              <option value="mất kết nối">Mất kết nối</option>
            </select>
          </label>
          <label className="select-field">
            <select value={status} onChange={updateFilter(setStatus)}>
              <option value="all">Tất cả trạng thái</option>
              <option value="success">Thành công</option>
              <option value="failed">Thất bại</option>
              <option value="pending">Đang xử lý</option>
            </select>
          </label>
          <button type="submit" className="activity-search-button">
            <Search size={16} />
            Tìm
          </button>
        </form>

        <DataTableHead
          columns={[
            'Thiết bị',
            'Hành động',
            'Người thực hiện',
            'Trạng thái',
            {
              key: 'time',
              label: 'Thời gian',
              direction: sortDirection,
              onSort: toggleTimeSort,
            },
          ]}
        />
        <div className="table-body history-table">
          {activityRows.length ? activityRows.map((row) => (
            <div className="table-row" key={row.id}>
              <span className="sensor-name">
                <i className="sensor-dot device"><Lightbulb size={15} /></i>
                <span>{row.device}<small>{row.room}</small></span>
              </span>
              <span className={getActionClassName(row.action)}>{row.action}</span>
              <span>{row.user}</span>
              <StatusBadge status={row.status} />
              <CopyableDateTime value={row.displayDateTime} />
            </div>
          )) : <EmptyState message={loadError} />}
        </div>
        <TableFooter
          count={totalItems}
          page={page}
          maxPage={totalPages}
          pageSize={pageSize}
          onPage={setPage}
          onPageSize={handlePageSizeChange}
        />
      </section>
    </div>
  )
}
