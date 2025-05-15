import { defineStore } from 'pinia';
import type { Player, GameState, WebSocketMessage } from '../types/game';
import { Client } from '@stomp/stompjs';
import { watch } from 'vue';

export const useGameStore = defineStore('game', {
  state: () => ({
    gameState: null as GameState | null,
    currentPlayer: null as Player | null,
    isConnected: false,
    lastError: null as { message: string } | null,
    stompClient: null as Client | null,
    aiResponse: null as { text: string, code: any, completeCode?: string } | null,
    connectionRetryCount: 0
  }),

  actions: {
    initializeGame(gameId: string, player: Player) {
      this.currentPlayer = player;

      const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
      const wsUrl = baseUrl.replace(/^http/, 'ws');

      if (this.stompClient) {
        this.stompClient.deactivate();
      }

      this.stompClient = new Client({
        brokerURL: `${wsUrl}/game`,
        debug: function(str) {
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          this.isConnected = true;
          this.connectionRetryCount = 0;

          this.stompClient?.subscribe(`/topic/game/${gameId}`, (message) => {
            try {
              const gameMessage = JSON.parse(message.body);
              this.handleWebSocketMessage(gameMessage);
            } catch (e) {
              console.error('Error processing game message:', e);
            }
          });

          this.stompClient?.subscribe(`/user/${player.id}/queue/game`, (message) => {
            try {
              const gameMessage = JSON.parse(message.body);
              this.handleWebSocketMessage(gameMessage);
            } catch (e) {
              console.error('Error processing personal game message:', e);
            }
          });

          this.stompClient?.publish({
            destination: '/app/game/join',
            body: JSON.stringify({
              gameId: gameId,
              playerId: player.id,
              username: player.username,
              picture: player.picture
            })
          });

          this.startConnectionHealthCheck();
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.lastError = { message: 'Failed to connect to game server' };
          this.isConnected = false;
          this.attemptReconnect(gameId, player);
        },
        onDisconnect: () => {
          this.isConnected = false;
          this.attemptReconnect(gameId, player);
        }
      });

      this.stompClient.activate();
    },

    attemptReconnect(gameId: string, player: Player) {
      if (this.connectionRetryCount >= 5) {
        return;
      }

      this.connectionRetryCount++;

      setTimeout(() => {
        if (!this.isConnected && this.stompClient) {
          this.initializeGame(gameId, player);
        }
      }, 2000);
    },

    handleWebSocketMessage(message: WebSocketMessage) {
      if (message.payload && typeof message.payload === 'object' && 'id' in message.payload) {
        const messageGameId = message.payload.id;
        const currentGameId = this.gameState?.id;

        if (currentGameId && messageGameId && messageGameId !== currentGameId) {
          return;
        }
      }

      if (message && typeof message === 'object' && 'id' in message && message.id &&
          this.gameState?.id && message.id !== this.gameState.id) {
        return;
      }

      if (message.success === true && message.score !== undefined) {
        if (this.gameState && this.currentPlayer) {
          const playerId = this.currentPlayer.id;

          if (this.gameState.playerStatus[playerId]) {
            const oldScore = this.gameState.playerStatus[playerId].score;

            if (!this.gameState.scores) {
              this.gameState.scores = {};
            }

            const currentRound = this.gameState.currentRound;

            this.gameState.playerStatus[playerId].score = message.score;

            this.handleScoreUpdate({
              score: message.score,
              timeBonus: message.timeBonus,
              qualityScore: message.qualityScore,
              correctnessScore: message.correctnessScore
            });
          }
        }
        return;
      }

      switch (message.type) {
        case 'GAME_STATE':
          if (!message.payload || typeof message.payload !== 'object' || !('id' in message.payload)) {
            return;
          }

          if (this.gameState && message.payload.id !== this.gameState.id) {
            return;
          }

          if (this.gameState && message.payload) {
            const oldRound = this.gameState.currentRound;
            const newRound = message.payload.currentRound;

            const oldState = this.gameState.state;
            const newState = message.payload.state;

            if (newRound < oldRound) {
              return;
            }
          }

          this.gameState = message.payload;
          break;
        case 'AI_RESPONSE':
          let codeContent: any = message.code;

          if (typeof message.code === 'string') {
            codeContent = message.code;
          } else if (message.code && typeof message.code === 'object') {
            codeContent = (message.code as any).code || message.code;
          } else if (message.completeCode) {
            codeContent = message.completeCode;
          }

          this.aiResponse = {
            text: message.text || '',
            code: codeContent || '',
            completeCode: message.completeCode
          };
          break;
        case 'SUBMIT_SOLUTION':
          break;
        case 'ERROR':
          console.error('Received ERROR message from server:', message);
          if (message.payload && message.payload.message) {
            console.error('Error message:', message.payload.message);

            if (message.payload.message.includes('not in any active game')) {
            }
          }

          this.lastError = { message: message.payload?.message || 'Unknown error' };
          break;
      }
    },

    submitPrompt(prompt: string) {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
        console.error('Cannot submit prompt - missing required state');

        if (!this.isConnected && this.currentPlayer && this.gameState) {
          console.log('Not connected when trying to submit prompt, attempting to reconnect...');
          this.attemptReconnect(this.gameState.id, this.currentPlayer);
          return Promise.reject(new Error('Not connected to game server'));
        }
        return Promise.reject(new Error('Missing required state'));
      }

      console.log('Submitting prompt to server:', prompt);

      this.aiResponse = null;

      return new Promise((resolve, reject) => {
        const gameId = this.gameState!.id;
        const playerId = this.currentPlayer!.id;
        const username = this.currentPlayer!.username;
        const picture = this.currentPlayer!.picture || '';
        const stompClient = this.stompClient!;

        const responseTimeout = setTimeout(() => {
          console.error('Timeout waiting for AI response');
          reject(new Error('AI response timeout'));
        }, 30000);

        const unwatch = watch(() => this.aiResponse, (newResponse) => {
          if (newResponse !== null) {
            console.log('AI response detected in watcher:', newResponse);
            clearTimeout(responseTimeout);
            unwatch();

            if (newResponse) {
              console.log('AI response received successfully');
              resolve(newResponse);
            } else {
              console.error('AI response was unexpected format');
              reject(new Error('Invalid AI response format'));
            }
          }
        }, { deep: true });

        stompClient.publish({
          destination: '/app/game/join',
          body: JSON.stringify({
            gameId: gameId,
            playerId: playerId,
            username: username,
            picture: picture
          })
        });

        console.log('Re-joined game before submitting prompt');

        setTimeout(() => {
          if (stompClient && this.isConnected) {
            stompClient.publish({
              destination: '/app/game/prompt',
              body: JSON.stringify({
                gameId: gameId,
                playerId: playerId,
                prompt: prompt
              })
            });
            console.log('Prompt submitted after re-joining');

            this.startConnectionHealthCheck();
          } else {
            console.error('Lost connection after re-joining, could not submit prompt');
            clearTimeout(responseTimeout);
            unwatch();
            reject(new Error('Lost connection after re-joining'));
          }
        }, 500);
      });
    },

    startConnectionHealthCheck() {
      this.stopConnectionHealthCheck();

      const healthCheckInterval = window.setInterval(() => {
        if (!this.stompClient || !this.isConnected) {
          console.log('Connection health check: Not connected');

          if (this.currentPlayer && this.gameState) {
            this.attemptReconnect(this.gameState.id, this.currentPlayer);
          }
        } else {
          console.log('Connection health check: Connected');
        }
      }, 5000);

      (this as any).healthCheckIntervalId = healthCheckInterval;
    },

    stopConnectionHealthCheck() {
      if ((this as any).healthCheckIntervalId) {
        window.clearInterval((this as any).healthCheckIntervalId);
        (this as any).healthCheckIntervalId = null;
      }
    },

    submitSolution(code: string) {
      console.log('GameStore: Starting submitSolution');
      console.log('StompClient exists:', !!this.stompClient);
      console.log('Is connected:', this.isConnected);
      console.log('Current player exists:', !!this.currentPlayer);
      console.log('Game state exists:', !!this.gameState);

      return new Promise((resolve, reject) => {
        if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
          console.error('GameStore: Cannot submit solution - missing required state');

          if (!this.isConnected && this.currentPlayer && this.gameState) {
            console.log('GameStore: Not connected when trying to submit solution, attempting to reconnect...');
            this.attemptReconnect(this.gameState.id, this.currentPlayer);

            setTimeout(() => {
              if (this.isConnected && this.stompClient && this.currentPlayer && this.gameState) {
                console.log('GameStore: Reconnected successfully, retrying solution submission');
                this.submitSolution(code).then(resolve).catch(reject);
              } else {
                console.error('GameStore: Failed to reconnect for solution submission');
                reject(new Error('Failed to reconnect for submission'));
              }
            }, 2500);
            return;
          }

          reject(new Error('Missing required state for submission'));
          return;
        }

        const gameId = this.gameState.id;
        const playerId = this.currentPlayer.id;
        const username = this.currentPlayer.username;
        const picture = this.currentPlayer.picture || '';
        const stompClient = this.stompClient;

        try {
          stompClient.publish({
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
        } catch (err) {
          console.error('GameStore: Error re-joining game:', err);
          this.isConnected = false;
          reject(err);
          return;
        }

        setTimeout(() => {
          if (!this.isConnected || !this.stompClient) {
            console.error('GameStore: Lost connection after re-joining, will retry');
            this.isConnected = false;
            reject(new Error('Connection lost after joining'));
            return;
          }

          try {
            this.stompClient.publish({
              destination: `/app/game/${gameId}/submit`,
              body: JSON.stringify({
                code: code,
                playerId: playerId
              })
            });

            console.log('GameStore: Solution submitted via standard endpoint');
            resolve(true);

            this.startConnectionHealthCheck();
          } catch (err) {
            console.error('GameStore: Error submitting solution:', err);
            this.isConnected = false;
            this.startConnectionHealthCheck();
            reject(err);
          }
        }, 500);
      });
    },

    markPuzzleCompleted() {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
        console.error('GameStore: Cannot mark puzzle as completed - missing required state');

        if (!this.isConnected && this.currentPlayer && this.gameState) {
          console.log('GameStore: Not connected when trying to mark completion, attempting to reconnect...');
          this.attemptReconnect(this.gameState.id, this.currentPlayer);

          setTimeout(() => {
            if (this.isConnected && this.stompClient) {
              console.log('GameStore: Reconnected successfully, retrying puzzle completion');
              this.markPuzzleCompleted();
            } else {
              console.error('GameStore: Failed to reconnect for puzzle completion');
            }
          }, 2500);
        }
        return;
      }

      const gameId = this.gameState.id;
      const playerId = this.currentPlayer.id;
      const username = this.currentPlayer.username;
      const picture = this.currentPlayer.picture || '';
      const stompClient = this.stompClient;

      try {
        stompClient.publish({
          destination: '/app/game/join',
          body: JSON.stringify({
            gameId: gameId,
            playerId: playerId,
            username: username,
            picture: picture
          })
        });

        console.log('GameStore: Re-joined game to ensure membership before completing puzzle');
      } catch (err) {
        console.error('GameStore: Error re-joining game for completion:', err);
        this.isConnected = false;
        return;
      }

      setTimeout(() => {
        if (!this.isConnected || !this.stompClient) {
          console.error('GameStore: Lost connection after re-joining for completion, will retry');

          this.isConnected = false;

          setTimeout(() => {
            console.log('GameStore: Retrying puzzle completion after connection loss');
            this.markPuzzleCompleted();
          }, 3000);
          return;
        }

        try {
          this.stompClient.publish({
            destination: `/app/game/${gameId}/complete`,
            body: JSON.stringify({
              playerId: playerId
            })
          });

          console.log('GameStore: Puzzle completion marked via standard endpoint');

          setTimeout(() => {
            if (this.stompClient && this.isConnected) {
              this.stompClient.publish({
                destination: '/app/game/complete',
                body: JSON.stringify({
                  gameId: gameId,
                  playerId: playerId
                })
              });
              console.log('GameStore: Puzzle completion also marked via alternative endpoint');

              if (this.gameState && this.currentPlayer) {
                const playerId = this.currentPlayer.id;
                if (this.gameState.playerStatus[playerId]) {
                  this.gameState.playerStatus[playerId].hasCompleted = true;
                  console.log('GameStore: Manually updated local player completion status');
                }
              }
            } else {
              console.error('GameStore: Connection lost before sending alternative complete format');
            }
          }, 300);
        } catch (err) {
          console.error('Error marking puzzle as completed:', err);

          this.isConnected = false;

          this.startConnectionHealthCheck();
        }
      }, 1000);
    },

    forfeitGame() {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) return;

      const gameId = this.gameState.id;
      const playerId = this.currentPlayer.id;
      const username = this.currentPlayer.username;
      const picture = this.currentPlayer.picture || '';

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

      setTimeout(() => {
        if (this.stompClient && this.currentPlayer && this.gameState) {
          try {
            this.stompClient.publish({
              destination: `/app/game/${gameId}/forfeit`,
              body: JSON.stringify({
                playerId: playerId
              })
            });

            console.log('GameStore: Game forfeited via standard endpoint');

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
      this.stopConnectionHealthCheck();

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
      if (!this.stompClient || !this.isConnected || !this.gameState) {
        console.log('Cannot update code - disconnected or missing game state');
        return;
      }

      try {
        this.stompClient.publish({
          destination: `/app/game/${this.gameState.id}/code`,
          body: JSON.stringify({
            playerId: playerId,
            code: code
          })
        });
        console.log(`Code updated for player ${playerId}`);
      } catch (error) {
        console.error('Error updating code:', error);
        if (this.currentPlayer && this.gameState) {
          this.attemptReconnect(this.gameState.id, this.currentPlayer);
        }
      }
    },

    handleScoreUpdate(scoreData: {
      score: number;
      timeBonus?: number;
      qualityScore?: number;
      correctnessScore?: number;
    }) {
      console.log('GameStore: handleScoreUpdate triggered with data:', scoreData);

      if (!scoreData || typeof scoreData.score !== 'number') {
        console.error('GameStore: Invalid score data received:', scoreData);
        return;
      }

      console.log('GameStore: Score update processed. Components listening via $onAction should respond.');

    },

    async reconnectToGame() {
      if (!this.currentPlayer || !this.gameState) {
        console.error('Cannot reconnect - missing player or game state');
        return false;
      }

      console.log('Explicitly attempting to reconnect to game server...');

      if (this.stompClient) {
        try {
          this.stompClient.deactivate();
          console.log('Deactivated existing STOMP client');
        } catch (e) {
          console.warn('Error deactivating STOMP client:', e);
        }
        this.stompClient = null;
      }

      this.isConnected = false;

      return new Promise<boolean>((resolve) => {
        let retryCount = 0;
        const maxRetries = 5;
        const connect = () => {
          if (retryCount >= maxRetries) {
            console.error(`Failed to reconnect after ${maxRetries} attempts`);
            resolve(false);
            return;
          }

          retryCount++;
          console.log(`Connection attempt ${retryCount}/${maxRetries}`);

          const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
          const wsUrl = baseUrl.replace(/^http/, 'ws');

          const client = new Client({
            brokerURL: `${wsUrl}/game`,
            debug: function (str) {
              console.log('STOMP:', str);
            },
            reconnectDelay: 5000
          });

          client.onConnect = (frame) => {
            console.log('Game STOMP connection established');
            this.isConnected = true;
            this.stompClient = client;
            this.connectionRetryCount = 0;

            if (this.gameState) {
              client.subscribe(`/topic/game/${this.gameState.id}`, (message) => {
                try {
                  const data = JSON.parse(message.body);
                  this.handleWebSocketMessage(data);
                } catch (error) {
                  console.error('Error parsing WebSocket message:', error);
                }
              });

              if (this.currentPlayer) {
                client.subscribe(`/user/${this.currentPlayer.id}/queue/game`, (message) => {
                  try {
                    const data = JSON.parse(message.body);
                    this.handleWebSocketMessage(data);
                  } catch (error) {
                    console.error('Error parsing personal WebSocket message:', error);
                  }
                });

                client.publish({
                  destination: '/app/game/join',
                  body: JSON.stringify({
                    gameId: this.gameState.id,
                    playerId: this.currentPlayer.id,
                    username: this.currentPlayer.username,
                    picture: this.currentPlayer.picture || ''
                  })
                });
              }
            }

            this.startConnectionHealthCheck();
            resolve(true);
          };

          client.onStompError = (frame) => {
            console.error('STOMP error:', frame.headers, frame.body);
            setTimeout(connect, 1000 * Math.min(retryCount + 1, 5));
          };

          client.onWebSocketClose = () => {
            console.warn('WebSocket connection closed');
            this.isConnected = false;
            if (retryCount < maxRetries) {
              setTimeout(connect, 1000 * Math.min(retryCount + 1, 5));
            } else {
              resolve(false);
            }
          };

          try {
            client.activate();
          } catch (e) {
            console.error('Error activating STOMP client:', e);
            setTimeout(connect, 1000 * Math.min(retryCount + 1, 5));
          }
        };

        connect();
      });
    },

    startNextRound() {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
        console.error('GameStore: Cannot start next round - missing required state');
        return Promise.reject(new Error('Missing required state'));
      }

      console.log('GameStore: Starting next round request');

      return new Promise((resolve, reject) => {
        const gameId = this.gameState!.id;
        const playerId = this.currentPlayer!.id;
        const expectedNextRound = this.gameState!.currentRound + 1;
        const stompClient = this.stompClient!;

        try {
          if (!this.isConnected) {
            console.log('GameStore: Reconnecting before starting next round');
            this.reconnectToGame().then(connected => {
              if (connected) {
                console.log('GameStore: Successfully reconnected, retrying next round request');
                this.startNextRound().then(resolve).catch(reject);
              } else {
                reject(new Error('Failed to reconnect'));
              }
            });
            return;
          }

          stompClient.publish({
            destination: `/app/game/${gameId}/next-round`,
            body: JSON.stringify({
              playerId: playerId,
              gameId: gameId,
              currentRound: this.gameState!.currentRound,
              expectedNextRound: expectedNextRound
            })
          });

          console.log(`GameStore: Next round request sent for game ${gameId} - expecting round ${expectedNextRound}`);
          resolve(true);
        } catch (err) {
          console.error('GameStore: Error requesting next round:', err);
          reject(err);
        }
      });
    }
  }
});
