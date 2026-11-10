import { useEffect, useState } from 'react'
import {
  AlertCircle,
  BookOpen,
  Camera,
  Code2,
  ExternalLink,
  GitBranch,
  LoaderCircle,
  Palette,
  PencilLine,
  Save,
  X,
} from 'lucide-react'
import PageIntro from '../../components/common/PageIntro'
import profileImage from './profile.jpg'
import { iotApi } from '../../services/iotApi'

const AVATAR_STORAGE_KEY = 'lumina_profile_avatar'
const PROFILE_UPDATED_EVENT = 'lumina:profile-updated'

const DEFAULT_PROFILE_LINKS = {
  github: 'https://github.com/Toanproptit/IOT_Light_Sensor.git',
  figma: 'https://www.figma.com/design/FAFolqqY1l4d1ouGOGe4EN/Untitled?node-id=0-1&p=f&t=hX9CBeMNZOFfyx5F-0',
  projectDocs: 'https://docs.google.com/document/d/1Nup2By1D3d8yEg-9v_vy686105hoH92S/edit?usp=sharing&ouid=113704325966074309678&rtpof=true&sd=true',
  apiDocs: 'http://localhost:8080/swagger-ui/index.html',
}

const PROFILE_LINK_DEFINITIONS = [
  { key: 'github', label: 'GitHub', description: 'Source code dự án', icon: GitBranch, className: 'github' },
  { key: 'figma', label: 'Figma', description: 'UI/UX Design', icon: Palette, className: 'figma' },
  { key: 'projectDocs', label: 'Project Docs', description: 'Tài liệu dự án', icon: BookOpen, className: 'docs' },
  { key: 'apiDocs', label: 'API Document', description: 'REST API Reference', icon: Code2, className: 'api' },
]

function profileToForm(profile) {
  return {
    studentId: profile?.studentId ?? '',
    fullName: profile?.fullName ?? '',
    organization: profile?.organization ?? '',
    email: profile?.email ?? '',
    links: { ...DEFAULT_PROFILE_LINKS, ...profile?.links },
  }
}

