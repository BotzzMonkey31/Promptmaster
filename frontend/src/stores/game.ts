import { defineStore } from 'pinia'
import type { Player, GameState, WebSocketMessage } from '../types/game'
import { Client } from '@stomp/stompjs'

export const useGameStore = defineStore('game', {
  state: () => ({
    gameState: null as GameState | null,
    currentPlayer: null as Player | null,
    isConnected: false,
    lastError: null as { message: string } | null,
    stompClient: null as Client | null,
    aiResponse: null as { text: string; code: any; completeCode?: string } | null,
    connectionAttempts: 0,
  }),

  actions: {
    initializeGame(gameId: string, player: Player) {
      this.currentPlayer = player
      this.connectionAttempts = 0

      const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
      const wsUrl = baseUrl.replace(/^http/, 'ws')

      if (this.stompClient) {
        this.stompClient.deactivate()
      }

      this.stompClient = new Client({
        brokerURL: `${wsUrl}/game`,
        debug: function (str) {
          console.log('STOMP:', str)
        },
        reconnectDelay: 2000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected')
          this.isConnected = true
          this.connectionAttempts = 0

          // Subscribe to game updates
          this.stompClient?.subscribe(`/topic/game/${gameId}`, (message) => {
            try {
              const gameMessage = JSON.parse(message.body)
              console.log('Received game message:', gameMessage)
              this.handleWebSocketMessage(gameMessage)
            } catch (e) {
              console.error('Error processing game message:', e)
            }
          })

          // Subscribe to personal messages
          this.stompClient?.subscribe(`/user/${player.id}/queue/game`, (message) => {
            try {
              const gameMessage = JSON.parse(message.body)
              console.log('Received personal game message:', gameMessage)
              this.handleWebSocketMessage(gameMessage)
            } catch (e) {
              console.error('Error processing personal game message:', e)
            }
          })

          // Join the game
          console.log('Sending join game message for game:', gameId)
          this.stompClient?.publish({
            destination: '/app/game/join',
            body: JSON.stringify({
              gameId: gameId,
              playerId: player.id,
            }),
          })
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame)
          this.isConnected = false
          this.lastError = { message: 'Failed to connect to game server' }
          this.handleReconnect()
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected')
          this.isConnected = false
          this.handleReconnect()
        },
      })

      console.log('Activating WebSocket connection')
      this.stompClient.activate()
    },

    handleReconnect() {
      if (this.connectionAttempts >= 5) {
        this.lastError = { message: 'Failed to connect after multiple attempts' }
        return
      }

      this.connectionAttempts++
      console.log(`Attempting to reconnect (attempt ${this.connectionAttempts})`)

      setTimeout(() => {
        if (!this.isConnected && this.stompClient) {
          this.stompClient.activate()
        }
      }, 2000 * this.connectionAttempts)
    },

    handleWebSocketMessage(message: WebSocketMessage) {
      console.log('Processing WebSocket message:', message)

      switch (message.type) {
        case 'GAME_STATE':
          if (message.payload && typeof message.payload === 'object' && 'id' in message.payload) {
            console.log('Updating game state:', message.payload)
            console.log('Game state puzzle:', message.payload.puzzle)
            this.gameState = message.payload
            console.log('Game state after update:', this.gameState)
          } else {
            console.error('Invalid game state payload:', message.payload)
          }
          break
        case 'AI_RESPONSE':
          this.aiResponse = {
            text: message.text || '',
            code: message.code || '',
            completeCode: message.completeCode,
          }
          break
        case 'PLAYER_COMPLETION':
          // Update player completion status in game state
          if (this.gameState && message.playerId) {
            const playerStatus = this.gameState.playerStatus[message.playerId]
            if (playerStatus) {
              playerStatus.hasCompleted = true
            }
          }
          break
        case 'ERROR':
          console.error('Received ERROR message from server:', message)
          this.lastError = { message: message.payload?.message || 'Unknown error' }
          break
      }
    },

    submitSolution(code: string) {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
        return Promise.reject(new Error('Not connected to game server'))
      }

      this.stompClient.publish({
        destination: `/app/game/${this.gameState.id}/submit`,
        body: JSON.stringify({
          playerId: this.currentPlayer.id,
          code: code,
        }),
      })
    },

    markPuzzleCompleted() {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) return

      this.stompClient.publish({
        destination: `/app/game/${this.gameState.id}/complete`,
        body: JSON.stringify({
          playerId: this.currentPlayer.id,
        }),
      })
    },

    forfeitGame() {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) return

      this.stompClient.publish({
        destination: `/app/game/${this.gameState.id}/forfeit`,
        body: JSON.stringify({
          playerId: this.currentPlayer.id,
        }),
      })
    },

    updateCurrentCode(playerId: string, code: string) {
      if (!this.stompClient || !this.isConnected || !this.gameState) return

      this.stompClient.publish({
        destination: `/app/game/${this.gameState.id}/code`,
        body: JSON.stringify({
          playerId: playerId,
          code: code,
        }),
      })
    },

    sendPrompt(prompt: string) {
      if (!this.stompClient || !this.isConnected || !this.currentPlayer || !this.gameState) {
        return Promise.reject(new Error('Not connected to game server'))
      }

      return new Promise((resolve, reject) => {
        try {
          this.stompClient?.publish({
            destination: '/app/game/prompt',
            body: JSON.stringify({
              gameId: this.gameState?.id,
              playerId: this.currentPlayer?.id,
              prompt: prompt,
            }),
          })
          resolve(true)
        } catch (error) {
          reject(error)
        }
      })
    },

    cleanup() {
      if (this.stompClient) {
        this.stompClient.deactivate()
        this.stompClient = null
      }
      this.gameState = null
      this.currentPlayer = null
      this.isConnected = false
      this.lastError = null
      this.aiResponse = null
    },
  },
})
