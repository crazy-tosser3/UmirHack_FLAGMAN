import api from "@lib/api";
import type { Unit } from "@lib/types";
import { useQuery } from "@tanstack/react-query";

export interface CatalogIngredient {
  id: string;
  name: string;
  unit: Unit;
}

interface IngredientsPage {
  items: CatalogIngredient[];
  page: number;
  limit: number;
  total: number;
}

const PAGE_LIMIT = 100;

const fetchAll = async () => {
  const items: CatalogIngredient[] = [];
  for (let page = 1; ; page++) {
    const { data } = await api.get<IngredientsPage>("/ingredients", {
      params: { page, limit: PAGE_LIMIT },
    });
    items.push(...data.items);
    if (items.length >= data.total || data.items.length < PAGE_LIMIT) break;
  }
  return items.sort((a, b) => a.name.localeCompare(b.name, "ru"));
};

export function useIngredientsCatalog(enabled = true) {
  return useQuery({
    queryKey: ["ingredients", "catalog-all"],
    queryFn: fetchAll,
    enabled,
    staleTime: 5 * 60 * 1000,
  });
}
