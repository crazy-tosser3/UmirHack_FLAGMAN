export type CellStatus = 'high' | 'medium' | 'low';
type Align = 'left' | 'center' | 'right';

export interface Cell{
  status?: CellStatus;
  align?: Align;
  body: string | number;
}

export interface TableProps{
  headers: string[]
  data: Cell[][]
}