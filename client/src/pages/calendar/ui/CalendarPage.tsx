import { cn, isStaff } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { PageTitle } from "@shared/page-title";
import dayjs from "dayjs";
import "dayjs/locale/ru";
import { useMemo, useState } from "react";
import { FaCalendarAlt, FaChevronLeft, FaChevronRight, FaCircleNotch } from "react-icons/fa";
import type { CalendarEvent, CalendarEventType } from "../model/types";
import { useCalendarEvents } from "../model/use-calendar-events";
import { DayDetails } from "./DayDetails";
import { EVENT_TYPES } from "./eventStyles";
import { MonthGrid } from "./MonthGrid";

dayjs.locale("ru");

const buildDays = (month: dayjs.Dayjs) => {
  const first = month.startOf("month");
  const start = first.subtract((first.day() + 6) % 7, "day");
  return Array.from({ length: 42 }, (_, i) => start.add(i, "day"));
};

const CalendarContent = () => {
  const [month, setMonth] = useState(() => dayjs().startOf("month"));
  const [selected, setSelected] = useState(() => dayjs().format("YYYY-MM-DD"));
  const [hidden, setHidden] = useState<Set<CalendarEventType>>(new Set());

  const days = useMemo(() => buildDays(month), [month]);
  const { events, isPending, isError } = useCalendarEvents(days[0], days[days.length - 1]);
  const showLoader = useDelayedValue(isPending, { minDisplay: 300 });

  const eventsByDay = useMemo(() => {
    const map = new Map<string, CalendarEvent[]>();
    for (const e of events) {
      if (hidden.has(e.type)) continue;
      const list = map.get(e.day);
      if (list) list.push(e);
      else map.set(e.day, [e]);
    }
    return map;
  }, [events, hidden]);

  const toggleType = (type: CalendarEventType) =>
    setHidden((prev) => {
      const next = new Set(prev);
      if (next.has(type)) next.delete(type);
      else next.add(type);
      return next;
    });

  const goToMonth = (next: dayjs.Dayjs) => {
    setMonth(next.startOf("month"));
    setSelected(next.isSame(dayjs(), "month") ? dayjs().format("YYYY-MM-DD") : next.startOf("month").format("YYYY-MM-DD"));
  };

  const monthTitle = month.format("MMMM YYYY");

  return (
    <>
      <div className="flex flex-wrap items-center gap-2 p-2 shrink-0">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => goToMonth(month.subtract(1, "month"))}
            className="grid size-8 place-items-center rounded-lg border border-primary/20 transition-colors hover:bg-secondary"
          >
            <FaChevronLeft size={12} />
          </button>
          <span className="font-manrope text-lg w-40 text-center capitalize">{monthTitle}</span>
          <button
            type="button"
            onClick={() => goToMonth(month.add(1, "month"))}
            className="grid size-8 place-items-center rounded-lg border border-primary/20 transition-colors hover:bg-secondary"
          >
            <FaChevronRight size={12} />
          </button>
          <button
            type="button"
            onClick={() => goToMonth(dayjs())}
            className="rounded-lg bg-secondary px-3 py-1 text-sm transition-colors hover:bg-primary/40"
          >
            Сегодня
          </button>
        </div>

        <div className="flex flex-wrap gap-1 md:ml-auto">
          {EVENT_TYPES.map(({ type, label, dot }) => (
            <button
              key={type}
              type="button"
              onClick={() => toggleType(type)}
              className={cn(
                "flex items-center gap-1.5 rounded-full border border-primary/40 px-2 py-0.5 text-xs transition-opacity",
                hidden.has(type) && "opacity-40 line-through",
              )}
            >
              <span className={cn("size-2 rounded-full", dot)} />
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="relative flex flex-col lg:flex-row gap-4 p-2 flex-1 min-h-0 overflow-auto">
        {isError && (
          <p className="absolute inset-x-2 top-2 z-10 text-err bg-err-bg p-2 rounded-xl text-center text-sm">
            Часть данных не загрузилась, календарь может быть неполным
          </p>
        )}

        <section className="lg:flex-3 min-w-0 relative">
          <MonthGrid
            month={month}
            days={days}
            eventsByDay={eventsByDay}
            selected={selected}
            onSelect={setSelected}
          />
          {showLoader && (
            <div className="absolute inset-0 grid place-items-center bg-bg/60">
              <FaCircleNotch size={20} className="animate-spin text-primary" />
            </div>
          )}
        </section>

        <aside className="lg:flex-1 lg:min-w-72 min-h-0 flex flex-col lg:border-l lg:border-primary lg:pl-4">
          <DayDetails day={selected} events={eventsByDay.get(selected) ?? []} />
        </aside>
      </div>
    </>
  );
};

export const CalendarPage = () => {
  const { user } = useAuth();
  const allowed = user ? isStaff(user.role) : false;

  return (
    <div className="bg-bg flex flex-col flex-1 min-h-0 border border-primary shadow-md rounded-xl overflow-hidden">
      <PageTitle title="Календарь" />
      <h1 className="bg-accent p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Календарь сроков и поставок</span>
        <FaCalendarAlt />
      </h1>

      {allowed ? (
        <CalendarContent />
      ) : (
        <p className="m-4 text-warn bg-warn-bg p-2 rounded-xl text-center">
          Календарь доступен менеджерам и администраторам
        </p>
      )}
    </div>
  );
};
