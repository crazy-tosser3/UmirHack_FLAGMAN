import { units } from "@lib/utils";
import type { Unit } from "@lib/types";

interface TooltipEntry {
  name?: string | number;
  value?: number | string | readonly (number | string)[];
  color?: string;
  payload?: { unit?: Unit };
}

interface ChartTooltipProps {
  active?: boolean;
  payload?: readonly TooltipEntry[];
  label?: string | number;
}

export const ChartTooltip = ({ active, payload, label }: ChartTooltipProps) => {
  if (!active || !payload?.length) return null;

  return (
    <div className="rounded-xl border border-primary bg-bg p-3 text-sm shadow-lg">
      <p className="mb-1">{label}</p>
      {payload.map((entry) => {
        const unit = entry.payload?.unit;
        return (
          <p key={String(entry.name)} style={{ color: entry.color }}>
            {entry.name}: <b className="tabular-nums">{String(entry.value)}</b>{" "}
            {unit ? units[unit] : ""}
          </p>
        );
      })}
    </div>
  );
};
