const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api').replace(/\/$/, '')
const TOKEN_KEY = 'lumina_access_token'
const DEMO_TOKEN = 'lumina-local-demo'
const DEMO_ENABLED = import.meta.env.VITE_ENABLE_DEMO === 'true'
if (!DEMO_ENABLED && localStorage.getItem(TOKEN_KEY) === DEMO_TOKEN) localStorage.removeItem(TOKEN_KEY)

export class ApiError extends Error {
  constructor(message, status, errorCode) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
  }
}

export const authSession = {
  isDemo: () => localStorage.getItem(TOKEN_KEY) === DEMO_TOKEN,
  hasToken: () => Boolean(localStorage.getItem(TOKEN_KEY)),
  clear: () => localStorage.removeItem(TOKEN_KEY),
  login: async (email, password) => {
    if (DEMO_ENABLED && email === 'admin@lumina.vn' && password === '123456') {
      localStorage.setItem(TOKEN_KEY, DEMO_TOKEN)
      return { fullName: 'Nguyễn Hoàng Sơn' }
    }
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    const payload = await response.json().catch(() => null)
    if (!response.ok || !payload?.success) {
      throw new ApiError(payload?.message ?? 'Không thể đăng nhập vào backend', response.status, payload?.errorCode)
    }
    localStorage.setItem(TOKEN_KEY, payload.data.accessToken)
    return payload.data.user
  },
}

async function request(path, options = {}) {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) throw new ApiError('Vui lòng đăng nhập', 401, 'AUTH_REQUIRED')

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
      Authorization: `Bearer ${token}`,
    },
  })

  if (response.status === 401) {
    authSession.clear()
    window.dispatchEvent(new Event('lumina:auth-expired'))
  }

  const contentType = response.headers.get('content-type') ?? ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.blob()
  if (!response.ok || (payload && 'success' in payload && !payload.success)) {
    throw new ApiError(payload?.message ?? `API trả về lỗi ${response.status}`, response.status, payload?.errorCode)
  }
  return payload?.data ?? payload
}

function queryString(params) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value)
  })
  const value = query.toString()
  return value ? `?${value}` : ''
}

export const apiClient = {
  get: (path, params = {}) => request(`${path}${queryString(params)}`),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
}
