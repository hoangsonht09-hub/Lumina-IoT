import { Activity, History, Home, UserRound } from 'lucide-react'

export const NAV_ITEMS = [
  { id: 'overview', label: 'Tổng quan', icon: Home },
  { id: 'sensors', label: 'Lịch sử cảm biến', icon: Activity },
  { id: 'history', label: 'Lịch sử bật/tắt', icon: History },
  { id: 'profile', label: 'Thông tin cá nhân', icon: UserRound }
]

export const PAGE_TITLES = Object.fromEntries(
  NAV_ITEMS.map(({ id, label }) => [id, label]),
)

export const DEFAULT_PAGE = 'overview'

export const isValidPage = (page) => NAV_ITEMS.some(({ id }) => id === page)
