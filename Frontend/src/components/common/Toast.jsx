import { useEffect } from 'react'
import { Lightbulb, X } from 'lucide-react'

export default function Toast({ message, onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 2600)
    return () => clearTimeout(timer)
  }, [message, onClose])

  return (
    <div className="toast">
      <span><Lightbulb size={17} /></span>
      <p>{message}</p>
      <button onClick={onClose} aria-label="Đóng thông báo"><X size={16} /></button>
    </div>
  )
}
