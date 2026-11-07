import { useRef, useState } from "react";
import { api, photoSrc } from "../api";
import { TYPE_LABELS, STATUS_LABELS, deadline, clock } from "../labels";

export default function RequestRow({ request, rank, technicians, now, onChanged, onError }) {
  const [busy, setBusy] = useState(false);
  const fileInput = useRef(null);

  const open = request.status === "NEW" || request.status === "IN_PROGRESS";
  const due = deadline(request.estimatedCompletionTime, now);
  const photo = photoSrc(request.photoUrl);

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

  function pickPhoto(event) {
    const file = event.target.files[0];
    if (file) run(() => api.uploadPhoto(request.id, file));
    event.target.value = "";
  }

  return (
    <article
      className={"row p" + request.priority + (open ? "" : " row-closed")}
      aria-busy={busy}
    >
      <div className="spine">
        <span className="rank">{rank}</span>
      </div>

      <div className="row-body">
        <header className="row-top">
          <h3 className="apt">
            Apt {request.apartmentNumber}
          </h3>
          <span className="type">{TYPE_LABELS[request.requestType]}</span>
          <span className={"due" + (open && due.late ? " due-late" : "")}>
            {open ? due.text : STATUS_LABELS[request.status]}
          </span>
        </header>

        <p className="what">{request.description}</p>

        <div className="meta">
          <span>Reported {clock(request.createdAt)}</span>
          {request.technician ? (
            <span>
              {request.technician.name} &nbsp;{request.technician.phone}
            </span>
          ) : (
            <span className="meta-flag">Nobody assigned yet</span>
          )}
        </div>

        {photo && (
          <a className="shot" href={photo} target="_blank" rel="noreferrer">
            <img src={photo} alt={"Photo from apartment " + request.apartmentNumber} />
          </a>
        )}

        <div className="actions">
          <label className="field-inline">
            <span className="sr-only">Send a technician to {request.apartmentNumber}</span>
            <select
              value={request.technician ? request.technician.id : ""}
              disabled={busy || technicians.length === 0}
              onChange={(e) => run(() => api.assign(request.id, e.target.value))}
            >
              <option value="" disabled>
                {technicians.length ? "Choose a technician" : "No technicians yet"}
              </option>
              {technicians.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>

          {!request.technician && technicians.length > 0 && (
            <button type="button" disabled={busy} onClick={() => run(() => api.autoAssign(request.id))}>
              Assign whoever is free
            </button>
          )}

          {request.status !== "COMPLETED" && (
            <button
              type="button"
              className="go"
              disabled={busy}
              onClick={() => run(() => api.setStatus(request.id, "COMPLETED"))}
            >
              Mark done
            </button>
          )}

          {request.status === "COMPLETED" && (
            <button type="button" disabled={busy} onClick={() => run(() => api.setStatus(request.id, "IN_PROGRESS"))}>
              Reopen
            </button>
          )}

          <button type="button" disabled={busy} onClick={() => fileInput.current.click()}>
            {photo ? "Replace photo" : "Add photo"}
          </button>
          <input
            ref={fileInput}
            type="file"
            accept="image/*"
            hidden
            onChange={pickPhoto}
          />

          <button
            type="button"
            className="quiet"
            disabled={busy}
            onClick={() => run(() => api.deleteRequest(request.id))}
          >
            Remove
          </button>
        </div>
      </div>
    </article>
  );
}
