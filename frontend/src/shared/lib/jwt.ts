export function getJwtSubject(token: string | null): number | null {
  if (!token) return null

  const parts = token.split('.')
  if (parts.length < 2) return null

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const payload = JSON.parse(atob(padded)) as { sub?: string }
    const value = payload.sub ? Number(payload.sub) : NaN
    return Number.isFinite(value) ? value : null
  } catch {
    return null
  }
}

