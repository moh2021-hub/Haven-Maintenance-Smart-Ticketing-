// Turns backend enum names into words a person would actually say.

export const TYPE_LABELS = {
  GAS_LEAK: "Gas leak",
  NO_HEAT: "No heat",
  WATER_LEAK: "Water leak",
  BROKEN_APPLIANCE: "Broken appliance",
  OTHER: "Something else",
};

export const STATUS_LABELS = {
  NEW: "Waiting",
  IN_PROGRESS: "In progress",
  COMPLETED: "Done",
  CANCELLED: "Cancelled",
};

export const TYPE_ORDER = [
  "GAS_LEAK",
  "NO_HEAT",
  "WATER_LEAK",
  "BROKEN_APPLIANCE",
  "OTHER",
];

/** "due in 2h 15m" / "3h 40m late" — the number staff actually act on. */
export function deadline(iso, now) {
  if (!iso) return { text: "no deadline", late: false };

  const ms = new Date(iso).getTime() - now;
  const late = ms < 0;
  const mins = Math.round(Math.abs(ms) / 60000);
  const days = Math.floor(mins / 1440);
  const hours = Math.floor((mins % 1440) / 60);
  const rest = mins % 60;

  let span;
  if (days > 0) span = days + "d " + hours + "h";
  else if (hours > 0) span = hours + "h " + rest + "m";
  else span = rest + "m";

  return { text: late ? span + " late" : "due in " + span, late };
}

export function clock(iso) {
  if (!iso) return "";
  return new Date(iso).toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}