function InfoField({ label, value }) {
  return (
    <div className="info-field">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function ProjectLink({ link }) {
  const Icon = link.icon
  const opensInNewTab = link.href.startsWith('http') || link.href.endsWith('.md')

  return (
    <a
      className="project-link"
      href={link.href}
      target={opensInNewTab ? '_blank' : undefined}
      rel={opensInNewTab ? 'noreferrer' : undefined}
    >
      <span className={`link-icon ${link.className}`}><Icon size={20} /></span>
      <span><strong>{link.label}</strong><small>{link.description}</small></span>
      <ExternalLink size={16} />
    </a>
  )
}

export default function ProfilePage({ onNotify }) {
  const [avatarUrl, setAvatarUrl] = useState(() => localStorage.getItem(AVATAR_STORAGE_KEY) ?? profileImage)
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(() => profileToForm(null))
  const [editing, setEditing] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    iotApi.getProfile()
      .then((data) => {
        if (!active) return
        setProfile(data)
        setForm(profileToForm(data))
      })
      .catch((requestError) => {
        if (active && requestError.status !== 401) setError(requestError.message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const handleAvatarChange = (event) => {
    const file = event.target.files?.[0]
    if (!file || !file.type.startsWith('image/')) return
    if (file.size > 2 * 1024 * 1024) {
      setError('Ảnh đại diện không được lớn hơn 2 MB')
      return
    }

    const reader = new FileReader()
    reader.onload = () => {
      if (typeof reader.result !== 'string') return
      try {
        localStorage.setItem(AVATAR_STORAGE_KEY, reader.result)
        setAvatarUrl(reader.result)
        setError('')
        onNotify?.('Đã cập nhật ảnh đại diện trên trình duyệt này')
        window.dispatchEvent(new CustomEvent(PROFILE_UPDATED_EVENT, {
          detail: { ...profile, avatarUrl: reader.result },
        }))
      } catch {
        setError('Không thể lưu ảnh trên trình duyệt. Hãy chọn ảnh có dung lượng nhỏ hơn.')
      }
    }
    reader.readAsDataURL(file)
  }

  const startEditing = () => {
    if (!profile) return
    setForm(profileToForm(profile))
    setError('')
    setEditing(true)
  }

  const cancelEditing = () => {
    setForm(profileToForm(profile))
    setError('')
    setEditing(false)
  }

  const handleInputChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleLinkChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({
      ...current,
      links: { ...current.links, [name]: value },
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const studentId = form.studentId.trim()
    const fullName = form.fullName.trim()
    const organization = form.organization.trim()
    const email = form.email.trim().toLowerCase()
    const links = Object.fromEntries(
      Object.entries(form.links).map(([key, value]) => [key, value.trim()]),
    )

    if (!studentId || !fullName || !organization || !email || Object.values(links).some((value) => !value)) {
      setError('Vui lòng nhập đầy đủ thông tin cá nhân và 4 liên kết dự án')
      return
    }

    setSaving(true)
    setError('')
    try {
      const updatedProfile = await iotApi.updateProfile({ studentId, fullName, organization, email, links })
      setProfile(updatedProfile)
      setForm(profileToForm(updatedProfile))
      setEditing(false)
      onNotify?.('Đã cập nhật thông tin cá nhân')
      window.dispatchEvent(new CustomEvent(PROFILE_UPDATED_EVENT, {
        detail: { ...updatedProfile, avatarUrl },
      }))
    } catch (requestError) {
      if (requestError.status !== 401) setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }


  const projectLinks = PROFILE_LINK_DEFINITIONS.map((definition) => ({
    ...definition,
    href: profile?.links?.[definition.key] ?? DEFAULT_PROFILE_LINKS[definition.key],
  }))

  return (
    <div className="page profile-page">
      <PageIntro
        eyebrow="Tài khoản"
        title="Thông tin cá nhân"
      />

      <form className='profile-stack' onSubmit={handleSubmit}>
        <section className='card details-card'>
          <div className="section-head">
            <div><h2>Thông tin chi tiết</h2><p>Thông tin học tập và liên hệ</p></div>
            {!editing && (
              <button
                className='secondary-button profile-edit-button'
                type='button'
                onClick={startEditing}
                disabled={loading || !profile}
              >
                <PencilLine size={16} /> Chỉnh sửa
              </button>
            )}
          </div>
          <div className='profile-details-layout'>
            <div className='avatar-upload'>
              <label className='profile-avatar-picker'>
                <span className='profile-avatar-image'>
                  {avatarUrl ? (
                    <img src={avatarUrl} alt='Ảnh đại diện đã chọn' />
                  ) : (
                    <span>TT</span>
                  )}
                </span>
                <i><Camera size={16} /></i>
                <input
                  type='file'
                  accept='image/png,image/jpeg,image/webp'
                  onChange={handleAvatarChange}
                />
              </label>
              <strong>Ảnh đại diện</strong>
              <small>PNG, JPG hoặc WEBP, tối đa 2 MB</small>
            </div>

            {editing ? (
              <div className='profile-form'>
                <div className='detail-grid'>
                  <label className='profile-input-field'>
                    <span>Họ và tên</span>
                    <input
                      name='fullName'
                      value={form.fullName}
                      onChange={handleInputChange}
                      maxLength={120}
                      required
                      autoFocus
                    />
                  </label>
                  <label className='profile-input-field'>
                    <span>Mã sinh viên</span>
                    <input
                      name='studentId'
                      value={form.studentId}
                      onChange={handleInputChange}
                      maxLength={50}
                      required
                    />
                  </label>
                  <label className='profile-input-field'>
                    <span>Học viện</span>
                    <input
                      name='organization'
                      value={form.organization}
                      onChange={handleInputChange}
                      maxLength={160}
                      required
                    />
                  </label>
                  <label className='profile-input-field'>
                    <span>Email</span>
                    <input
                      name='email'
                      type='email'
                      value={form.email}
                      onChange={handleInputChange}
                      maxLength={160}
                      required
                    />
                  </label>
                </div>
              </div>
            ) : (
              <div className='profile-readonly'>
                <div className="detail-grid">
                  <InfoField label="Họ và tên" value={loading ? 'Đang tải...' : profile?.fullName ?? '—'} />
                  <InfoField label="Mã sinh viên" value={loading ? 'Đang tải...' : profile?.studentId ?? '—'} />
                  <InfoField label="Học viện" value={loading ? 'Đang tải...' : profile?.organization ?? '—'} />
                  <InfoField label="Email" value={loading ? 'Đang tải...' : profile?.email ?? '—'} />
                </div>
                {error && (
                  <p className='profile-form-error' role='alert'>
                    <AlertCircle size={16} /> {error}
                  </p>
                )}
              </div>
            )}
          </div>
        </section>

        <section className="card project-links">
          <div className="section-head">
            <div><h2>Tài nguyên dự án</h2><p>Liên kết thiết kế, mã nguồn và tài liệu</p></div>
            <ExternalLink size={18} />
          </div>
          {editing ? (
            <div className='profile-links-editor'>
              {PROFILE_LINK_DEFINITIONS.map((link) => {
                const Icon = link.icon
                return (
                  <label className='profile-link-input' key={link.key}>
                    <span className={`link-icon ${link.className}`}><Icon size={19} /></span>
                    <span className='profile-link-input-copy'>
                      <strong>{link.label}</strong>
                      <input
                        name={link.key}
                        value={form.links[link.key]}
                        onChange={handleLinkChange}
                        maxLength={500}
                        required
                      />
                    </span>
                  </label>
                )
              })}
            </div>
          ) : (
            <div className="link-grid">
              {projectLinks.map((link) => <ProjectLink link={link} key={link.key} />)}
            </div>
          )}
        </section>

        {editing && (
          <div className='profile-save-bar'>
            {error && (
              <p className='profile-form-error' role='alert'>
                <AlertCircle size={16} /> {error}
              </p>
            )}
            <div className='profile-form-actions'>
              <button className='secondary-button' type='button' onClick={cancelEditing} disabled={saving}>
                <X size={16} /> Hủy
              </button>
              <button className='primary-button' type='submit' disabled={saving}>
                {saving ? <LoaderCircle className='spin' size={16} /> : <Save size={16} />}
                {saving ? 'Đang lưu...' : 'Lưu tất cả thay đổi'}
              </button>
            </div>
          </div>
        )}
      </form>
    </div>
  )
}
