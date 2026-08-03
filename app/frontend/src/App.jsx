import { useEffect, useState } from 'react'
import { listEmployees, addEmployee, deleteEmployee } from './api'

const EMPTY_FORM = { id: '', name: '', department: '', salary: '' }

export default function App() {
  const [employees, setEmployees] = useState([])
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  async function refresh() {
    setLoading(true)
    try {
      setEmployees(await listEmployees())
      setError('')
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refresh()
  }, [])

  function update(field) {
    return (e) => setForm({ ...form, [field]: e.target.value })
  }

  async function handleAdd(e) {
    e.preventDefault()
    try {
      await addEmployee({
        id: Number(form.id),
        name: form.name,
        department: form.department,
        salary: Number(form.salary),
      })
      setForm(EMPTY_FORM)
      setError('')
      await refresh()
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleDelete(id) {
    try {
      await deleteEmployee(id)
      await refresh()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="container">
      <header>
        <h1>Employee Management</h1>
        <p className="subtitle">Spring Boot &middot; MySQL &middot; Redis &middot; Kubernetes</p>
      </header>

      {error && <div className="banner error">{error}</div>}

      <form className="card" onSubmit={handleAdd}>
        <h2>Add employee</h2>
        <div className="grid">
          <input type="number" placeholder="ID" value={form.id} onChange={update('id')} required />
          <input placeholder="Name" value={form.name} onChange={update('name')} required />
          <input placeholder="Department" value={form.department} onChange={update('department')} required />
          <input type="number" step="0.01" placeholder="Salary" value={form.salary} onChange={update('salary')} required />
        </div>
        <button type="submit">Add</button>
      </form>

      <div className="card">
        <h2>Employees {!loading && <span className="count">({employees.length})</span>}</h2>
        {loading ? (
          <p className="muted">Loading…</p>
        ) : employees.length === 0 ? (
          <p className="muted">No employees yet. Add one above.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Department</th>
                <th>Salary</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {employees.map((emp) => (
                <tr key={emp.id}>
                  <td>{emp.id}</td>
                  <td>{emp.name}</td>
                  <td>{emp.department}</td>
                  <td>{emp.salary.toLocaleString()}</td>
                  <td>
                    <button className="danger" onClick={() => handleDelete(emp.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
