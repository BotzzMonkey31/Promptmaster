import { Client } from '@stomp/stompjs';
import type { Frame, Message } from '@stomp/stompjs';
import { ref } from 'vue';
import type { WebSocketMessage, GameState, GameError } from '@/types/game';

export class WebSocketService {
  private client: Client;
  private subscriptions: { [key: string]: any } = {};
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectTimeout = 1000;

  public isConnected = ref(false);
  public lastError = ref<GameError | null>(null);

  constructor(brokerURL: string) {
    this.client = new Client({
      brokerURL,
      debug: function (str: string) {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    this.client.onConnect = () => {
      console.log('Connected to STOMP');
      this.resubscribe();
    };

    this.client.onStompError = (frame: Frame) => {
      console.error('STOMP error', frame);
    };
  }

  public connect(): void {
    this.client.activate();
  }

  public disconnect(): void {
    this.client.deactivate();
  }

  public subscribe(destination: string, callback: (message: any) => void): void {
    if (this.client.connected) {
      this.subscriptions[destination] = this.client.subscribe(destination, (message: Message) => {
        callback(JSON.parse(message.body));
      });
    }
  }

  public unsubscribe(destination: string): void {
    if (this.subscriptions[destination]) {
      this.subscriptions[destination].unsubscribe();
      delete this.subscriptions[destination];
    }
  }

  public send(destination: string, body: unknown): void {
    if (this.client.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body)
      });
    }
  }

  private resubscribe(): void {
    Object.keys(this.subscriptions).forEach(destination => {
      const callback = this.subscriptions[destination];
      this.subscribe(destination, callback);
    });
  }

  private handleDisconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      setTimeout(() => {
        this.reconnectAttempts++;
        this.connect();
      }, this.reconnectTimeout * Math.pow(2, this.reconnectAttempts));
    }
  }

  onMessage(callback: (data: GameState) => void) {
    if (this.client.connected) {
      this.subscribe('game_state', (message) => {
        try {
          const gameState: GameState = message.payload as GameState;
          callback(gameState);
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      });
    }
  }
}
