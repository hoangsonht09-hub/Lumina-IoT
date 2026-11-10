import { useEffect, useState } from 'react'
import {
  Bell,
  ChevronDown,
  Droplets,
  Lightbulb,
  Menu,
  Settings,
} from 'lucide-react'
import { PAGE_TITLES } from '../../config/navigation'
import profileImage from '../../features/profile/profile.jpg'
import { iotApi } from '../../services/iotApi'

const AVATAR_STORAGE_KEY = 'lumina_profile_avatar'
const PROFILE_UPDATED_EVENT = 'lumina:profile-updated'

export default function Topbar({ page, onMenu }) {
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [profile, setProfile] = useState(null)
  const [avatarUrl, setAvatarUrl] = useState(() => localStorage.getItem(AVATAR_STORAGE_KEY) ?? profileImage)

  useEffect(() => {
    let active = true

    iotApi.getProfile()
      .then((data) => {
        if (active) setProfile(data)
      })
      .catch(() => {})

    const handleProfileUpdated = (event) => {
      if (event.detail) setProfile((current) => ({ ...current, ...event.detail }))
      if (event.detail?.avatarUrl) setAvatarUrl(event.detail.avatarUrl)
    }
    window.addEventListener(PROFILE_UPDATED_EVENT, handleProfileUpdated)

    return () => {
      active = false
      window.removeEventListener(PROFILE_UPDATED_EVENT, handleProfileUpdated)
    }
  }, [])

  return (
    <header className="topbar">
      <div className="topbar-title">
        <button className="mobile-menu" onClick={onMenu} aria-label="Mở menu"><Menu size={22} /></button>
        <span>{PAGE_TITLES[page]}</span>
      </div>

      <div className="top-actions">
        <button
          className="icon-button notification-button"
          aria-label="Thông báo"
          onClick={() => setNotificationsOpen((open) => !open)}
        >
          <Bell size={19} />
          <span className="notification-dot" />
        </button>
        <button className="icon-button" aria-label="Cài đặt"><Settings size={19} /></button>
        <div className="top-divider" />
        <button className="user-menu">
          <div className="avatar avatar-small">
            <img src={avatarUrl} alt="Ảnh đại diện" />
          </div>
          <div><strong>{profile?.fullName ?? 'Đang tải...'}</strong><span>{profile?.organization ?? 'Sinh viên PTIT'}</span></div>
          <ChevronDown size={15} />
        </button>
      </div>

      {notificationsOpen && (
        <div className="notification-popover card">
          <div className="popover-head"><strong>Thông báo</strong><span>2 mới</span></div>
          <div className="notice">
            <span className="notice-icon warn"><Droplets size={16} /></span>
            <div><strong>Độ ẩm đang cao</strong><p>Cảm biến khu vườn ghi nhận 81%.</p><small>2 phút trước</small></div>
          </div>
          <div className="notice">
            <span className="notice-icon"><Lightbulb size={16} /></span>
            <div><strong>LED 03 đã bật</strong><p>Thiết bị hoạt động theo lịch.</p><small>30 phút trước</small></div>
          </div>
        </div>
      )}
    </header>
  )
}
