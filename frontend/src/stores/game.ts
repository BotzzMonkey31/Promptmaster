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
    aiResponse: null as { text: string, code: any, completeCode?: string } | null,
    connectionRetryCount: 0
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
          this.connectionRetryCount = 0;

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

          // Start connection health check
          this.startConnectionHealthCheck();
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.lastError = { message: 'Failed to connect to game server' };
          this.isConnected = false;
          this.attemptReconnect(gameId, player);
        },
        onDisconnect: () => {
          console.log('STOMP disconnected');
          this.isConnected = false;
          this.attemptReconnect(gameId, player);
        }
      });

      this.stompClient.activate();
    },

    attemptReconnect(gameId: string, player: Player) {
      if (this.connectionRetryCount >= 5) {
        console.log('Max reconnection attempts reached');
        return;
      }

      this.connectionRetryCount++;
      console.log(`Attempting to reconnect (attempt ${this.connectionRetryCount})...`);

      setTimeout(() => {
        if (!this.isConnected && this.stompClient) {
          console.log('Reconnecting to game server...');
          this.initializeGame(gameId, player);
        }
      }, 2000);
    },

    handleWebSocketMessage(message: WebSocketMessage) {
      console.log('Received game message:', message);

      // First check if we have a proper game ID in the message payload
      if (message.payload && typeof message.payload === 'object' && 'id' in message.payload) {
        const messageGameId = message.payload.id;
        const currentGameId = this.gameState?.id;

        // If this message is for a different game, ignore it
        if (currentGameId && messageGameId && messageGameId !== currentGameId) {
          console.log(`Ignoring message for different game ID: ${messageGameId}, our game is: ${currentGameId}`);
          return;
        }
      }

      // For direct game objects (not wrapped in payload)
      if (message && typeof message === 'object' && 'id' in message && message.id &&
          this.gameState?.id && message.id !== this.gameState.id) {
        console.log(`Ignoring direct game object for different game ID: ${message.id}, our game is: ${this.gameState.id}`);
        return;
      }

      // First check if this is a direct score update (personal message)
      // These come directly to the player's queue
      if (message.success === true && message.score !== undefined) {
        console.log('Detected direct score update message for this player:', message.score);

        // If we have gameState and currentPlayer, update the score directly
        if (this.gameState && this.currentPlayer) {
          const playerId = this.currentPlayer.id;

          // Ensure it's only processed by the player it's meant for
          if (this.gameState.playerStatus[playerId]) {
            const oldScore = this.gameState.playerStatus[playerId].score;
            this.gameState.playerStatus[playerId].score = message.score;
            console.log(`Updated player ${playerId} score: ${oldScore} -> ${message.score}`);

            // Emit a score update event for components to react to
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

      // Handle other message types
      switch (message.type) {
        case 'GAME_STATE':
          console.log('📊 GAME_STATE UPDATE: Received new game state');

          // Check if the payload contains a valid game state
          if (!message.payload || typeof message.payload !== 'object' || !('id' in message.payload)) {
            console.log('📊 GAME_STATE UPDATE: Invalid payload, ignoring');
            return;
          }

          // Check if this is for our current game
          if (this.gameState && message.payload.id !== this.gameState.id) {
            console.log(`📊 GAME_STATE UPDATE: Message for different game ID: ${message.payload.id}, our game is: ${this.gameState.id}`);
            return;
          }

          if (this.gameState && message.payload) {
            const oldRound = this.gameState.currentRound;
            const newRound = message.payload.currentRound;
            console.log(`📊 GAME_STATE UPDATE: Round change from ${oldRound} to ${newRound}`);

            if (newRound !== oldRound) {
              console.log('📊 GAME_STATE UPDATE: Round number has changed!');
            }

            // Check for inconsistent round number (going backward)
            if (newRound < oldRound) {
              console.log(`📊 GAME_STATE UPDATE: WARNING - Round going backward from ${oldRound} to ${newRound}, ignoring`);
              return;
            }
          }

          // Apply the game state update
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
          break;
        case 'ERROR':
          console.error('Received ERROR message from server:', message);
          if (message.payload && message.payload.message) {
            console.error('Error message:', message.payload.message);

            // Check for specific errors and handle them
            if (message.payload.message.includes('not in any active game')) {
              console.log('Player not in active game error detected - this is expected and will be handled');
            }
          }

          this.lastError = { message: message.payload?.message || 'Unknown error' };
          break;
      }
    },

    submitPrompt(prompt: string) {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
        console.error('Cannot submit prompt - missing required state');

        // Try to reconnect if not connected
        if (!this.isConnected && this.currentPlayer && this.gameState) {
          console.log('Not connected when trying to submit prompt, attempting to reconnect...');
          this.attemptReconnect(this.gameState.id, this.currentPlayer);
          return;
        }
        return;
      }

      console.log('Submitting prompt to server:', prompt);

      // Store variables locally
      const gameId = this.gameState.id;
      const playerId = this.currentPlayer.id;
      const username = this.currentPlayer.username;
      const picture = this.currentPlayer.picture || '';

      // First, ensure we're still in the game by re-joining
      this.stompClient.publish({
        destination: '/app/game/join',
        body: JSON.stringify({
          gameId: gameId,
          playerId: playerId,
          username: username,
          picture: picture
        })
      });

      console.log('Re-joined game before submitting prompt');

      // Then submit the prompt
      setTimeout(() => {
        if (this.stompClient && this.isConnected && this.gameState) {
          this.stompClient.publish({
            destination: '/app/game/prompt',
            body: JSON.stringify({
              gameId: gameId,
              playerId: playerId,
              prompt: prompt
            })
          });
          console.log('Prompt submitted after re-joining');

          // Start a connection health check after prompt submission
          this.startConnectionHealthCheck();
        } else {
          console.error('Lost connection after re-joining, could not submit prompt');
        }
      }, 500);
    },

    // Add a method to periodically check connection health
    startConnectionHealthCheck() {
      // Clear any existing interval
      this.stopConnectionHealthCheck();

      // Create a new interval
      const healthCheckInterval = window.setInterval(() => {
        if (!this.stompClient || !this.isConnected) {
          console.log('Connection health check: Not connected');

          if (this.currentPlayer && this.gameState) {
            this.attemptReconnect(this.gameState.id, this.currentPlayer);
          }
        } else {
          console.log('Connection health check: Connected');
        }
      }, 5000); // Check every 5 seconds

      // Store the interval ID for later cleanup
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

          // Try to reconnect if not connected but have other required state
          if (!this.isConnected && this.currentPlayer && this.gameState) {
            console.log('GameStore: Not connected when trying to submit solution, attempting to reconnect...');
            this.attemptReconnect(this.gameState.id, this.currentPlayer);

            // Set a retry after reconnection attempt
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

        // Capture state in local variables to avoid issues with "this" context
        const gameId = this.gameState.id;
        const playerId = this.currentPlayer.id;
        const username = this.currentPlayer.username;
        const picture = this.currentPlayer.picture || '';
        const stompClient = this.stompClient;

        // First, ensure we're fully joined and recognized by the server
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
          // Set isConnected to false to trigger reconnection on next attempt
          this.isConnected = false;
          reject(err);
          return;
        }

        // Use a delay to ensure the server has time to process the join
        setTimeout(() => {
          // Check connection state again before proceeding
          if (!this.isConnected || !this.stompClient) {
            console.error('GameStore: Lost connection after re-joining, will retry');
            this.isConnected = false;
            reject(new Error('Connection lost after joining'));
            return;
          }

          // Try to submit the solution
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
            resolve(true);

            // Add a connection health check to detect potential issues
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

        // Try to reconnect if not connected but have other required state
        if (!this.isConnected && this.currentPlayer && this.gameState) {
          console.log('GameStore: Not connected when trying to mark completion, attempting to reconnect...');
          this.attemptReconnect(this.gameState.id, this.currentPlayer);

          // Set a retry after reconnection attempt
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

      // Capture state in local variables to avoid issues with "this" context changing
      const gameId = this.gameState.id;
      const playerId = this.currentPlayer.id;
      const username = this.currentPlayer.username;
      const picture = this.currentPlayer.picture || '';
      const stompClient = this.stompClient;

      // First, ensure we're fully joined and recognized by the server
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
        // Set isConnected to false to trigger reconnection on next attempt
        this.isConnected = false;
        return;
      }

      // Use a longer delay to ensure the server has time to process the join
      setTimeout(() => {
        // Check connection state again before proceeding
        if (!this.isConnected || !this.stompClient) {
          console.error('GameStore: Lost connection after re-joining for completion, will retry');

          // Mark as disconnected
          this.isConnected = false;

          // Set a retry with increased delay
          setTimeout(() => {
            console.log('GameStore: Retrying puzzle completion after connection loss');
            this.markPuzzleCompleted();
          }, 3000);
          return;
        }

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
            // Verify connection again before sending alternative format
            if (this.stompClient && this.isConnected) {
              this.stompClient.publish({
                destination: '/app/game/complete',
                body: JSON.stringify({
                  gameId: gameId,
                  playerId: playerId
                })
              });
              console.log('GameStore: Puzzle completion also marked via alternative endpoint');

              // Manually update local state as a fallback if needed
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

          // Mark connection as potentially problematic
          this.isConnected = false;

          // Start connection health check to recover
          this.startConnectionHealthCheck();
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
      // First stop health check
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
      if (!this.stompClient || !this.isConnected || !this.gameState) return;

      this.stompClient.publish({
        destination: `/app/game/${this.gameState.id}/code`,
        body: JSON.stringify({
          playerId: playerId,
          code: code
        })
      });
    },

    // Add a new action for direct score updates
    handleScoreUpdate(scoreData: {
      score: number;
      timeBonus?: number;
      qualityScore?: number;
      correctnessScore?: number;
    }) {
      console.log('GameStore: handleScoreUpdate triggered with data:', scoreData);

      // Ensure we have valid score data
      if (!scoreData || typeof scoreData.score !== 'number') {
        console.error('GameStore: Invalid score data received:', scoreData);
        return;
      }

      // This method just exists so components can subscribe to it with $onAction
      console.log('GameStore: Score update processed. Components listening via $onAction should respond.');

      // Don't actually need to do anything here since the UI handles its own state
      // This method exists primarily for action subscription from components
    },

    async reconnectToGame() {
      if (!this.currentPlayer || !this.gameState) {
        console.error('Cannot reconnect - missing player or game state');
        return false;
      }

      console.log('Explicitly attempting to reconnect to game server...');

      // Force disconnect if there's an existing client
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

      // Return a promise that resolves when connection is established
      return new Promise<boolean>((resolve) => {
        // Setup with retry attempts
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

          // Initialize STOMP connection
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

            // Subscribe to game updates
            if (this.gameState) {
              client.subscribe(`/topic/game/${this.gameState.id}`, (message) => {
                try {
                  const data = JSON.parse(message.body);
                  this.handleWebSocketMessage(data);
                } catch (error) {
                  console.error('Error parsing WebSocket message:', error);
                }
              });

              // Subscribe to personal queue
              if (this.currentPlayer) {
                client.subscribe(`/user/${this.currentPlayer.id}/queue/game`, (message) => {
                  try {
                    const data = JSON.parse(message.body);
                    this.handleWebSocketMessage(data);
                  } catch (error) {
                    console.error('Error parsing personal WebSocket message:', error);
                  }
                });

                // Join the game
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

            // Success!
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

        // Start the connection process
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
        // Capture state in local variables to avoid null reference issues
        const gameId = this.gameState!.id;
        const playerId = this.currentPlayer!.id;
        const expectedNextRound = this.gameState!.currentRound + 1;
        const stompClient = this.stompClient!;

        try {
          // Ensure we're connected
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

          // Send request to start next round
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
