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

const token = getToken();

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export function connectNotifications(): void 
{
  console.log('connecting notifications...');
  if (stompClient?.active) return; // već konektovan, ne diraj

  stompClient = new Client({
    webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,

    onConnect: () => {
      console.log('Notifikacije: konektovan');

      stompClient!.subscribe('/user/queue/notifications', (message: IMessage) => {
        const notification: Notification = JSON.parse(message.body);

        notifications.update(list => [notification, ...list]);
        unreadCount.update(count => count + 1);
      });
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
}