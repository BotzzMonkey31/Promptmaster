<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">
      <h2 class="text-2xl font-semibold text-center mb-6">Game Lobby</h2>
      <p class="text-center text-gray-600 mb-4">Find an opponent or invite a friend to a match!</p>

      <!-- Game Notifications -->
      <div v-if="gameNotification" class="bg-blue-100 border border-blue-300 text-blue-800 px-4 py-3 rounded relative mb-6">
        <span v-html="gameNotification.message"></span>
        <div v-if="gameNotification.type === 'challenge'" class="flex justify-center gap-4 mt-3">
          <button @click="acceptChallenge" class="bg-green-500 text-white px-4 py-2 rounded">Accept Challenge</button>
          <button @click="rejectChallenge" class="bg-red-500 text-white px-4 py-2 rounded">Decline</button>
        </div>
      </div>

      <!-- Game Controls -->
      <div class="flex justify-center gap-4 mb-6">
        <button
          @click="findRandomOpponent"
          class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 transition"
          :disabled="findingOpponent"
        >
          {{ findingOpponent ? 'Searching...' : 'Find Random Opponent' }}
        </button>
        <button
          @click="toggleInviteModal"
          class="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600 transition"
        >
          Invite a Friend
        </button>
      </div>

      <!-- Invite Friend Modal -->
      <div v-if="showInviteModal" class="fixed inset-0 flex items-center justify-center z-50">
        <div class="absolute inset-0 bg-black bg-opacity-50" @click="toggleInviteModal"></div>
        <div class="bg-white p-6 rounded-lg shadow-lg z-10 w-full max-w-md">
          <h3 class="text-lg font-semibold mb-4">Invite a Friend</h3>
          <p class="mb-4">Share this link with your friend:</p>
          <div class="flex">
            <input
              type="text"
              readonly
              :value="inviteLink"
              class="flex-1 border rounded-l px-3 py-2"
            />
            <button
              @click="copyInviteLink"
              class="bg-blue-500 text-white px-4 py-2 rounded-r"
            >
              Copy
            </button>
          </div>
          <p v-if="linkCopied" class="text-green-600 mt-2">Link copied to clipboard!</p>
          <button @click="toggleInviteModal" class="mt-4 w-full bg-gray-300 text-gray-800 px-4 py-2 rounded">Close</button>
        </div>
      </div>

      <!-- Available Players -->
      <div class="bg-white p-6 shadow rounded-lg mb-6">
        <h3 class="text-lg font-semibold mb-4">Available Players</h3>
        <div v-if="players.length === 0" class="text-gray-500 text-center py-4">
          No players are currently available.
        </div>
        <ul v-else>
          <li
            v-for="player in players"
            :key="player.userId"
            class="flex justify-between items-center py-3 border-b"
          >
            <div class="flex items-center">
              <img :src="player.picture || defaultAvatar" class="w-8 h-8 rounded-full mr-3" alt="Avatar" @error="handleImageError" />
              <span>{{ player.username }}</span>
              <span class="ml-2 text-sm text-gray-500">ELO: {{ player.elo }}</span>
            </div>
            <button
              @click="challengePlayer(player)"
              class="bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600 transition"
              :disabled="challengingSomeone"
            >
              Challenge
            </button>
          </li>
        </ul>
      </div>

      <!-- Your Rank -->
      <div class="bg-white p-6 shadow rounded-lg">
        <h3 class="text-lg font-semibold mb-4">Your Rank</h3>
        <div v-if="error" class="text-red-500 mb-4">
          {{ error }}
        </div>
        <div v-else-if="loading" class="text-gray-500 mb-4">Loading user data...</div>
        <div v-else>
          <div v-if="user" class="flex items-center">
            <img :src="user.picture" class="w-10 h-10 rounded-full mr-3" alt="Avatar" @error="handleImageError" />
            <div>
              <p class="font-semibold">{{ user.username }}</p>
              <p>
                Rank: <span class="text-blue-500">{{ currentRank }}</span>
              </p>
              <p class="text-sm text-gray-600">ELO: {{ user.elo }}</p>
              <p class="text-sm text-gray-600">Country: {{ user.country || 'Unknown' }}</p>
            </div>
          </div>
          <div class="mt-4">
            <p class="text-sm text-gray-600">Progress to next rank: {{ progressToNextRank }}%</p>
            <div class="w-full bg-gray-300 h-2 rounded overflow-hidden">
              <div class="bg-blue-500 h-full" :style="{ width: progressToNextRank + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <ChatBox />
    </main>
  </div>
