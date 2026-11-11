import { useEffect, useState } from 'react'
import { Activity, Droplets, Search, Sun, Thermometer } from 'lucide-react'
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

const SENSOR_ICONS = {
  temperature: Thermometer,
  humidity: Droplets,
  light: Sun,
  motion: Activity,
}

function buildHistorySearchParams(appliedSearch) {
  if (!appliedSearch) return {}

  if (appliedSearch.field === 'all') return { search: appliedSearch.value }
  if (appliedSearch.field === 'time') return { timeQuery: appliedSearch.value }

  return {
    type: appliedSearch.field,
    value: Number(appliedSearch.value),
  }
}

export default function SensorHistoryPage() {
  const [sensorRows, setSensorRows] = useState([])
  const [loadError, setLoadError] = useState('')
  const [totalItems, setTotalItems] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [searchField, setSearchField] = useState('all')
  const [searchInput, setSearchInput] = useState('')
  const [appliedSearch, setAppliedSearch] = useState(null)
  const [sortDirection, setSortDirection] = useState('desc')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(5)

  useEffect(() => {
    let active = true
    const load = () => iotApi.getSensorHistory({
      page,
      limit: pageSize,
      direction: sortDirection,
      ...buildHistorySearchParams(appliedSearch),
    }).then((result) => {
      if (!active) return
      setLoadError('')
      setSensorRows(result.items.map((row) => ({
        id: row.id,
        sensor: row.sensorName,
        type: row.type,
        value: row.value,
        unit: row.unit,
        status: row.status,
        ...formatApiDateTime(row.recordedAt),
      })))
      setTotalItems(result.pagination.totalItems)
      setTotalPages(Math.max(1, result.pagination.totalPages))
    }).catch((error) => {
      if (!active) return
      setSensorRows([])
      setLoadError(error.message || 'Không thể tải lịch sử cảm biến.')
      setTotalItems(0)
      setTotalPages(1)
    })
    load()
    const timer = window.setInterval(load, 5000)
    return () => { active = false; window.clearInterval(timer) }
  }, [appliedSearch, page, pageSize, sortDirection])

  const handleFieldChange = (event) => {
    setSearchField(event.target.value)
    setSearchInput('')
    setAppliedSearch(null)
    setPage(1)
  }

  const handleSearch = (event) => {
    event.preventDefault()
    const value = searchInput.trim()

    setAppliedSearch(value ? { field: searchField, value } : null)
    setPage(1)
  }

  const toggleTimeSort = () => {
    setSortDirection((currentDirection) => (currentDirection === 'desc' ? 'asc' : 'desc'))
    setPage(1)
  }

  const handlePageSizeChange = (nextPageSize) => {
    setPageSize(nextPageSize)
    setPage(1)
  }

  return (
    <div className="page">
      <PageIntro
        eyebrow="Dữ liệu cảm biến"
        title="Lịch sử cảm biến"
        description="Theo dõi nhiệt độ, độ ẩm, ánh sáng và chuyển động giả lập (0: không, 1: có)."
      />

      <section className="card table-card">
        <form className='filters sensor-search-bar' onSubmit={handleSearch}>
          <label className='select-field'>
            <select
              aria-label='Chọn trường tìm kiếm'
              value={searchField}
              onChange={handleFieldChange}
            >
              <option value='all'>Tất cả</option>
              <option value='temperature'>Nhiệt độ</option>
              <option value='light'>Ánh sáng</option>
              <option value='humidity'>Độ ẩm</option>
              <option value='motion'>Chuyển động (giả lập)</option>
              <option value='time'>Thời gian</option>
            </select>
          </label>
          <div className={`search-control ${searchField === 'time' ? 'with-hint' : ''}`}>
            <div className='search-field'>
              <Search size={17} />
              {searchField === 'time' ? (
                <input
                  type='text'
                  aria-label='Nhập thời gian cần tìm'
                  aria-describedby='sensor-time-hint'
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  placeholder='VD: 2026-08-18 10:45:30'
                />
              ) : (
                <input
                  type={searchField === 'all' ? 'text' : 'number'}
                  step='any'
                  aria-label={searchField === 'all' ? 'Nhập nội dung cần tìm' : 'Nhập giá trị cảm biến'}
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  placeholder='Nhập giá trị cần tìm...'
                />
              )}
            </div>
            {searchField === 'time' && (
              <small className='time-input-hint' id='sensor-time-hint'>
                Thứ tự: YYYY-MM-DD HH:mm:ss; có thể bỏ dần giây, phút, giờ, ngày hoặc tháng
              </small>
            )}
          </div>
          <button type='submit' className='sensor-search-button'>
            <Search size={16} />
            Tìm
          </button>
        </form>

        <DataTableHead
          columns={[
            'ID',
            'Cảm biến',
            'Giá trị',
            'Đơn vị',
            {
              key: 'time',
              label: 'Thời gian',
              direction: sortDirection,
              onSort: toggleTimeSort,
            },
            'Trạng thái',
          ]}
        />
        <div className="table-body sensor-table">
          {sensorRows.length ? sensorRows.map((row) => {
            const Icon = SENSOR_ICONS[row.type]
            return (
              <div className="table-row" key={row.id}>
                <span className="mono">#{row.id}</span>
                <span className="sensor-name">
                  <i className={`sensor-dot ${row.type}`}><Icon size={15} /></i>
                  <span>{row.sensor}</span>
                </span>
                <strong>{row.value}</strong>
                <span className="muted-text">{row.unit}</span>
                <CopyableDateTime value={row.displayDateTime} />
                <StatusBadge status={row.status} />
              </div>
            )
          }) : <EmptyState message={loadError} />}
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
