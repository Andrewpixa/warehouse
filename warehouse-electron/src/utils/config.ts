export const DEFAULT_API_BASE = 'https://yaoheng.cloud/warehouse'

export function normalizeApiBase(input: string): string {
  let value = (input || '').trim()
  if (!value) return DEFAULT_API_BASE
  value = value.replace(/\/+$/, '')
  if (!/\/warehouse$/i.test(value)) {
    value += '/warehouse'
  }
  return value
}
