import { ArrowDown, ArrowUp, ChevronLeft, ChevronRight, Search } from 'lucide-react'

const PAGE_SIZE_OPTIONS = [5, 10, 20]

const STATUS_LABELS = {
  normal: 'Bình thường',
  warning: 'Cần chú ý',
  low: 'Mức thấp',
  success: 'Thành công',
  failed: 'Thất bại',
  pending: 'Đang xử lý',
}

export function DataTableHead({ columns }) {
  return (
    <div className='table-head'>
      {columns.map((column) => {
        const config = typeof column === 'string' ? { key: column, label: column } : column

        return (
          <span key={config.key}>
            {config.onSort ? (
              <button
                type='button'
                className='table-sort-button'
                onClick={config.onSort}
                aria-label={config.direction === 'asc' ? 'Sort newest first' : 'Sort oldest first'}
              >
                {config.label}
                {config.direction === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} />}
              </button>
            ) : config.label}
          </span>
        )
      })}
    </div>
  )
}

export function StatusBadge({ status }) {
  return (
    <span className={`status-badge ${status}`}>
      <i />
      {STATUS_LABELS[status]}
    </span>
  )
}

export function EmptyState({ message = '' }) {
  return (
    <div className="empty-state">
      <Search size={25} />
      <strong>{message ? 'Không thể tải dữ liệu' : 'Không tìm thấy dữ liệu'}</strong>
      <span>{message || 'Hãy thử thay đổi bộ lọc.'}</span>
    </div>
  )
}

function getVisiblePages(page, maxPage) {
  if (maxPage <= 7) return Array.from({ length: maxPage }, (_, index) => index + 1)

  const pages = [1]
  const start = Math.max(2, page - 1)
  const end = Math.min(maxPage - 1, page + 1)

  if (start > 2) pages.push('left-ellipsis')
  for (let number = start; number <= end; number += 1) pages.push(number)
  if (end < maxPage - 1) pages.push('right-ellipsis')
  pages.push(maxPage)
  return pages
}

export function TableFooter({ count, page, maxPage, pageSize, onPage, onPageSize }) {
  const firstRow = count ? (page - 1) * pageSize + 1 : 0
  const lastRow = Math.min(page * pageSize, count)
  const visiblePages = getVisiblePages(page, maxPage)

  return (
    <div className="table-footer">
      <div className="table-footer-info">
        <span>Hiển thị {firstRow}–{lastRow} trong {count} kết quả</span>
        <label className="rows-per-page">
          <span>Số dòng/trang</span>
          <select
            aria-label="Chọn số dòng mỗi trang"
            value={pageSize}
            onChange={(event) => onPageSize(Number(event.target.value))}
          >
            {PAGE_SIZE_OPTIONS.map((option) => (
              <option value={option} key={option}>{option}</option>
            ))}
          </select>
        </label>
      </div>
      <div className="pagination">
        <button type="button" disabled={page === 1} onClick={() => onPage(page - 1)}>
          <ChevronLeft size={15} />
        </button>
        {visiblePages.map((pageNumber) => typeof pageNumber === 'number' ? (
          <button
            key={pageNumber}
            type="button"
            className={page === pageNumber ? 'active' : ''}
            aria-current={page === pageNumber ? 'page' : undefined}
            onClick={() => onPage(pageNumber)}
          >
            {pageNumber}
          </button>
        ) : <span aria-hidden="true" key={pageNumber}>…</span>)}
        <button type="button" disabled={page === maxPage} onClick={() => onPage(page + 1)}>
          <ChevronRight size={15} />
        </button>
      </div>
    </div>
  )
}
