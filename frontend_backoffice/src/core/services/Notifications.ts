import { writable } from 'svelte/store';
import SockJS from 'sockjs-client';
import { Client, type IMessage, type IFrame } from '@stomp/stompjs';
import { getToken } from './SessionStore';

export interface Notification {
  id: number;
  title: string;
  message: string;
  from: string;
  to: string;
  read: boolean;
  type: string;
  referenceId: number | null;
  referenceType: string | null;
  created: string;
  modified: string | null;
}

export const notifications = writable<Notification[]>([]);
export const unreadCount = writable<number>(0);

let stompClient: Client | null = null;

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

async function loadUnreadNotifications(): Promise<void> {
  try {
    const response = await fetch(`${API_BASE_URL}/notifications/unread`, {
      headers: { Authorization: `Bearer ${getToken()}` }
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const unread: Notification[] = await response.json();

    // Zamijeni listu (ne dodaj), da se ne dupliraju pri reconnectu
    notifications.set(unread);
    unreadCount.set(unread.length);
  } catch (err) {
    console.error('Greška pri učitavanju neprocitanih notifikacija:', err);
  }
}

export function connectNotificationsWebsocket(): void 
{
  if (stompClient?.active) return;

  stompClient = new Client({
    webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
    beforeConnect: () => {
      stompClient!.connectHeaders = { Authorization: `Bearer ${getToken()}` };
    },
    reconnectDelay: 5000,
    onConnect: async () => 
    {
      console.log('Notifications WebSocket connected');

      // 1. Prvo se pretplati, da ne propustiš ništa što stigne dok traje fetch
      stompClient!.subscribe('/user/queue/notifications', (message: IMessage) => 
      {
        console.log('Got new notification');

        const notification: Notification = JSON.parse(message.body);

        console.log(notification);

        notifications.update(list => {
          // zaštita od duplikata (npr. stigne i preko WS-a i preko REST-a)
          if (list.some(n => n.id === notification.id)) return list;
          unreadCount.update(count => count + 1);
          return [notification, ...list];
        });
      });

      // 2. Zatim učitaj postojeće neprocitane
      await loadUnreadNotifications();
    },

    onStompError: (frame: IFrame) => {
      console.error('STOMP greška:', frame);
    },
  });

  stompClient.activate();
}

export function disconnectNotifications(): void {
  stompClient?.deactivate();
  stompClient = null;
  notifications.set([]);
  unreadCount.set(0);
}