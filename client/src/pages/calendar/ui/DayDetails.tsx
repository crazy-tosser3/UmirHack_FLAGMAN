import { cn } from "@lib/utils";
import dayjs from "dayjs";
import type { CalendarEvent } from "../model/types";
import { EVENT_STYLE, EVENT_TYPES } from "./eventStyles";

export const DayDetails = ({ day, events }: { day: string; events: CalendarEvent[] }) => (
  <div className="flex flex-col gap-2 min-h-0">
    <h2 className="font-manrope text-lg border-b border-primary shrink-0">
      {dayjs(day).format("D MMMM, dddd")}
    </h2>

    {events.length === 0 ? (
      <p className="text-sm text-primary py-6 text-center">Событий нет</p>
    ) : (
      <div className="flex flex-col gap-3 overflow-auto">
        {EVENT_TYPES.map(({ type, label }) => {
          const group = events.filter((e) => e.type === type);
          if (!group.length) return null;
          return (
            <section key={type} className="flex flex-col gap-1">
              <h3 className="flex items-center gap-2 text-sm text-primary">
                <span className={cn("size-2 rounded-full", EVENT_STYLE[type].dot)} />
                {label}
                <span className="tabular-nums">· {group.length}</span>
              </h3>
              <ul className="flex flex-col gap-1">
                {group.map((e) => (
                  <li key={e.id} className="flex flex-col p-2 rounded-lg border border-primary/40 text-sm">
                    <span className="truncate font-manrope">{e.title}</span>
                    <span className="text-xs text-primary truncate">{e.details}</span>
                  </li>
                ))}
              </ul>
            </section>
          );
        })}
      </div>
    )}
  </div>
);
