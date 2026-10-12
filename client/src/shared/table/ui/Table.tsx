import type { Cell, CellStatus, TableProps } from "../model/types";
import { FaSort, FaSortDown, FaSortUp } from "react-icons/fa6";
import { cn } from "@lib/utils";

export const Table = ({ columns, data, sort, onSort }: TableProps) => {
  const handleSort = (col: typeof columns[number]) => {
    if (col.sortable && col.sortKey && onSort) {
      onSort(col.sortKey);
    }
  };

  return (
    <div className="h-full w-full overflow-auto rounded-xl border border-primary">
      <table className="w-full border-collapse">
        <thead className="sticky top-0 z-10 bg-primary/40 backdrop-blur-md rounded-t-xl">
          <tr>
            <th className="p-2 text-center text-sm w-10">#</th>
            {columns.map((col, index) => {
              const isActive = sort?.key === col.sortKey;
              const canSort = col.sortable && onSort;

              return (
                <th
                  key={index}
                  onClick={() => handleSort(col)}
                  className={cn(
                    "p-2 text-left text-sm",
                    canSort && "cursor-pointer select-none hover:bg-primary/30 transition-colors",
                    col.align === "center" && "text-center",
                    col.align === "right" && "text-right"
                  )}
                >
                  <span className="inline-flex items-center gap-1">
                    {col.label}
                    {canSort && (
                      <span className="text-primary">
                        {isActive && sort?.direction === "asc" && <FaSortUp size={12} />}
                        {isActive && sort?.direction === "desc" && <FaSortDown size={12} />}
                        {!isActive && <FaSort size={10} />}
                      </span>
                    )}
                  </span>
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody className="divide-y divide-primary/40">
          {data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + 1}
                className="p-8 text-center text-sm text-primary"
              >
                Ничего не найдено
              </td>
            </tr>
          ) : (
            data.map((row, index) => (
              <tr
                key={index}
                className="divide-x divide-primary/20 hover:bg-secondary transition-colors"
              >
                <td className="text-center text-primary/60 p-2 text-sm">{index + 1}</td>
                {row.map((cell, i) => (
                  <Cell data={cell} key={i} />
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};

const statusClass = (status?: CellStatus) => {
  switch (status) {
    case 'high': return 'bg-success-bg text-success p-1 px-2 rounded-full text-xs';
    case 'medium': return 'bg-warn-bg text-warn p-1 px-2 rounded-full text-xs';
    case 'low': return 'bg-err-bg text-err p-1 px-2 rounded-full text-xs';
    default: return '';
  }
};

const Cell = ({ data }: { data: Cell }) => {
  const align = data.align ?? "left";
  const hasStatus = data.status !== undefined;

  return (
    <td className={cn("p-2 text-sm", `text-${align}`)}>
      {hasStatus ? (
        <span className={statusClass(data.status)}>{data.body}</span>
      ) : (
        <span className={cn(
          "block",
          align === "left" && "max-w-75 truncate",
          typeof data.body === "number" && "tabular-nums"
        )}>
          {data.body}
        </span>
      )}
    </td>
  );
};