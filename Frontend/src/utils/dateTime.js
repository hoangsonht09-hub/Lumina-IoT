const normalizeTimeText = (value) => String(value).trim().toLocaleLowerCase('vi')

export function matchesTimeText(row, query) {
  const normalizedQuery = normalizeTimeText(query)
  if (!normalizedQuery) return true

  const isoTime = row.timestamp?.split('T')[1] ?? ''
  const searchableText = [
    row.time,
    row.date,
    row.timestamp,
    row.timestamp?.replace('T', ' '),
    `${row.date} ${row.time}`,
    `${row.date} ${isoTime}`,
  ].join(' ')

  return normalizeTimeText(searchableText).includes(normalizedQuery)
}

export function formatApiDateTime(value) {
  if (!value) return { timestamp: '', time: '--:--:--', date: '--/--/----', displayDateTime: '--/--/---- --:--:--' }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return { timestamp: value, time: value, date: '', displayDateTime: value }
  const dateTimeParts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Bangkok',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(parsed).reduce((parts, part) => {
    if (part.type !== 'literal') parts[part.type] = part.value
    return parts
  }, {})
  const displayDateTime = `${dateTimeParts.year}-${dateTimeParts.month}-${dateTimeParts.day} ${dateTimeParts.hour}:${dateTimeParts.minute}:${dateTimeParts.second}`

  return {
    timestamp: parsed.toISOString(),
    displayDateTime,
    time: new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
    }).format(parsed),
    date: new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    }).format(parsed),
  }
}
