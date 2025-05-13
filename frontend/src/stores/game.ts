import { defineStore } from 'pinia';
import type { Player, GameState, WebSocketMessage } from '../types/game';
import { Client } from '@stomp/stompjs';

export const useGameStore = defineStore('game', {
  state: () => ({
    gameState: null as GameState | null,
    currentPlayer: null as Player | null,
    isConnected: false,
    lastError: null as { message: string } | null,
    stompClient: null as Client | null,
    aiResponse: null as { text: string, code: any, completeCode?: string } | null
  }),

  actions: {
    initializeGame(gameId: string, player: Player) {
      console.log('Initializing game:', gameId, player);
      this.currentPlayer = player;

      // Initialize STOMP connection
      const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
      const wsUrl = baseUrl.replace(/^http/, 'ws');

      if (this.stompClient) {
        this.stompClient.deactivate();
      }

      this.stompClient = new Client({
        brokerURL: `${wsUrl}/game`,
        debug: function(str) {
          console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('Game STOMP connection established');
          this.isConnected = true;

          // Subscribe to game updates
          this.stompClient?.subscribe(`/topic/game/${gameId}`, (message) => {
            try {
              const gameMessage = JSON.parse(message.body);
              this.handleWebSocketMessage(gameMessage);
            } catch (e) {
              console.error('Error processing game message:', e);
            }
          });

          // Subscribe to personal game messages
          this.stompClient?.subscribe(`/user/${player.id}/queue/game`, (message) => {
            try {
              const gameMessage = JSON.parse(message.body);
              this.handleWebSocketMessage(gameMessage);
            } catch (e) {
              console.error('Error processing personal game message:', e);
            }
          });

          // Join the game
          this.stompClient?.publish({
            destination: '/app/game/join',
            body: JSON.stringify({
              gameId: gameId,
              playerId: player.id,
              username: player.username,
              picture: player.picture
            })
          });
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.lastError = { message: 'Failed to connect to game server' };
        }
      });

      this.stompClient.activate();
    },

    handleWebSocketMessage(message: WebSocketMessage) {
      console.log('Received game message:', message);
      switch (message.type) {
        case 'GAME_STATE':
          this.gameState = message.payload;
          break;
        case 'AI_RESPONSE':
          console.log('Handling AI response:', message);
          console.log('Raw AI response message:', JSON.stringify(message));

          // Extract code, trying different possible formats
          let codeContent: any = message.code;

          if (typeof message.code === 'string') {
            // Code is directly a string
            codeContent = message.code;
          } else if (message.code && typeof message.code === 'object') {
            // Code might be nested in an object
            codeContent = (message.code as any).code || message.code;
          } else if (message.completeCode) {
            // Some responses use completeCode instead
            codeContent = message.completeCode;
          }

          this.aiResponse = {
            text: message.text || '',
            code: codeContent || '',
            completeCode: message.completeCode
          };
          break;
        case 'SUBMIT_SOLUTION':
          console.log('Handling solution submission response:', message);
          // Handle any response needed for submission
          break;
        case 'ERROR':
          console.error('Received ERROR message from server:', message);
          if (message.payload && message.payload.message) {
            console.error('Error message:', message.payload.message);

            // Check for specific errors and handle them
            if (message.payload.message.includes('not in any active game')) {
              console.log('Player not in active game error detected - this is expected and will be handled');
              // The rejoin is now built into each action that needs it
            }
          }

          this.lastError = { message: message.payload?.message || 'Unknown error' };
          break;
        // Add other message type handlers as needed
      }
    },

    submitPrompt(prompt: string) {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
        console.error('Cannot submit prompt - missing required state');
        return;
      }

      console.log('Submitting prompt to server:', prompt);
      this.stompClient.publish({
        destination: '/app/game/prompt',
        body: JSON.stringify({
          gameId: this.gameState.id,
          playerId: this.currentPlayer.id,
          prompt: prompt
        })
      });
    },

    submitSolution(code: string) {
      console.log('GameStore: Starting submitSolution');
      console.log('StompClient exists:', !!this.stompClient);
      console.log('Is connected:', this.isConnected);
      console.log('Current player exists:', !!this.currentPlayer);
      console.log('Game state exists:', !!this.gameState);

      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
        console.error('GameStore: Cannot submit solution - missing required state');
        return;
      }

      // Capture state in local variables
      const gameId = this.gameState.id;
      const playerId = this.currentPlayer.id;
      const username = this.currentPlayer.username;
      const picture = this.currentPlayer.picture || '';

      // First, ensure we're fully joined and recognized by the server
      this.stompClient.publish({
        destination: '/app/game/join',
        body: JSON.stringify({
          gameId: gameId,
          playerId: playerId,
          username: username,
          picture: picture
        })
      });

      console.log('GameStore: Re-joined game to ensure membership');
      console.log(`GameStore: Will publish to /app/game/${gameId}/submit after delay`);

      // Use a longer delay (1000ms) to ensure the server has time to process the join
      setTimeout(() => {
        if (this.stompClient && this.currentPlayer && this.gameState) {
          // Try a different approach to submit the solution
          try {
            // Format 1: Standard
            this.stompClient.publish({
              destination: `/app/game/${gameId}/submit`,
              body: JSON.stringify({
                code: code,
                playerId: playerId
              })
            });

            console.log('GameStore: Solution submitted via standard endpoint');

            // Format 2: Alternative (in case the first format has issues)
            setTimeout(() => {
              if (this.stompClient) {
                this.stompClient.publish({
                  destination: '/app/game/submit',
                  body: JSON.stringify({
                    gameId: gameId,
                    code: code,
                    playerId: playerId
                  })
                });
                console.log('GameStore: Solution also submitted via alternative endpoint');
              }
            }, 300);
          } catch (err) {
            console.error('Error submitting solution:', err);
          }
        }
      }, 1000);
    },

    markPuzzleCompleted() {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) return;

      // Capture state in local variables
      const gameId = this.gameState.id;
      const playerId = this.currentPlayer.id;
      const username = this.currentPlayer.username;
      const picture = this.currentPlayer.picture || '';

      // First, ensure we're fully joined and recognized by the server
      this.stompClient.publish({
        destination: '/app/game/join',
        body: JSON.stringify({
          gameId: gameId,
          playerId: playerId,
          username: username,
          picture: picture
        })
      });

      console.log('GameStore: Re-joined game to ensure membership before completing puzzle');

      // Use a longer delay to ensure the server has time to process the join
      setTimeout(() => {
        if (this.stompClient && this.currentPlayer && this.gameState) {
          try {
            // Format 1: Standard
            this.stompClient.publish({
              destination: `/app/game/${gameId}/complete`,
              body: JSON.stringify({
                playerId: playerId
              })
            });

            console.log('GameStore: Puzzle completion marked via standard endpoint');

            // Format 2: Alternative (in case the first format has issues)
            setTimeout(() => {
              if (this.stompClient) {
                this.stompClient.publish({
                  destination: '/app/game/complete',
                  body: JSON.stringify({
                    gameId: gameId,
                    playerId: playerId
                  })
                });
                console.log('GameStore: Puzzle completion also marked via alternative endpoint');
              }
            }, 300);
          } catch (err) {
            console.error('Error marking puzzle as completed:', err);
          }
        }
      }, 1000);
    },

    forfeitGame() {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) return;

      // Capture state in local variables
      const gameId = this.gameState.id;
      const playerId = this.currentPlayer.id;
      const username = this.currentPlayer.username;
      const picture = this.currentPlayer.picture || '';

      // First, ensure we're fully joined and recognized by the server
      this.stompClient.publish({
        destination: '/app/game/join',
        body: JSON.stringify({
          gameId: gameId,
          playerId: playerId,
          username: username,
          picture: picture
        })
      });

      console.log('GameStore: Re-joined game to ensure membership before forfeiting');

      // Use a longer delay to ensure the server has time to process the join
      setTimeout(() => {
        if (this.stompClient && this.currentPlayer && this.gameState) {
          try {
            // Format 1: Standard
            this.stompClient.publish({
              destination: `/app/game/${gameId}/forfeit`,
              body: JSON.stringify({
                playerId: playerId
              })
            });

            console.log('GameStore: Game forfeited via standard endpoint');

            // Format 2: Alternative (in case the first format has issues)
            setTimeout(() => {
              if (this.stompClient) {
                this.stompClient.publish({
                  destination: '/app/game/forfeit',
                  body: JSON.stringify({
                    gameId: gameId,
                    playerId: playerId
                  })
                });
                console.log('GameStore: Game also forfeited via alternative endpoint');
              }
            }, 300);
          } catch (err) {
            console.error('Error forfeiting game:', err);
          }
        }
      }, 1000);
    },

    isPlayerTurn(): boolean {
      if (!this.gameState || !this.currentPlayer) return false;
      return this.gameState.currentTurn === this.currentPlayer.id;
    },

    cleanup() {
      if (this.stompClient) {
        this.stompClient.deactivate();
        this.stompClient = null;
      }
      this.gameState = null;
      this.currentPlayer = null;
      this.isConnected = false;
      this.lastError = null;
      this.aiResponse = null;
    },

    updateCurrentCode(playerId: string, code: string) {
      if (!this.stompClient || !this.isConnected || !this.gameState) return;

      this.stompClient.publish({
        destination: `/app/game/${this.gameState.id}/code`,
        body: JSON.stringify({
          playerId: playerId,
          code: code
        })
      });
    }
  }
});
