import { TYPE_LABELS, deadline } from "../labels";

/**
 * The top of the board answers the only question staff have when they walk up:
 * what is the worst thing happening right now?
 */
export default function BoardHeader({ nextUp, openCount, lateCount, now }) {
  const due = nextUp ? deadline(nextUp.estimatedCompletionTime, now) : null;

  return (
    <header className="board-head">
      <div className="brand">
        <h1>Maintenance board</h1>
        <p>
          {openCount === 0
            ? "Nothing open"
            : openCount + (openCount === 1 ? " request open" : " requests open")}
          {lateCount > 0 && <span className="late-count">{lateCount} past due</span>}
        </p>
      </div>

      {nextUp ? (
        <div className={"next p" + nextUp.priority}>
          <span className="next-tag">Handle first</span>
          <strong className="next-apt">Apt {nextUp.apartmentNumber}</strong>
          <span className="next-type">{TYPE_LABELS[nextUp.requestType]}</span>
          <span className={"next-due" + (due.late ? " due-late" : "")}>{due.text}</span>
        </div>
      ) : (
        <div className="next next-clear">
          <strong className="next-apt">Board is clear</strong>
          <span className="next-type">Every request has been closed out.</span>
        </div>
      )}
    </header>
  );
}
