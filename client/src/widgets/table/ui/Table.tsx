import type { Cell, CellStatus, TableProps } from "../model/types";

export const Table = ({ headers, data }: TableProps) => {
  return (
    <div className="w-full flex-1 min-h-0 overflow-auto rounded-xl border border-primary">
      <table className="w-full">
        <thead className="sticky top-0 z-10 bg-primary/40 backdrop-blur-md">
          <tr>
            <th className="p-2">#</th>
            {headers.map((header, index) => (
              <th className="p-2" key={index}>{header}</th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-primary">
          {data.map((row, index) => (
            <tr
              key={index}
              className="divide-x divide-primary hover:bg-secondary transition-colors"
            >
              <td className="text-center text-primary p-2">{index + 1}</td>
              {row.map((cell, i) => (
                <Cell data={cell} key={i} />
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

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
