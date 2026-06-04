"use client";

import { useEffect, useRef, useCallback } from "react";
import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { useAuthStore } from "@/stores/useAuthStore";

type StompOptions = {
  url?: string;
  topics: string[];
  onMessage: (topic: string, message: IMessage) => void;
  enabled?: boolean;
};

export function useStompClient({ url, topics, onMessage, enabled = true }: StompOptions) {
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<StompSubscription[]>([]);
  const token = useAuthStore((s) => s.token);

  const connect = useCallback(() => {
    if (!enabled || !token) return;

    const client = new Client({
      brokerURL: url || process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8761/ws",
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      reconnectDelay: 5000,
    });

    client.onConnect = () => {
      subscriptionsRef.current = topics.map((topic) =>
        client.subscribe(topic, (msg) => onMessage(topic, msg))
      );
    };

    client.onStompError = (frame) => {
      console.error("STOMP error:", frame.headers["message"]);
    };

    client.activate();
    clientRef.current = client;
  }, [url, topics, onMessage, enabled, token]);

  const disconnect = useCallback(() => {
    subscriptionsRef.current.forEach((sub) => sub.unsubscribe());
    subscriptionsRef.current = [];
    clientRef.current?.deactivate();
    clientRef.current = null;
  }, []);

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return { disconnect, reconnect: connect };
}
