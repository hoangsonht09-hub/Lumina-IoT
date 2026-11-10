import { useCallback } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import { DEFAULT_PAGE, isValidPage } from '../../config/navigation'

export default function DashboardLayout({ sidebarOpen, onOpenMenu, onCloseMenu, onLogout }) {
  const location = useLocation()
  const navigate = useNavigate()
  const pathPage = location.pathname.split('/').filter(Boolean)[0] || DEFAULT_PAGE
  const activePage = isValidPage(pathPage) ? pathPage : DEFAULT_PAGE

  const handleNavigate = useCallback((page) => {
    const nextPage = isValidPage(page) ? page : DEFAULT_PAGE
    navigate(nextPage === DEFAULT_PAGE ? '/' : '/' + nextPage)
  }, [navigate])

  return (
    <div className="app-shell">
      <Sidebar
        active={activePage}
        onNavigate={handleNavigate}
        open={sidebarOpen}
        onClose={onCloseMenu}
        onLogout={onLogout}
      />
      <div className="main-shell">
        <Topbar page={activePage} onMenu={onOpenMenu} />
        <main><Outlet /></main>
      </div>
    </div>
  )
}
