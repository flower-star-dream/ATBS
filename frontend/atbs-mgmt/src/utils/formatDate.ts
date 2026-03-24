/**
 * 格式化日期时间
 * @param {string|number|Date} date - 日期对象或时间戳
 * @returns {string} 格式化后的日期字符串，格式：YYYY-MM-DD HH:mm:ss
 */
const formatDate = (date: string | number | Date): string => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化日期时间（带T分隔符）
 * @param {string|number|Date} date - 日期对象或时间戳
 * @returns {string} 格式化后的日期字符串，格式：YYYY-MM-DDTHH:mm:ss
 */
const formatReverseDate = (date: string | number | Date): string => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

export { formatDate, formatReverseDate }