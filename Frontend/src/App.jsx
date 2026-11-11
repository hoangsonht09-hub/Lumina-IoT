import { useCallback, useEffect, useState } from 'react'
import { HashRouter, Navigate, Route, Routes } from 'react-router-dom'
import Toast from './components/common/Toast'
import DashboardLayout from './components/layout/DashboardLayout'
import ActivityHistoryPage from './features/activity/ActivityHistoryPage'
import LoginPage from './features/auth/LoginPage'
import OverviewPage from './features/overview/OverviewDashboard'
import ProfilePage from './features/profile/ProfilePage'
import SensorHistoryPage from './features/sensors/SensorHistoryPage'
import { authSession } from './services/apiClient'
import { iotApi } from './services/iotApi'

const COMMAND_POLL_INTERVAL_MS = 500
const COMMAND_POLL_ATTEMPTS = 16

const delay = (milliseconds) => new Promise((resolve) => window.setTimeout(resolve, milliseconds))

async function waitForCommand(commandId) {
  let command = await iotApi.getCommandStatus(commandId)
  for (let attempt = 0; attempt < COMMAND_POLL_ATTEMPTS && command.status === 'pending'; attempt += 1) {
    await delay(COMMAND_POLL_INTERVAL_MS)
    command = await iotApi.getCommandStatus(commandId)
  }
  return command
}

function commandFailureMessage(command) {
  if (command?.failureReason === 'HARDWARE_ACK_TIMEOUT') {
    return 'Thiết bị không phản hồi trong thời gian cho phép'
  }
  return 'Phần cứng không thực hiện được lệnh'
}

function AppRoutes({ onLogout }) {
  const [devices, setDevices] = useState([])
  const [summary, setSummary] = useState(null)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [toast, setToast] = useState('')
  const [pendingDeviceIds, setPendingDeviceIds] = useState([])

  const refreshOverview = useCallback(async (showError = false) => {
    try {
      const [summaryResult, devicesResult] = await Promise.allSettled([
        iotApi.getDashboard(),
        iotApi.getDevices(),
      ])
      const nextSummary = summaryResult.status === 'fulfilled' ? summaryResult.value : null
      if (nextSummary) setSummary(nextSummary)
      if (devicesResult.status === 'fulfilled') setDevices(devicesResult.value.map((device) => ({
        ...device,
        on: device.isOn,
        room: nextSummary?.room?.name ?? 'Phòng IoT 01',
      })))
      const failure = [summaryResult, devicesResult].find(result => result.status === 'rejected')
      if (failure && showError && failure.reason.status !== 401) setToast(failure.reason.message)
    } catch (error) {
      if (showError && error.status !== 401) setToast(error.message)
    }
  }, [])

  useEffect(() => {
    const initialTimer = window.setTimeout(() => refreshOverview(true), 0)
    const timer = window.setInterval(() => refreshOverview(false), 3000)
    return () => {
      window.clearTimeout(initialTimer)
      window.clearInterval(timer)
    }
  }, [refreshOverview])

  const toggleDevice = async (id) => {
    const targetDevice = devices.find((device) => device.id === id)
    if (!targetDevice || pendingDeviceIds.includes(id)) return
    setPendingDeviceIds((current) => [...current, id])
    try {
      const accepted = await iotApi.controlDevice(id, !targetDevice.on, targetDevice.brightness)
      const command = accepted.status === 'pending'
        ? await waitForCommand(accepted.commandId)
        : accepted
      if (command.status !== 'success') throw new Error(commandFailureMessage(command))
      await refreshOverview()
      setToast(`${targetDevice.name} đã được ${targetDevice.on ? 'tắt' : 'bật'}`)
    } catch (error) {
      setToast(error.message)
    } finally {
      await refreshOverview()
      setPendingDeviceIds((current) => current.filter((deviceId) => deviceId !== id))
    }
  }

  const controlAllDevices = async (isOn) => {
    if (pendingDeviceIds.length > 0 || devices.length === 0) return
    const deviceIds = devices.map((device) => device.id)
    setPendingDeviceIds(deviceIds)
    try {
      const accepted = await iotApi.controlAll(isOn)
      const commands = await Promise.all((accepted.commands ?? []).map((command) => (
        command.status === 'pending' ? waitForCommand(command.commandId) : command
      )))
      const failedCommand = commands.find((command) => command.status !== 'success')
      if (failedCommand) throw new Error(commandFailureMessage(failedCommand))
      await refreshOverview()
      setToast(`Đã ${isOn ? 'bật' : 'tắt'} tất cả thiết bị`)
    } catch (error) {
      setToast(error.message)
    } finally {
      await refreshOverview()
      setPendingDeviceIds([])
    }
  }

  const closeToast = useCallback(() => setToast(''), [])

  return (
    <>
      <Routes>
        <Route
          element={(
            <DashboardLayout
              sidebarOpen={sidebarOpen}
              onOpenMenu={() => setSidebarOpen(true)}
              onCloseMenu={() => setSidebarOpen(false)}
              onLogout={onLogout}
            />
          )}
        >
          <Route
            index
            element={(
              <OverviewPage
                summary={summary}
                devices={devices}
                onToggle={toggleDevice}
                onAllOn={() => controlAllDevices(true)}
                onAllOff={() => controlAllDevices(false)}
                pendingDeviceIds={pendingDeviceIds}
              />
            )}
          />
          <Route path='sensors' element={<SensorHistoryPage />} />
          <Route path='history' element={<ActivityHistoryPage />} />
          <Route path='profile' element={<ProfilePage onNotify={setToast} />} />
          <Route path='*' element={<Navigate to='/' replace />} />
        </Route>
      </Routes>
      {toast && <Toast message={toast} onClose={closeToast} />}
    </>
  )
}

export default function App() {
  const [authenticated, setAuthenticated] = useState(authSession.hasToken())

  const handleLogout = useCallback(() => {
    authSession.clear()
    setAuthenticated(false)
  }, [])

  useEffect(() => {
    const handleExpired = () => setAuthenticated(false)
    window.addEventListener('lumina:auth-expired', handleExpired)
    return () => window.removeEventListener('lumina:auth-expired', handleExpired)
  }, [])

  if (!authenticated) return <LoginPage onAuthenticated={() => setAuthenticated(true)} />

  return (
    <HashRouter>
      <AppRoutes onLogout={handleLogout} />
    </HashRouter>
  )
}
