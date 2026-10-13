import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';

type Callback<T = any> = (payload: T) => void;

class WebRealtimeSocket {
  private client: Client | null = null;
  private orderSubscribers: Set<Callback> = new Set();
  private deliverySubscribers: Set<Callback> = new Set();
  private cartSubscribers: Set<Callback> = new Set();
  private notificationSubscribers: Set<Callback> = new Set();
  private connected = false;

  private connect() {
    if (this.connected || this.client) {
      return;
    }

    const token = sessionStorage.getItem('token');
    if (!token) {
      return;
    }

    this.client = new Client({
      webSocketFactory: () => {
        const wsUrl = import.meta.env.VITE_WS_BASE_URL;
        if (!wsUrl && !import.meta.env.DEV) {
          throw new Error('VITE_WS_BASE_URL environment variable is required for production');
        }
        return new SockJS(wsUrl ? `${wsUrl}/ws` : '/ws');
      },
      reconnectDelay: 4000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: () => {},
    });

    this.client.beforeConnect = () => {
      const currentToken = sessionStorage.getItem('token');
      if (!currentToken || !this.client) {
        throw new Error('Phiên đăng nhập không còn hợp lệ');
      }
      this.client.connectHeaders = {
        Authorization: `Bearer ${currentToken}`,
      };
    };

    this.client.onConnect = () => {
      this.connected = true;
      this.subscribeTopics();
    };

    this.client.onDisconnect = () => {
      this.connected = false;
    };

    this.client.onStompError = (frame) => {
      console.error('[Realtime] STOMP error:', frame.headers['message'], frame.body);
    };

    this.client.onWebSocketClose = () => {
      this.connected = false;
    };

    this.client.activate();
  }

  private subscribeTopics() {
    if (!this.client) {
      return;
    }

    this.client.subscribe('/user/queue/orders', (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);
        this.orderSubscribers.forEach(cb => cb(payload));
      } catch (error) {
        console.error('[Realtime] Failed to parse order message:', error);
      }
    });

    this.client.subscribe('/user/queue/deliveries', (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);
        this.deliverySubscribers.forEach(cb => cb(payload));
      } catch (error) {
        console.error('[Realtime] Failed to parse delivery message:', error);
      }
    });

    this.client.subscribe('/user/queue/cart', (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);
        this.cartSubscribers.forEach(cb => cb(payload));
      } catch (error) {
        console.error('[Realtime] Failed to parse cart message:', error);
      }
    });

    this.client.subscribe('/user/queue/notifications', (message: IMessage) => {
      this.publish(message, this.notificationSubscribers, 'thông báo');
    });

    const savedUser = sessionStorage.getItem('auth_user');
    if (!savedUser) {
      return;
    }

    try {
      const user = JSON.parse(savedUser) as { role?: string; restaurantId?: string };
      if (user.role === 'restaurant' && user.restaurantId) {
        this.client.subscribe(
          `/topic/restaurants/${user.restaurantId}/orders`,
          message => this.publish(message, this.orderSubscribers, 'đơn hàng'),
        );
        this.client.subscribe(
          `/topic/restaurants/${user.restaurantId}/deliveries`,
          message => this.publish(message, this.deliverySubscribers, 'giao hàng'),
        );
      } else if (user.role === 'admin') {
        this.client.subscribe(
          '/topic/admin/orders',
          message => this.publish(message, this.orderSubscribers, 'đơn hàng'),
        );
        this.client.subscribe(
          '/topic/admin/deliveries',
          message => this.publish(message, this.deliverySubscribers, 'giao hàng'),
        );
      }
    } catch (error) {
      console.error('[Realtime] Không thể đọc thông tin phiên đăng nhập:', error);
    }
  }

  private publish(message: IMessage, subscribers: Set<Callback>, label: string) {
    try {
      const payload = JSON.parse(message.body);
      subscribers.forEach(callback => callback(payload));
    } catch (error) {
      console.error(`[Realtime] Không thể đọc cập nhật ${label}:`, error);
    }
  }

  subscribeOrderUpdates(callback: Callback) {
    this.orderSubscribers.add(callback);
    this.connect();
    return () => {
      this.orderSubscribers.delete(callback);
    };
  }

  subscribeDeliveryUpdates(callback: Callback) {
    this.deliverySubscribers.add(callback);
    this.connect();
    return () => {
      this.deliverySubscribers.delete(callback);
    };
  }

  onOrderUpdate(callback: Callback) {
    return this.subscribeOrderUpdates(callback);
  }

  onCartUpdate(callback: Callback) {
    this.cartSubscribers.add(callback);
    this.connect();
    return () => {
      this.cartSubscribers.delete(callback);
    };
  }

  subscribeNotificationUpdates(callback: Callback) {
    this.notificationSubscribers.add(callback);
    this.connect();
    return () => {
      this.notificationSubscribers.delete(callback);
    };
  }

  disconnect() {
    const client = this.client;
    this.connected = false;
    this.client = null;
    if (client?.active) {
      void client.deactivate();
    }
  }
}

const realtimeSocket = new WebRealtimeSocket();

export const subscribeOrderUpdates = (callback: Callback) =>
  realtimeSocket.subscribeOrderUpdates(callback);

export const subscribeDeliveryUpdates = (callback: Callback) =>
  realtimeSocket.subscribeDeliveryUpdates(callback);

export const subscribeNotificationUpdates = (callback: Callback) =>
  realtimeSocket.subscribeNotificationUpdates(callback);

export { realtimeSocket };

