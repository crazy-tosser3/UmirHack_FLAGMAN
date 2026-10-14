import { cn } from "@lib/utils";

interface ModuleProps {
  colSpan?: number;
  rowSpan?: number;
  className?: string;
  children: React.ReactNode;
}

const COL_SPAN: Record<number, string> = {
  1: "col-span-1",
  2: "md:col-span-2",
  3: "md:col-span-3",
  4: "md:col-span-full",
};

const ROW_SPAN: Record<number, string> = {
  1: "row-span-1",
  2: "row-span-2",
  3: "row-span-3",
  4: "row-span-4",
  5: "row-span-full",
};

export const DashboardModule = ({
  colSpan = 1,
  rowSpan = 1,
  className,
  children,
}: ModuleProps) => {
  return (
    <div
      className={cn(
        "bg-bg min-h-0 rounded-xl overflow-auto md:h-full",
        rowSpan >= 2 ? "h-[75dvh] min-h-96" : "h-[60dvh] min-h-80",
        "md:min-h-0",
        COL_SPAN[colSpan] ?? "col-span-1",
        ROW_SPAN[rowSpan] ?? "row-span-1",
        className)}
    >
      {children}
    </div>
  );
};