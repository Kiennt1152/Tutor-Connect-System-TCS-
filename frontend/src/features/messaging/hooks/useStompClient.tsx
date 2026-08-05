import { useEffect, useState } from 'react';
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { authStorage } from '../../../shared/auth/authStorage';

function resolveBrokerUrl(): string {
  const explicit = import.meta.env.VITE_WS_URL as string | undefined;
  if (explicit) return explicit;
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

type Listener = (payload: unknown) => void;

type ManagedSubscription = {
  listeners: Set<Listener>;
  subscription: StompSubscription | null;
};

/**
 * Client STOMP duy nhat cho toan app (module-level singleton) de tat ca hook
 * (useConversations, useMessages, ...) dung chung 1 ket noi WebSocket.
 */
class StompManager {
  private client: Client | null = null;
  private readonly subscriptions = new Map<string, ManagedSubscription>();
  private readonly connectionListeners = new Set<(connected: boolean) => void>();
  private connected = false;

  isConnected() {
    return this.connected;
  }

  onConnectionChange(listener: (connected: boolean) => void) {
    this.connectionListeners.add(listener);
    return () => {
      this.connectionListeners.delete(listener);
    };
  }

  ensureConnected() {
    if (this.client) return;
    const token = authStorage.getToken();
    if (!token) return;

    const client = new Client({
      brokerURL: resolveBrokerUrl(),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true;
        this.connectionListeners.forEach((listener) => listener(true));
        this.subscriptions.forEach((entry, destination) => {
          entry.subscription = client.subscribe(destination, (message) =>
            this.dispatch(entry, message),
          );
        });
      },
      onDisconnect: () => {
        this.connected = false;
        this.connectionListeners.forEach((listener) => listener(false));
      },
      onWebSocketClose: () => {
        this.connected = false;
        this.connectionListeners.forEach((listener) => listener(false));
      },
      onStompError: () => {
        this.connected = false;
        this.connectionListeners.forEach((listener) => listener(false));
      },
    });

    this.client = client;
    client.activate();
  }

  private dispatch(entry: ManagedSubscription, message: IMessage) {
    let payload: unknown = message.body;
    try {
      payload = JSON.parse(message.body);
    } catch {
      // giu nguyen string neu body khong phai JSON
    }
    entry.listeners.forEach((listener) => listener(payload));
  }

  subscribe(destination: string, listener: Listener) {
    let entry = this.subscriptions.get(destination);
    if (!entry) {
      entry = { listeners: new Set(), subscription: null };
      this.subscriptions.set(destination, entry);
    }
    entry.listeners.add(listener);

    if (this.client?.connected && !entry.subscription) {
      entry.subscription = this.client.subscribe(destination, (message) =>
        this.dispatch(entry!, message),
      );
    }

    return () => {
      const current = this.subscriptions.get(destination);
      if (!current) return;
      current.listeners.delete(listener);
      if (current.listeners.size === 0) {
        current.subscription?.unsubscribe();
        this.subscriptions.delete(destination);
      }
    };
  }

  publish(destination: string, body: unknown) {
    if (!this.client?.connected) return;
    this.client.publish({ destination, body: JSON.stringify(body) });
  }
}

const stompManager = new StompManager();

/**
 * Quan ly ket noi STOMP qua WebSocket (dung chung singleton). Tu dong ket noi
 * khi co token, tu dong reconnect khi mat ket noi.
 */
export function useStompClient() {
  const [connected, setConnected] = useState(stompManager.isConnected());

  useEffect(() => {
    stompManager.ensureConnected();
    return stompManager.onConnectionChange(setConnected);
  }, []);

  return {
    connected,
    subscribe: stompManager.subscribe.bind(stompManager),
    publish: stompManager.publish.bind(stompManager),
  };
}
