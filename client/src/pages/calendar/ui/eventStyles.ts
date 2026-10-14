import type { CalendarEventType } from "../model/types";

export const EVENT_TYPES: { type: CalendarEventType; label: string; chip: string; dot: string }[] = [
  { type: "expired", label: "Просрочено", chip: "bg-err-bg text-err", dot: "bg-err" },
  { type: "expiring", label: "Истекает срок", chip: "bg-warn-bg text-warn", dot: "bg-warn" },
  { type: "expiry", label: "Срок годности", chip: "bg-secondary text-text", dot: "bg-primary" },
  { type: "supply", label: "Нужна поставка", chip: "bg-accent text-text", dot: "bg-accent" },
  { type: "delivery", label: "Принята поставка", chip: "bg-success-bg text-success", dot: "bg-success" },
];

export const EVENT_STYLE = Object.fromEntries(EVENT_TYPES.map((t) => [t.type, t])) as Record<
  CalendarEventType,
  (typeof EVENT_TYPES)[number]
>;
