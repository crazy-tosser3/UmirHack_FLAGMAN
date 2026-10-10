import type { Cell } from "@widgets/table/model/types";
import { Table } from "@widgets/table/ui/Table";

const mockTableHead = [
  "Дата создания", "ID", "№ поставки", "Продукты"
]
const mockTableBody: Cell[][] = [
  [
    {
      body: new Date().toLocaleDateString()
    },
    {
      body: Date.now()
    },
    {
      body: Date.now(),
      status: "high"
    },
    {
      body: "Картошка"
    },
  ],
  [
    {
      body: new Date().toLocaleDateString()
    },
    {
      body: Date.now()
    },
    {
      body: Date.now(),
      status: "medium"
    },
    {
      body: "Картошка"
    },
  ],
  [
    {
      body: new Date().toLocaleDateString()
    },
    {
      body: Date.now()
    },
    {
      body: Date.now(),
      status: "low"
    },
    {
      body: "Картошка"
    },
  ],
]

// Навигация (синхронизировано с созданием страниц)
interface Nav {
  path: string;
  element: React.ReactNode;
}

export const navigation: Nav[] = [
 {
  path: "/dashboard",
  element: <Table headers={mockTableHead} data={mockTableBody} />
 } 
]