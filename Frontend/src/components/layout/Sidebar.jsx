import { ChevronDown, CircleHelp, LogOut } from 'lucide-react'
import { NAV_ITEMS } from '../../config/navigation'
import Brand from './Brand'

export default function Sidebar({ active, onNavigate, open, onClose, onLogout }) {
  const handleNavigate = (page) => {
    onNavigate(page)
    onClose()
  }

  const handleLogout = () => {
    onClose()
    onLogout()
  }

  return (
    <>
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <Brand />
        

        <span className="nav-caption">Không gian làm việc</span>
        <nav>
          {NAV_ITEMS.map(({ id, label, icon: Icon }) => (
            <button
              className={`nav-item ${active === id ? 'active' : ''}`}
              key={id}
              onClick={() => handleNavigate(id)}
            >
              <Icon size={18} />
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <button className="nav-item"><CircleHelp size={18} /><span>Trợ giúp</span></button>
          <button className="nav-item logout" onClick={handleLogout}><LogOut size={18} /><span>Đăng xuất</span></button>
        </div>
      </aside>
      {open && <button aria-label="Đóng menu" className="sidebar-backdrop" onClick={onClose} />}
    </>
  )
}
