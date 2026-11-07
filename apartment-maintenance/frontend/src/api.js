// Every call to the Spring Boot backend lives here.

const API = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

// Locally saved photos come back as "/photos/abc.jpg", which is relative to
// the backend, not to React. S3 URLs are already absolute.
const ORIGIN = API.replace(/\/api\/?$/, "");

export function photoSrc(photoUrl) {
  if (!photoUrl) return null;
  return photoUrl.startsWith("http") ? photoUrl : ORIGIN + photoUrl;
}

async function send(path, options) {
  const res = await fetch(API + path, options);

  if (!res.ok) {
    let message = "Something went wrong (" + res.status + ")";
    try {
      const body = await res.json();
      if (body && body.message) message = body.message;
    } catch (ignored) {
      // backend returned no JSON body
    }
    throw new Error(message);
  }

  if (res.status === 204) return null;
  return res.json();
}

const json = (body) => ({
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export const api = {
  listRequests: (status) =>
    send("/requests" + (status && status !== "ALL" ? "?status=" + status : "")),

  summary: () => send("/requests/summary"),

  createRequest: (body) => send("/requests", json(body)),

  assign: (id, technicianId) =>
    send("/requests/" + id + "/assign?technicianId=" + technicianId, { method: "PUT" }),

  autoAssign: (id) => send("/requests/" + id + "/auto-assign", { method: "PUT" }),

  setStatus: (id, status) =>
    send("/requests/" + id + "/status?status=" + status, { method: "PUT" }),

  uploadPhoto: (id, file) => {
    const form = new FormData();
    form.append("file", file);
    return send("/requests/" + id + "/photo", { method: "POST", body: form });
  },

  deleteRequest: (id) => send("/requests/" + id, { method: "DELETE" }),

  listTechnicians: () => send("/technicians"),

  createTechnician: (body) => send("/technicians", json(body)),

  deleteTechnician: (id) => send("/technicians/" + id, { method: "DELETE" }),
};
