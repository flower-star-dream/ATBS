/** 字符串 → Base64（UTF-8） */
export function strToBase64(str: string): string {
  const utf8Bytes = new TextEncoder().encode(str)          // Uint8Array
  const binStr = Array.from(utf8Bytes, b => String.fromCharCode(b)).join('')
  return btoa(binStr)
}

/** Base64 → 字符串（UTF-8） */
export function base64ToStr(b64: string): string {
  const binStr = atob(b64)
  const bytes = Uint8Array.from(binStr, ch => ch.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}
