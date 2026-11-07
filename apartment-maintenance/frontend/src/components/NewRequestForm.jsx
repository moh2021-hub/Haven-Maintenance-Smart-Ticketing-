import { useState } from "react";
import { api } from "../api";
import { TYPE_LABELS, TYPE_ORDER } from "../labels";

const EMPTY = { apartmentNumber: "", requestType: "WATER_LEAK", description: "" };

export default function NewRequestForm({ onCreated, onError }) {
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    try {
      await api.createRequest(form);
      setForm(EMPTY);
      await onCreated();
    } catch (err) {
      onError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Log a request</h2>

      <label className="field">
        Apartment
        <input
          value={form.apartmentNumber}
          onChange={set("apartmentNumber")}
          placeholder="302"
          maxLength={10}
          required
        />
      </label>

      <label className="field">
        What is wrong
        <select value={form.requestType} onChange={set("requestType")}>
          {TYPE_ORDER.map((type) => (
            <option key={type} value={type}>
              {TYPE_LABELS[type]}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        Details
        <textarea
          value={form.description}
          onChange={set("description")}
          placeholder="Gas smell in the kitchen, strongest near the stove"
          rows={3}
          required
        />
      </label>

      <button type="submit" className="go block" disabled={busy}>
        {busy ? "Logging…" : "Log request"}
      </button>

      <p className="hint">
        The board sorts it for you. A gas leak jumps the queue even if it is reported last.
      </p>
    </form>
  );
}
