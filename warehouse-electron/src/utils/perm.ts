export const PERM_KEY = 'employee_permissions'

export function readPermissions(): string[] {
  try {
    const raw = localStorage.getItem(PERM_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export function savePermissions(list: string[]) {
  localStorage.setItem(PERM_KEY, JSON.stringify(list || []))
}

export function hasPerm(permissions: string[], code: string): boolean {
  if (permissions.includes('*:*')) return true
  return permissions.includes(code)
}
