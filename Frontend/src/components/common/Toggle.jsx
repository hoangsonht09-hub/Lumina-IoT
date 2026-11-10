export default function Toggle({ on, onChange, label, disabled = false, busy = false }) {
  return (
    <button
      className={`toggle ${on ? 'on' : ''}`}
      onClick={onChange}
      aria-label={label}
      aria-pressed={on}
      aria-busy={busy}
      disabled={disabled}
    >
      <span />
    </button>
  )
}
