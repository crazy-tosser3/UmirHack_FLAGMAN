import { useCallback, useSyncExternalStore } from "react";

export function useMediaQuery(query: string): boolean {
  const getSnapshot = useCallback(
    () => window.matchMedia(query).matches,
    [query],
  );

  const subscribe = useCallback(
    (onStoreChange: () => void) => {
      const mql = window.matchMedia(query);
      mql.addEventListener("change", onStoreChange);
      return () => mql.removeEventListener("change", onStoreChange);
    },
    [query],
  );

  return useSyncExternalStore(subscribe, getSnapshot);
}