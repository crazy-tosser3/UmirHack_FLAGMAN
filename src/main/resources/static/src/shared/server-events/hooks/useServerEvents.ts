import { isStaff, units } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import { useToastStore } from "@shared/toasts";
import { useQueryClient, type QueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { openSseStream, SseUnauthorizedError, type RawSseEvent } from "../lib/sse-stream";
import type { BatchExpiryEvent, ServerEvent, ServerEventName } from "../model/types";

export type ServerEventsStatus = "idle" | "connecting" | "open" | "reconnecting";

const INVALIDATIONS: Record<ServerEventName, string[]> = {
  "stock.updated": ["stock", "expiring-batches", "analytics"],
  "stop-list.updated": ["stop-list", "dishes"],
  "offers.updated": ["hot-offers"],
  "delivery.created": ["stock", "expiring-batches", "analytics"],
  "order.confirmed": ["orders", "analytics"],
  "batch.expiring": ["expiring-batches"],
  "batch.expired": ["expiring-batches", "stock", "analytics"],
};

const MIN_RETRY_MS = 1_000;
const MAX_RETRY_MS = 30_000;
const TOAST_GROUP_MS = 1_500;

const isKnownEvent = (name: string): name is ServerEventName => name in INVALIDATIONS;

const parseEvent = (raw: RawSseEvent): ServerEvent | null => {
  if (!isKnownEvent(raw.event)) return null;
  try {
    return { name: raw.event, data: JSON.parse(raw.data) } as ServerEvent;
  } catch {
    return null;
  }
};

const createInvalidator = (queryClient: QueryClient) => {
  const pending = new Set<string>();
  let timer: ReturnType<typeof setTimeout> | null = null;

  return {
    add(keys: string[]) {
      keys.forEach((key) => pending.add(key));
      timer ??= setTimeout(() => {
        pending.forEach((key) => queryClient.invalidateQueries({ queryKey: [key] }));
        pending.clear();
        timer = null;
      }, 200);
    },
    dispose() {
      if (timer) clearTimeout(timer);
    },
  };
};

const createBatchNotifier = () => {
  const groups: Record<"batch.expiring" | "batch.expired", BatchExpiryEvent[]> = {
    "batch.expiring": [],
    "batch.expired": [],
  };
  const timers: Partial<Record<keyof typeof groups, ReturnType<typeof setTimeout>>> = {};

  const flush = (name: keyof typeof groups) => {
    const batches = groups[name];
    groups[name] = [];
    delete timers[name];
    if (!batches.length) return;

    const { newToast } = useToastStore.getState();
    const expired = name === "batch.expired";

    if (batches.length === 1) {
      const b = batches[0];
      const amount = `${b.quantity} ${units[b.unit] ?? b.unit}`;
      newToast(
        expired
          ? `Просрочено: ${b.ingredientName}, ${amount}${b.writtenOff ? " (списано)" : ""}`
          : `Скоро истекает срок: ${b.ingredientName}, ${amount}`,
        expired ? "error" : "info",
      );
      return;
    }

    newToast(
      expired
        ? `Просрочено партий: ${batches.length}`
        : `Скоро истекает срок у партий: ${batches.length}`,
      expired ? "error" : "info",
    );
  };

  return {
    add(name: keyof typeof groups, batch: BatchExpiryEvent) {
      groups[name].push(batch);
      timers[name] ??= setTimeout(() => flush(name), TOAST_GROUP_MS);
    },
    dispose() {
      Object.values(timers).forEach((t) => t && clearTimeout(t));
    },
  };
};

export function useServerEvents(): ServerEventsStatus {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<ServerEventsStatus>("idle");

  const userId = user?.id;
  const staff = user ? isStaff(user.role) : false;

  useEffect(() => {
    if (!userId) return;

    const controller = new AbortController();
    const invalidator = createInvalidator(queryClient);
    const batchNotifier = createBatchNotifier();
    let retryMs = MIN_RETRY_MS;
    let hasConnected = false;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;

    const handle = (event: ServerEvent) => {
      invalidator.add(INVALIDATIONS[event.name]);
      if (!staff) return;

      switch (event.name) {
        case "delivery.created":
          useToastStore.getState().newToast(
            event.data.deliveryNumber
              ? `Принята поставка ${event.data.deliveryNumber}`
              : "Принята новая поставка",
            "success",
          );
          break;
        case "batch.expiring":
        case "batch.expired":
          batchNotifier.add(event.name, event.data);
          break;
      }
    };

    const connect = async () => {
      setStatus(retryMs === MIN_RETRY_MS ? "connecting" : "reconnecting");
      try {
        await openSseStream({
          signal: controller.signal,
          onOpen: () => {
            setStatus("open");
            retryMs = MIN_RETRY_MS;
            if (hasConnected) invalidator.add(Object.values(INVALIDATIONS).flat());
            hasConnected = true;
          },
          onEvent: (raw) => {
            const event = parseEvent(raw);
            if (event) handle(event);
          },
        });
      } catch (err) {
        if (controller.signal.aborted) return;
        if (err instanceof SseUnauthorizedError) {
          window.dispatchEvent(new CustomEvent("auth:logout"));
          return;
        }
      }

      if (controller.signal.aborted) return;
      setStatus("reconnecting");
      retryTimer = setTimeout(connect, retryMs);
      retryMs = Math.min(retryMs * 2, MAX_RETRY_MS);
    };

    connect();

    return () => {
      controller.abort();
      if (retryTimer) clearTimeout(retryTimer);
      invalidator.dispose();
      batchNotifier.dispose();
      setStatus("idle");
    };
  }, [userId, staff, queryClient]);

  return status;
}
