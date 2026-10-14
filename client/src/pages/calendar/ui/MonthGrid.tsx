import { cn } from "@lib/utils";
import dayjs, { type Dayjs } from "dayjs";
import type { CalendarEvent } from "../model/types";
import { EVENT_STYLE } from "./eventStyles";

const WEEKDAYS = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"];
const MAX_CHIPS = 3;

interface MonthGridProps {
  month: Dayjs;
  days: Dayjs[];
  eventsByDay: Map<string, CalendarEvent[]>;
  selected: string;
  onSelect: (day: string) => void;
}

export const MonthGrid = ({ month, days, eventsByDay, selected, onSelect }: MonthGridProps) => {
  const today = dayjs().format("YYYY-MM-DD");

  return (
    <div className="flex flex-col gap-1 min-w-0">
      <div className="grid grid-cols-7 gap-1 text-xs text-primary text-center">
        {WEEKDAYS.map((d) => (
          <span key={d}>{d}</span>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1">
        {days.map((day) => {
          const key = day.format("YYYY-MM-DD");
          const events = eventsByDay.get(key) ?? [];
          const inMonth = day.month() === month.month();

          return (
            <button
              key={key}
              type="button"
              onClick={() => onSelect(key)}
              className={cn(
                "flex flex-col gap-1 min-w-0 min-h-14 md:min-h-24 p-1 rounded-lg border text-left transition-colors hover:bg-secondary",
                inMonth ? "border-primary/40" : "border-transparent opacity-40",
                key === selected && "border-accent bg-accent/20",
              )}
            >
              <span
                className={cn(
                  "text-xs tabular-nums size-6 grid place-items-center rounded-full shrink-0",
                  key === today && "bg-text text-bg",
                )}
              >
                {day.date()}
              </span>

              <div className="flex md:hidden flex-wrap gap-0.5">
                {events.slice(0, 6).map((e) => (
                  <span key={e.id} className={cn("size-1.5 rounded-full", EVENT_STYLE[e.type].dot)} />
                ))}
              </div>

              <div className="hidden md:flex flex-col gap-0.5 min-w-0">
                {events.slice(0, MAX_CHIPS).map((e) => (
                  <span
                    key={e.id}
                    className={cn("truncate text-[11px] leading-4 px-1 rounded", EVENT_STYLE[e.type].chip)}
                  >
                    {e.title}
                  </span>
                ))}
                {events.length > MAX_CHIPS && (
                  <span className="text-[11px] text-primary px-1">ещё {events.length - MAX_CHIPS}</span>
                )}
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
};
