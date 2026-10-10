import type { Cell, CellStatus, TableProps } from "../model/types";

export const Table = ({ headers, data }: TableProps) => {
  return (
    <div className="w-full rounded-xl border border-primary flex-1 overflow-auto">
      <table className="w-full">
        <thead className="border-b bg-primary/40 backdrop-blur-md sticky top-0">
          <tr className="sticky top-0">
            <th className="p-2">#</th>
            {headers.map((header: string, index: number) => (
              <th className="p-2" key={index}>{header}</th>
            ))}
          </tr>
        </thead>
        <tbody className="border-collapse divide-y divide-primary">
          {data.map((row: Cell[], index: number) => (
            <tr className="divide-x divide-primary hover:bg-secondary transition-colors">
              <td className="text-center text-primary">{index+1}</td>
              {row.map((cell, index) => (
                <Cell data={cell} key={index} />
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const statusClass = (status?: CellStatus) => {
  switch (status) {
    case 'high': return 'bg-success-bg text-success p-1 px-2 rounded-full';
    case 'medium': return 'bg-warn-bg text-warn p-1 px-2 rounded-full';
    case 'low': return 'bg-err-bg text-err p-1 px-2 rounded-full';
  }
}

const Cell = ({ data }: { data: Cell }) => {
  switch (typeof data.body) {
    case 'number': return (
      <td className={"tabular-nums p-2 align-"+(data.align ?? "center")}>
        <span className={statusClass(data.status)}>{data.body}</span>
      </td>
    );
    case 'string': return (
      <td className={"p-2 align-"+(data.align ?? "center")}>
        <span className={statusClass(data.status)}>{data.body}</span>
      </td>
    );
  }
};
