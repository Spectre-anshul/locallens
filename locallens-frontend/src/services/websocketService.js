import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
  }

  connect(accessToken) {
    const wsUrl = import.meta.env.VITE_WS_BASE_URL || '/ws';
    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => console.log('[WS] Connected'),
      onDisconnect: () => console.log('[WS] Disconnected'),
      onStompError: (frame) => console.error('[WS] Error:', frame.headers.message),
    });
    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.subscriptions.clear();
    }
  }

  subscribe(destination, callback) {
    if (!this.client || !this.client.connected) return null;
    const sub = this.client.subscribe(destination, (message) => {
      callback(JSON.parse(message.body));
    });
    this.subscriptions.set(destination, sub);
    return sub;
  }

  unsubscribe(destination) {
    const sub = this.subscriptions.get(destination);
    if (sub) { sub.unsubscribe(); this.subscriptions.delete(destination); }
  }

  subscribeToReplan(tripId, callback) {
    return this.subscribe(`/topic/itinerary/${tripId}/replan`, callback);
  }

  subscribeToBookingUpdates(userId, callback) {
    return this.subscribe(`/topic/booking/${userId}/updates`, callback);
  }

  subscribeToCrowdUpdates(city, callback) {
    return this.subscribe(`/topic/map/${city}/crowd`, callback);
  }

  subscribeToMessages(userId, callback) {
    return this.subscribe(`/queue/messages/${userId}`, callback);
  }
}

export default new WebSocketService();
