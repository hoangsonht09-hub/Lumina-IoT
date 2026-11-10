import { useEffect, useRef, useState } from 'react'
import { Check, Copy } from 'lucide-react'

export default function CopyableDateTime({ value }) {
  const [copied, setCopied] = useState(false)
  const resetTimer = useRef(null)

  useEffect(() => () => window.clearTimeout(resetTimer.current), [])

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(true)
      window.clearTimeout(resetTimer.current)
      resetTimer.current = window.setTimeout(() => setCopied(false), 1600)
    } catch {
      setCopied(false)
    }
  }

  const Icon = copied ? Check : Copy

  return (
    <span className="copyable-datetime">
      <time>{value}</time>
      <button
        type="button"
        className={`copy-time-button${copied ? ' copied' : ''}`}
        onClick={handleCopy}
        aria-label={copied ? 'Đã sao chép thời gian' : `Sao chép thời gian ${value}`}
        title={copied ? 'Đã sao chép' : 'Sao chép thời gian'}
      >
        <Icon size={14} aria-hidden="true" />
      </button>
      <span className="visually-hidden" aria-live="polite">
        {copied ? 'Đã sao chép thời gian' : ''}
      </span>
    </span>
  )
}