</template>

<script lang="ts">
import Cookies from 'js-cookie'
import ChatBox from '../components/ChatBox.vue'
import apiClient from '../services/api'
import { useRouter } from 'vue-router'
import { ref, onMounted, onBeforeUnmount } from 'vue'

// Add type declaration for gameSocketInstance on Window interface
declare global {
  interface Window {
    gameSocketInstance?: WebSocket;
  }
}

interface RankThreshold {
  rank: string
  min: number
  max: number
}

const rankThresholds: RankThreshold[] = [
  { rank: 'UNRANKED', min: 0, max: 49 },
  { rank: 'BRONZE', min: 50, max: 999 },
  { rank: 'SILVER', min: 1000, max: 1399 },
  { rank: 'GOLD', min: 1400, max: 1799 },
  { rank: 'DIAMOND', min: 1800, max: 2199 },
  { rank: 'EMERALD', min: 2200, max: 2599 },
  { rank: 'PLATINUM', min: 2600, max: 2999 },
  { rank: 'PROMPTMASTER', min: 3000, max: 9999 },
]

interface User {
  id?: number
  email: string
  name?: string
  username: string
  picture: string
  country: string
  rank: string
  elo: number
  createdAt?: string
  updatedAt?: string
}

interface Player {
  userId: number
  username: string
  picture: string
  elo: number
}

interface GameNotification {
  type: string
  message: string
  data?: any
}

