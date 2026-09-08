export const DEFAULT_API_BASE = 'http://127.0.0.1:8899/warehouse'

export function normalizeApiBase(input: string): string {
  let value = (input || '').trim()
  if (!value) return DEFAULT_API_BASE
  value = value.replace(/\/+$/, '')
  if (!/\/warehouse$/i.test(value)) {
    value += '/warehouse'
  }
  return value
}
