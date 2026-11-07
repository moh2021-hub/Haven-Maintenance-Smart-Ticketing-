import { useCallback, useEffect, useState } from "react";
import { api } from "./api";
import { STATUS_LABELS } from "./labels";
import BoardHeader from "./components/BoardHeader";
import RequestRow from "./components/RequestRow";
import NewRequestForm from "./components/NewRequestForm";
import TechnicianPanel from "./components/TechnicianPanel";

const FILTERS = ["ALL", "NEW", "IN_PROGRESS", "COMPLETED"];
const FILTER_LABELS = { ALL: "Everything", ...STATUS_LABELS };

export default function App() {
  const [requests, setRequests] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [now, setNow] = useState(() => Date.now());

  // Keeps every "due in 3h 12m" honest without hammering the server.
  useEffect(() => {
    const tick = setInterval(() => setNow(Date.now()), 30000);
    return () => clearInterval(tick);
  }, []);

  const load = useCallback(async () => {
    try {
      const [list, crew] = await Promise.all([
        api.listRequests(filter),
        api.listTechnicians(),
      ]);
      setRequests(list);
      setTechnicians(crew);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    load();
  }, [load]);

  const openRequests = requests.filter(
    (r) => r.status === "NEW" || r.status === "IN_PROGRESS"
  );
  const lateCount = openRequests.filter((r) => r.overdue).length;

  // The list already arrives sorted by priority, so the first open one is the worst one.
  const nextUp = openRequests[0] || null;

  return (
    <div className="page">
      <BoardHeader
        nextUp={filter === "ALL" || filter === "NEW" || filter === "IN_PROGRESS" ? nextUp : null}
        openCount={openRequests.length}
        lateCount={lateCount}
        now={now}
      />

      {error && (
        <div className="alert" role="alert">
          <span>{error}</span>
          <button type="button" className="quiet small" onClick={load}>
            Try again
          </button>
        </div>
      )}

      <div className="layout">
        <main>
          <nav className="tabs">
            {FILTERS.map((key) => (
              <button
                key={key}
                type="button"
                className={filter === key ? "tab tab-on" : "tab"}
                aria-pressed={filter === key}
                onClick={() => setFilter(key)}
              >
                {FILTER_LABELS[key]}
              </button>
            ))}
          </nav>

          {loading && <p className="hint">Loading the board…</p>}

          {!loading && requests.length === 0 && (
            <div className="empty">
              <p>
                {filter === "ALL"
                  ? "No requests yet. Log the first one and it lands here, ranked."
                  : "Nothing in this view."}
              </p>
            </div>
          )}

          <div className="board">
            {requests.map((request, i) => (
              <RequestRow
                key={request.id}
                request={request}
                rank={i + 1}
                technicians={technicians}
                now={now}
                onChanged={load}
                onError={setError}
              />
            ))}
          </div>
        </main>

        <aside className="rail">
          <NewRequestForm onCreated={load} onError={setError} />
          <TechnicianPanel
            technicians={technicians}
            requests={requests}
            onChanged={load}
            onError={setError}
          />
          <section className="panel key">
            <h2>How ranking works</h2>
            <ol className="ladder">
              <li className="p1">Gas leak</li>
              <li className="p2">No heat</li>
              <li className="p3">Water leak</li>
              <li className="p4">Broken appliance</li>
              <li className="p5">Something else</li>
            </ol>
            <p className="hint">
              Same kind of problem? Whoever reported first goes first.
            </p>
          </section>
        </aside>
      </div>
    </div>
  );
}