export default {
  name: 'GameLobby',
  components: { ChatBox },
  setup() {
    const router = useRouter()
    return { router }
  },
  data() {
    return {
      players: [] as Player[],
      user: null as User | null,
      loading: true,
      error: null as string | null,
      gameSocket: null as WebSocket | null,
      gameNotification: null as GameNotification | null,
      challengingSomeone: false,
      findingOpponent: false,
      showInviteModal: false,
      inviteLink: '',
      linkCopied: false,
      challengerId: null as number | null,
      defaultAvatar: 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y',
      preserveSocket: false, // Add this flag to control socket preservation
      matchmakingTimer: null as number | null, // Changed from NodeJS.Timeout to number
      transitioningToGame: false // Move from setup() to data()
    }
  },
  computed: {
    currentRank(): string {
      if (!this.user) return 'UNRANKED'
      const rank = rankThresholds.find((r) => this.user!.elo >= r.min && this.user!.elo <= r.max)
      return rank ? rank.rank : 'PROMPTMASTER'
    },
    progressToNextRank(): number {
      if (!this.user) return 0
      const currentRank = rankThresholds.find(
        (r) => this.user!.elo >= r.min && this.user!.elo <= r.max,
      )
      if (!currentRank) return 100

      const nextRank = rankThresholds.find((r) => r.min > currentRank.min)
      if (!nextRank) return 100

      const progress = ((this.user.elo - currentRank.min) / (nextRank.min - currentRank.min)) * 100
      return Math.min(Math.max(Math.round(progress), 0), 100)
    },
  },
  methods: {
    handleImageError(event: Event) {
      const target = event.target as HTMLImageElement
      target.src = this.defaultAvatar
    },
    async loadUserData() {
      try {
        // Log the beginning of user data fetching
        console.log('Starting to fetch user data')

        const savedUser = Cookies.get('user')
        if (!savedUser) {
          console.log('No user cookie found')
          this.error = 'No user is logged in'
          this.loading = false
          return
        }

        try {
          const userData = JSON.parse(savedUser)
          console.log('Parsed user cookie data:', userData)

          if (!userData.email) {
            console.error('No email found in user cookie')
            this.error = 'User data is incomplete'
            this.loading = false
            return
          }

          // Add timeout to API call and use query parameters instead
          const response = await apiClient.get('/users/email', {
            params: { email: userData.email },  // Send as query parameter
            timeout: 10000 // 10 second timeout
          })

          if (!response || response.status !== 200) {
            throw new Error(`Server responded with status ${response?.status || 'unknown'}`)
          }

          if (!response.data) {
            console.error('Response exists but no data received')
            throw new Error('No data received from server')
          }

          this.user = response.data
          this.loading = false

          // If user data is loaded successfully, initialize the WebSocket connection
          this.initGameWebSocket()
        } catch (parseError) {
          console.error('Error parsing user cookie:', parseError)
          this.error = 'Invalid user data format'
          this.loading = false
        }
      } catch (error: any) {
        console.error('Error fetching user data:', error)
        this.error = `Failed to load user data: ${error.message || 'Unknown error'}`
        this.loading = false
      }
    },
    initGameWebSocket() {
      if (!this.user || !this.user.id) {
        console.error('Cannot initialize game WebSocket - user ID is missing')
        return
      }

      // Close previous socket if it exists
      if (this.gameSocket) {
        this.gameSocket.close()
      }

      const baseUrl = import.meta.env.VITE_API_BASE_URL.replace('http', 'ws')
      this.gameSocket = new WebSocket(`${baseUrl}/game`)

      this.gameSocket.onopen = () => {
        console.log('Game WebSocket connection established')
        // Join the lobby when connection is established
        this.joinLobby()
      }

      this.gameSocket.onmessage = (event) => {
        const message = JSON.parse(event.data)
        console.log('Game WebSocket message received:', message)

        this.handleGameMessage(message)
      }

      this.gameSocket.onclose = () => {
        console.log('Game WebSocket connection closed')
        // Try to reconnect after 5 seconds
        setTimeout(() => this.initGameWebSocket(), 5000)
      }

      this.gameSocket.onerror = (error) => {
        console.error('Game WebSocket error:', error)
      }
    },
    joinLobby() {
      if (!this.gameSocket || this.gameSocket.readyState !== WebSocket.OPEN || !this.user?.id) {
        console.error('Cannot join lobby - socket not ready or user ID missing')
        return
      }

      const message = {
        type: 'JOIN_LOBBY',
        content: '',
        userId: this.user.id
      }

      this.gameSocket.send(JSON.stringify(message))
    },
    leaveLobby() {
      if (!this.gameSocket || !this.user?.id) return

      const message = {
        type: 'LEAVE_LOBBY',
        content: '',
        userId: this.user.id
      }

      this.gameSocket.send(JSON.stringify(message))
    },
    findRandomOpponent() {
      if (!this.gameSocket || !this.user?.id) return

      this.findingOpponent = true

      // Show notification that search has started
      this.showNotification('info', 'Searching for an opponent...')

      // Enhanced opponent matching options
      const matchPreferences = {
        eloRange: 200,             // Starting ELO range to match within
        strictMatching: false      // Whether to use strict matching or gradual range expansion
      }

      const message = {
        type: 'FIND_OPPONENT',
        content: JSON.stringify(matchPreferences),
        userId: this.user.id
      }

      this.gameSocket.send(JSON.stringify(message))

      // Set a timeout to reset the button state if no response is received
      setTimeout(() => {
        if (this.findingOpponent) {
          this.findingOpponent = false
          this.showNotification('info', 'No response from server. Please try again.')
        }
      }, 15000) // Extended to 15 seconds
    },
    challengePlayer(player: Player) {
      if (!this.gameSocket || !this.user?.id) return

      this.challengingSomeone = true

      const message = {
        type: 'CHALLENGE_PLAYER',
        content: player.userId.toString(),
        userId: this.user.id
      }

      this.showNotification('info', `Challenge sent to ${player.username}. Waiting for response...`)
      this.gameSocket.send(JSON.stringify(message))
    },
    async acceptChallenge() {
      const message = {
        type: 'ACCEPT_CHALLENGE',
        content: JSON.stringify({
          challengerId: this.challengerId
        })
      }

      if (this.gameSocket && this.gameSocket.readyState === WebSocket.OPEN) {
        // Set the transition flag
        this.transitioningToGame = true

        // Set the preserve flag to true so the socket doesn't get closed
        this.preserveSocket = true

        // Store the socket instance in the window object for the Game component to use
        if (this.gameSocket) {
          window.gameSocketInstance = this.gameSocket
        }

        // Send the accept message
        this.gameSocket.send(JSON.stringify(message))

        // Navigate to the game view with a flag to preserve the connection
        this.router.push({
          path: '/game',
          query: {
            preserveConnection: 'true',
            gameId: this.gameNotification?.data?.gameId
          }
        })
      } else {
        this.error = 'WebSocket connection is not available'
      }
    },
    rejectChallenge() {
      if (!this.gameSocket || !this.user?.id || !this.challengerId) return

      const message = {
        type: 'REJECT_CHALLENGE',
        content: this.challengerId.toString(),
        userId: this.user.id
      }

      this.gameSocket.send(JSON.stringify(message))
      this.gameNotification = null
      this.challengerId = null
    },
    handleGameMessage(message: any) {
      // Only process messages meant for this user
      if (message.userId !== this.user?.id) {
        return
      }

      switch (message.type) {
        case 'LOBBY_UPDATE':
          this.handleLobbyUpdate(message)
          break
        case 'CHALLENGE_RECEIVED':
          this.handleChallengeReceived(message)
          break
        case 'CHALLENGE_SENT':
          this.handleChallengeSent(message)
          break
        case 'CHALLENGE_REJECTED':
          this.handleChallengeRejected(message)
          break
        case 'NO_OPPONENT':
          this.handleNoOpponent(message)
          break
        case 'GAME_STARTED':
          this.handleGameStarted(message)
          break
        case 'ERROR':
          this.handleError(message)
          break
      }
    },
    handleLobbyUpdate(message: any) {
      try {
        const playersData = JSON.parse(message.content)
        this.players = playersData
        console.log('Available players updated:', this.players)
      } catch (e) {
        console.error('Error parsing lobby update data:', e)
      }
    },
    handleChallengeReceived(message: any) {
      try {
        const challengeInfo = JSON.parse(message.content)
        this.challengerId = challengeInfo.challengerId

        const challengeMessage = `
          <div class="flex items-center">
            <img src="${challengeInfo.challengerPicture || this.defaultAvatar}" class="w-8 h-8 rounded-full mr-2" alt="Challenger" />
            <div>
              <strong>${challengeInfo.challengerName}</strong> (ELO: ${challengeInfo.challengerElo})
              <p>has challenged you to a game!</p>
            </div>
          </div>
        `

        this.showNotification('challenge', challengeMessage)

      } catch (e) {
        console.error('Error handling challenge:', e)
      }
    },
    handleChallengeSent(message: any) {
      this.challengingSomeone = false
    },
    handleChallengeRejected(message: any) {
      this.challengingSomeone = false
      this.showNotification('info', message.content)
    },
    handleNoOpponent(message: any) {
      this.findingOpponent = false
      this.showNotification('info', message.content)
    },
    handleGameStarted(message: any) {
      try {
        const gameData = JSON.parse(message.content)
        // Store the socket instance globally so Game.vue can use it
        if (this.gameSocket) {
          window.gameSocketInstance = this.gameSocket
        }

        // Set flag to preserve socket connection when transitioning

        // Set flag to preserve socket connection when transitioning
        this.preserveSocket = true

        // Navigate to game view with necessary data - fixed to use params instead of incorporating gameId in the path
        this.$router.push({
          name: 'game',  // Use named route instead of path with parameter
          params: { gameId: gameData.gameId }, // This will be properly set as a route param
          query: {
            opponentId: gameData.opponentId,
            opponentName: gameData.opponentName,
            opponentPicture: gameData.opponentPicture,
            opponentElo: gameData.opponentElo,
            rounds: gameData.rounds,
            currentRound: gameData.currentRound,
            currentPuzzleId: gameData.currentPuzzleId,
            timePerRound: gameData.timePerRound,
          }
        })
      } catch (e) {
        console.error('Error handling game start:', e)
      }
    },
    handleError(message: any) {
      this.showNotification('error', message.content)
    },
    showNotification(type: string, message: string) {
      this.gameNotification = { type, message }

      // Auto-hide non-challenge notifications after 5 seconds
      if (type !== 'challenge') {
        setTimeout(() => {
          if (this.gameNotification && this.gameNotification.message === message) {
            this.gameNotification = null
          }
        }, 5000)
      }
    },
    toggleInviteModal() {
      this.showInviteModal = !this.showInviteModal
      this.linkCopied = false

      if (this.showInviteModal) {
        const baseUrl = window.location.origin
        // Generate invite link - this is just a placeholder; in a real app,
        // you might create a unique game invitation code
        this.inviteLink = `${baseUrl}/invite?user=${this.user?.username || 'friend'}`
      }
    },
    copyInviteLink() {
      navigator.clipboard.writeText(this.inviteLink)
        .then(() => {
          this.linkCopied = true
        })
        .catch(err => {
          console.error('Failed to copy link:', err)
        })
    }
  },
  created() {
    this.loadUserData()
    this.inviteLink = `${window.location.origin}/invite`
  },
  beforeUnmount() {
    // Clean up WebSocket connection only if not preserving it
    if (this.gameSocket && !this.preserveSocket) {
      this.gameSocket.close()
    }

    // Clean up timers
    if (this.matchmakingTimer) {
      clearInterval(this.matchmakingTimer)
    }
  },
}
</script>

<style>
body {
  font-family: Arial, sans-serif;
}
</style>
