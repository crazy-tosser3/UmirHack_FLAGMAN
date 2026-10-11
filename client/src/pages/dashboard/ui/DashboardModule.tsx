import { cn } from "@lib/utils";

interface ModuleProps {
  className?: string;
  children: React.ReactNode;
}

export const DashboardModule = ({
  className,
  children,
}: ModuleProps) => {
  return (
    <div
      className={cn(
        "sm:flex-1 min-h-0 h-full border border-primary rounded-xl overflow-auto p-2",
        className)}
    >
      {children}
    </div>
  );
};