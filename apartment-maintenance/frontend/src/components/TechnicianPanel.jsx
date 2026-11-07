import { useState } from "react";
import { api } from "../api";

export default function TechnicianPanel({ technicians, requests, onChanged, onError }) {
  const [form, setForm] = useState({ name: "", phone: "" });
  const [adding, setAdding] = useState(false);
  const [busy, setBusy] = useState(false);

  const openJobs = (id) =>
    requests.filter((r) => r.technician && r.technician.id === id && r.status === "IN_PROGRESS").length;

  async function run(action) {
    setBusy(true);
    try {
      await action();
      await onChanged();
    } catch (err) {
      onError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function submit(e) {
    e.preventDefault();
    await run(() => api.createTechnician(form));
    setForm({ name: "", phone: "" });
    setAdding(false);
  }

  return (
    <section className="panel">
      <h2>Technicians</h2>

      {technicians.length === 0 && !adding && (
        <p className="hint">Add a technician and you can start assigning work.</p>
      )}

      <ul className="crew">
        {technicians.map((t) => {
          const load = openJobs(t.id);
          return (
            <li key={t.id}>
              <div>
                <span className="crew-name">{t.name}</span>
                <span className="crew-phone">{t.phone}</span>
              </div>
              <div className="crew-right">
                <span className="load">{load === 0 ? "free" : load + " open"}</span>
                <button
                  type="button"
                  className="quiet small"
                  disabled={busy}
                  onClick={() => run(() => api.deleteTechnician(t.id))}
                >
                  Remove
                </button>
              </div>
            </li>
          );
        })}
      </ul>

      {adding ? (
        <form onSubmit={submit}>
          <label className="field">
            Name
            <input
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="Maria Lopez"
              required
              autoFocus
            />
          </label>
          <label className="field">
            Phone
            <input
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              placeholder="313-555-0102"
              required
            />
          </label>
          <div className="pair">
            <button type="submit" className="go" disabled={busy}>
              Add technician
            </button>
            <button type="button" className="quiet" onClick={() => setAdding(false)}>
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <button type="button" className="block" onClick={() => setAdding(true)}>
          Add technician
        </button>
      )}
    </section>
  );
}
