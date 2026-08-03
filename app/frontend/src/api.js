// All calls use a relative path; nginx (in the container) or the Vite dev proxy
// forwards /api to the Spring Boot backend. Keeps the frontend origin-agnostic.
const BASE = '/api/employees'

async function parseError(res, fallback) {
  const body = await res.json().catch(() => ({}))
  return new Error(body.message || fallback)
}

export async function listEmployees() {
  const res = await fetch(BASE)
  if (!res.ok) throw await parseError(res, 'Failed to load employees')
  return res.json()
}

export async function addEmployee(employee) {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(employee),
  })
  if (!res.ok) throw await parseError(res, 'Failed to add employee')
  return res.json()
}

export async function deleteEmployee(id) {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!res.ok) throw await parseError(res, 'Failed to delete employee')
}
