import { BASE_URL, getAccessToken, refreshAccessToken } from "@lib/api";

export interface RawSseEvent {
  event: string;
  data: string;
}

interface StreamOptions {
  signal: AbortSignal;
  onOpen: () => void;
  onEvent: (event: RawSseEvent) => void;
}

export class SseUnauthorizedError extends Error {}

const parseBlock = (block: string): RawSseEvent | null => {
  let event = "message";
  const data: string[] = [];

  for (const line of block.split("\n")) {
    if (!line || line.startsWith(":")) continue;
    const colon = line.indexOf(":");
    const field = colon === -1 ? line : line.slice(0, colon);
    const value = colon === -1 ? "" : line.slice(colon + 1).replace(/^ /, "");

    if (field === "event") event = value;
    if (field === "data") data.push(value);
  }

  return data.length ? { event, data: data.join("\n") } : null;
};

const request = (signal: AbortSignal) => {
  const token = getAccessToken();
  return fetch(`${BASE_URL}/events`, {
    signal,
    credentials: "include",
    cache: "no-store",
    headers: {
      Accept: "text/event-stream",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
};

export async function openSseStream({ signal, onOpen, onEvent }: StreamOptions) {
  let response = await request(signal);

  if (response.status === 401) {
    const token = await refreshAccessToken();
    if (!token) throw new SseUnauthorizedError();
    response = await request(signal);
    if (response.status === 401) throw new SseUnauthorizedError();
  }

  if (!response.ok || !response.body) {
    throw new Error(`SSE ${response.status}`);
  }

  onOpen();

  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) return;

    buffer += value.replace(/\r\n?/g, "\n");
    let boundary = buffer.indexOf("\n\n");
    while (boundary !== -1) {
      const parsed = parseBlock(buffer.slice(0, boundary));
      buffer = buffer.slice(boundary + 2);
      if (parsed) onEvent(parsed);
      boundary = buffer.indexOf("\n\n");
    }
  }
}
