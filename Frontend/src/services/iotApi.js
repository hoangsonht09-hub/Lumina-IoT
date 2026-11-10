import { apiClient } from './apiClient'
import { authSession } from './apiClient'
import { demoApi } from './demoApi'

const backendApi = {
  getDashboard: () => apiClient.get('/dashboard/summary'),
  getDevices: () => apiClient.get('/devices'),
  controlDevice: (id, isOn, brightness) => apiClient.post(`/devices/${id}/control`, { isOn, brightness }),
  controlAll: (isOn) => apiClient.post('/devices/control-all', { isOn }),
  getCommandStatus: (commandId) => apiClient.get(`/devices/commands/${commandId}`),
  getSensorHistory: (params = {}) => apiClient.get('/sensors/history', { page: 1, limit: 10, direction: 'desc', ...params }),
  getActions: (params = {}) => apiClient.get('/actions', { page: 1, limit: 10, direction: 'desc', ...params }),
  getProfile: () => apiClient.get('/profile'),
  updateProfile: (profile) => apiClient.patch('/profile', profile),
}

export const iotApi = Object.fromEntries(Object.entries(backendApi).map(([name, action]) => [
  name, (...args) => authSession.isDemo() && demoApi[name] ? demoApi[name](...args) : action(...args),
]))
