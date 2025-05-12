<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">

      <!-- Game Notifications -->
      <div v-if="gameNotification" class="bg-blue-100 border border-blue-300 text-blue-800 px-4 py-3 rounded relative mb-6">
        <span v-html="gameNotification.message"></span>
      </div>

      <!-- Active Game -->
      <div v-if="inGame && currentGame" class="bg-white p-6 shadow rounded-lg mb-6">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-semibold">Game in Progress</h3>
          <div class="text-sm">
            <span class="font-bold">Round {{ currentGame.currentRound }}/3</span>
            <span class="mx-2">|</span>
            <span class="font-bold">Time: {{ formatTime(timeRemaining) }}</span>
          </div>
        </div>

        <div class="flex justify-between items-center mb-6">
          <div class="flex items-center">
            <img :src="user?.picture || defaultAvatar" class="w-10 h-10 rounded-full mr-3" alt="Your avatar" />
            <div>
              <p class="font-semibold">You</p>
              <p class="text-sm">Score: {{ currentGame.yourScore }}</p>
            </div>
          </div>

          <div class="text-xl font-bold">VS</div>

          <div class="flex items-center">
            <div class="text-right mr-3">
              <p class="font-semibold">{{ currentGame.opponentName }}</p>
              <p class="text-sm">Score: {{ currentGame.opponentScore }}</p>
            </div>
            <img :src="currentGame.opponentPicture" class="w-10 h-10 rounded-full" alt="Opponent avatar" />
          </div>
        </div>

        <!-- Puzzle Solving Interface -->
        <div v-if="currentPuzzle && !showPuzzleButton" class="mt-6">
          <div class="flex flex-col items-stretch p-6 bg-light-50 rounded-xl shadow-lg mx-auto gap-6">
            <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 shadow-sm">
              <h3 class="text-lg font-semibold text-gray-700">Puzzle: {{ currentPuzzle.name }}</h3>
              <p class="mt-2">{{ currentPuzzle.description }}</p>
            </div>

            <div class="flex items-start w-full gap-4">
              <img :src="user?.picture || defaultAvatar" class="w-10 h-10 rounded-full border-2 border-blue-500 shadow" alt="Avatar" />
              <div class="flex-1 p-4 rounded-3 bg-white text-15px text-gray-800 shadow relative">
                <div class="absolute left--2.5 top-5 border-y-10px border-r-10px border-l-0 border-y-transparent border-r-white"></div>
                {{ textBubble }}
              </div>
            </div>

            <div class="flex w-full gap-6">
              <div class="flex-1 flex flex-col gap-3 min-w-0">
                <textarea
                  v-model="userInput"
                  placeholder="Enter your input here"
                  class="w-full min-h-24 max-h-40 p-4 border border-gray-200 rounded-2 text-14px text-gray-800 bg-white shadow-sm focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-colors resize-y font-inherit box-border overflow-auto"
                ></textarea>
                <div class="flex justify-between w-full">
                  <button
                    @click="handleSubmit"
                    :disabled="isSubmitting"
                    class="px-6 py-2.5 bg-blue-500 text-white border-none rounded-2 cursor-pointer text-14px font-500 transition-colors shadow hover:bg-blue-600 active:translate-y-0.25 disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center min-w-[100px]"
                  >
                    <span v-if="!isSubmitting">Submit</span>
                    <span v-else class="flex items-center">
                      <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      Processing...
                    </span>
                  </button>
                  <button
                    @click="markCompleted"
                    :disabled="isSubmitting"
                    class="px-6 py-2.5 bg-green-500 text-white border-none rounded-2 cursor-pointer text-14px font-500 transition-colors shadow hover:bg-green-600 active:translate-y-0.25 disabled:opacity-70 disabled:cursor-not-allowed"
                  >
                    Complete Puzzle
                  </button>
                </div>
              </div>
            </div>

            <div class="w-full h-75 rounded-2 overflow-hidden shadow" ref="editorDiv"></div>
          </div>
        </div>

        <div v-else-if="inGame" class="text-center">
          <p class="text-center mb-4">
            Solve the <span class="font-bold">Multi-Step Puzzle #{{ currentGame.currentPuzzleId }}</span>
          </p>

          <div class="text-center">
            <button
              v-if="showPuzzleButton"
              @click="loadPuzzle"
              class="bg-blue-500 text-white px-6 py-3 rounded-lg shadow hover:bg-blue-600 transition"
            >
              Start Puzzle
            </button>
            <button
              @click="forfeitGame"
              class="bg-red-500 text-white px-6 py-3 rounded-lg shadow hover:bg-red-600 transition ml-4"
            >
              Forfeit Game
            </button>
          </div>
        </div>
      </div>

      <!-- Return to Lobby -->
      <div v-if="!inGame && !showGameResults" class="text-center mt-8">
        <p class="mb-4 text-gray-600">Waiting for a game to start...</p>
        <button
          @click="$router.push('/vs')"
          class="bg-blue-500 text-white px-6 py-3 rounded-lg shadow hover:bg-blue-600 transition"
        >
          Return to Lobby
        </button>
      </div>

      <!-- Game Results Modal -->
      <div v-if="showGameResults && gameResults" class="fixed inset-0 flex items-center justify-center z-50">
        <div class="absolute inset-0 bg-black bg-opacity-50"></div>
        <div class="bg-white p-8 rounded-lg shadow-lg z-10 w-full max-w-md">
          <h3 class="text-2xl font-bold mb-6 text-center">
            {{ gameResults.result === 'WIN' ? '🏆 Victory!' : gameResults.result === 'LOSS' ? '❌ Defeat' : '🤝 Draw' }}
          </h3>

          <div class="flex justify-between items-center mb-6">
            <div class="text-center">
              <p class="text-xl font-bold">{{ gameResults.yourScore }}</p>
              <p class="text-sm">Your Score</p>
            </div>
            <div class="text-3xl">vs</div>
            <div class="text-center">
              <p class="text-xl font-bold">{{ gameResults.opponentScore }}</p>
              <p class="text-sm">Opponent's Score</p>
            </div>
          </div>

          <div class="bg-gray-100 p-4 rounded mb-6">
            <p class="text-center">
              <span v-if="gameResults.eloChange > 0" class="text-green-600">+{{ gameResults.eloChange }} ELO</span>
              <span v-else-if="gameResults.eloChange < 0" class="text-red-600">{{ gameResults.eloChange }} ELO</span>
              <span v-else class="text-gray-600">±0 ELO</span>
            </p>
          </div>

          <div class="flex justify-between space-x-4">
            <button @click="playAgain" class="w-1/2 bg-blue-500 text-white py-2 rounded-lg">
              Play Again
            </button>
            <button @click="returnToLobby" class="w-1/2 bg-gray-500 text-white py-2 rounded-lg">
              Return to Lobby
            </button>
          </div>
        </div>
      </div>

      <!-- Score Popup Modal -->
      <div v-if="showScorePopup && scoreDetails" class="fixed inset-0 flex items-center justify-center z-50">
        <div class="absolute inset-0 bg-black bg-opacity-70"></div>
        <div class="bg-white p-8 rounded-lg shadow-xl max-w-md w-full animate-fade-in">
          <h2 class="text-3xl font-bold text-center mb-2">{{ scoreDetails.hasFailed ? 'Puzzle Failed!' : 'Puzzle Complete!' }}</h2>
          <p class="text-gray-600 text-center mb-6">Here's how you did:</p>

          <div class="bg-gradient-to-r rounded-lg p-6 mb-6 text-center"
               :class="scoreDetails.hasFailed ? 'from-red-500 to-red-700' : 'from-blue-500 to-purple-600'">
            <h3 class="text-white text-lg font-medium mb-1">Your Score</h3>
            <div class="text-5xl font-bold text-white">{{ scoreDetails.totalScore || 0 }}</div>
            <div v-if="scoreDetails.hasFailed" class="text-white mt-2 text-sm">
              Scores below 40 in any category result in failure
            </div>
          </div>

          <div class="space-y-3 mb-6">
            <div class="flex justify-between items-center" :class="{'bg-red-50 p-2 rounded-md': scoreDetails.timeScore < 40}">
              <span class="text-gray-700">Time ({{ formatTime(scoreDetails.timeSeconds) }})</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{'text-red-600': scoreDetails.timeScore < 40}">{{ scoreDetails.timeScore || 0 }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
                <span v-if="scoreDetails.timeScore < 40" class="text-red-500 ml-2 text-sm">Failed</span>
              </div>
            </div>

            <div class="flex justify-between items-center" :class="{'bg-red-50 p-2 rounded-md': scoreDetails.efficiencyScore < 40}">
              <span class="text-gray-700">Efficiency ({{ scoreDetails.interactionCount || 0 }} interactions)</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{'text-red-600': scoreDetails.efficiencyScore < 40}">{{ scoreDetails.efficiencyScore || 0 }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
                <span v-if="scoreDetails.efficiencyScore < 40" class="text-red-500 ml-2 text-sm">Failed</span>
              </div>
            </div>

            <div class="flex justify-between items-center" :class="{'bg-red-50 p-2 rounded-md': scoreDetails.tokenScore < 40}">
              <span class="text-gray-700">Token Usage</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{'text-red-600': scoreDetails.tokenScore < 40}">{{ scoreDetails.tokenScore || 0 }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
                <span v-if="scoreDetails.tokenScore < 40" class="text-red-500 ml-2 text-sm">Failed</span>
              </div>
            </div>

            <div class="flex justify-between items-center" :class="{'bg-red-50 p-2 rounded-md': scoreDetails.correctnessScore < 40}">
              <span class="text-gray-700">Code Correctness</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{'text-red-600': scoreDetails.correctnessScore < 40}">{{ scoreDetails.correctnessScore || 0 }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
                <span v-if="scoreDetails.correctnessScore < 40" class="text-red-500 ml-2 text-sm">Failed</span>
              </div>
            </div>

            <div class="flex justify-between items-center" :class="{'bg-red-50 p-2 rounded-md': scoreDetails.codeQualityScore < 40}">
              <span class="text-gray-700">Code Quality</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{'text-red-600': scoreDetails.codeQualityScore < 40}">{{ scoreDetails.codeQualityScore || 0 }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
                <span v-if="scoreDetails.codeQualityScore < 40" class="text-red-500 ml-2 text-sm">Failed</span>
              </div>
            </div>
          </div>

          <div class="flex justify-center">
            <button
              @click="closeScorePopup"
              class="px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-full font-medium hover:from-blue-600 hover:to-purple-700"
            >
              Continue
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script lang="ts">
import Cookies from 'js-cookie'
import apiClient from '../services/api'
import { useRoute, useRouter } from 'vue-router'
import { onMounted, onBeforeUnmount, ref, reactive } from 'vue'
import { EditorView, lineNumbers, highlightActiveLineGutter, highlightSpecialChars } from "@codemirror/view"
import { EditorState } from "@codemirror/state"
import { javascript } from "@codemirror/lang-javascript"
import { keymap } from "@codemirror/view"
import { defaultKeymap } from "@codemirror/commands"
import { syntaxHighlighting, defaultHighlightStyle } from "@codemirror/language"
import { bracketMatching } from "@codemirror/language"
import { closeBrackets } from "@codemirror/autocomplete"
import { history } from "@codemirror/commands"

// Add TypeScript declaration for the global gameSocketInstance
declare global {
  interface Window {
    gameSocketInstance?: WebSocket;
  }
}

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

interface GameNotification {
  type: string
  message: string
  data?: any
}

interface CurrentGame {
  gameId: string
  opponentId: number
  opponentName: string
  opponentPicture: string
  opponentElo: number
  rounds: number
  currentRound: number
  currentPuzzleId: number
  timePerRound: number
  yourScore: number
  opponentScore: number
}

interface GameResults {
  gameId: string
  result: 'WIN' | 'LOSS' | 'DRAW' | 'WIN_BY_FORFEIT' | 'FORFEIT'
  yourScore: number
  opponentScore: number
  eloChange: number
}

interface Puzzle {
  id: number
  name: string
  description: string
  code: string
  type: string
}

interface ScoreDetails {
  totalScore: number
  timeScore: number
  efficiencyScore: number
  tokenScore: number
  correctnessScore: number
  codeQualityScore: number
  timeSeconds: number
  interactionCount: number
  hasFailed: boolean
}

export default {
  name: 'GameView',
  setup() {
    const route = useRoute()
    const router = useRouter()

    const gameSocket = ref<WebSocket | null>(null)
    const preserveSocket = ref(false)
    const user = ref<User | null>(null)
    const gameId = ref<string | null>(null)
    const inGame = ref(false)
    const currentGame = ref<CurrentGame | null>(null)
    const timeRemaining = ref(0)
    const timerInterval = ref<ReturnType<typeof setInterval> | null>(null)
    const gameNotification = ref<GameNotification | null>(null)
    const showGameResults = ref(false)
    const gameResults = ref<GameResults | null>(null)
    const defaultAvatar = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'
    const loading = ref(true)
    const error = ref<string | null>(null)

    // Puzzle solving related refs
    const currentPuzzle = ref<Puzzle | null>(null)
    const showPuzzleButton = ref(true)
    const userInput = ref('')
    const code = ref('')
    const textBubble = ref("Let's solve this puzzle. What would you like to do first?")
    const editorDiv = ref<HTMLElement | null>(null)
    const editor = ref<EditorView | null>(null)
    const isSubmitting = ref(false)
    const showScorePopup = ref(false)
    const scoreDetails = ref<ScoreDetails | null>(null)

    const loadUserData = async () => {
      try {
        const savedUser = Cookies.get('user')
        if (!savedUser) {
          error.value = 'No user is logged in'
          loading.value = false
          return
        }

        try {
          const userData = JSON.parse(savedUser)

          if (!userData.email) {
            error.value = 'User data is incomplete'
            loading.value = false
            return
          }

          const response = await apiClient.get('/users/email', {
            params: { email: userData.email },
            timeout: 10000
          })

          if (!response || response.status !== 200) {
            throw new Error(`Server responded with status ${response?.status || 'unknown'}`)
          }

          if (!response.data) {
            throw new Error('No data received from server')
          }

          user.value = response.data
          loading.value = false

          // Initialize game if there's a game ID passed from the lobby
          initGameState()
        } catch (parseError) {
          console.error('Error parsing user cookie:', parseError)
          error.value = 'Invalid user data format'
          loading.value = false
        }
      } catch (error: any) {
        console.error('Error fetching user data:', error)
        error.value = `Failed to load user data: ${error.message || 'Unknown error'}`
        loading.value = false
      }
    }

    const initGameState = () => {
      // Check if we're given a gameId from the Lobby component
      const urlGameId = route.params.gameId as string
      if (urlGameId) {
        gameId.value = urlGameId
        // Initialize the game with data from query params if available
        const gameData = route.query
        if (gameData && Object.keys(gameData).length > 0) {
          initGameFromParams(gameData)
        }
      }

      // Initialize WebSocket connection
      initGameWebSocket()
    }

    const initGameFromParams = (params: any) => {
      inGame.value = true

      // Initialize from URL query parameters
      currentGame.value = {
        gameId: params.gameId as string,
        opponentId: parseInt(params.opponentId as string),
        opponentName: params.opponentName as string,
        opponentPicture: params.opponentPicture as string || defaultAvatar,
        opponentElo: parseInt(params.opponentElo as string) || 0,
        rounds: parseInt(params.rounds as string) || 3,
        currentRound: parseInt(params.currentRound as string) || 1,
        currentPuzzleId: parseInt(params.currentPuzzleId as string),
        timePerRound: parseInt(params.timePerRound as string) || 300,
        yourScore: parseInt(params.yourScore as string) || 0,
        opponentScore: parseInt(params.opponentScore as string) || 0
      }

      // Start the timer
      timeRemaining.value = currentGame.value.timePerRound
      startTimer()
    }

    const initGameWebSocket = () => {
      // First check if there's an existing socket instance from Lobby.vue
      if (window.gameSocketInstance && window.gameSocketInstance.readyState === WebSocket.OPEN) {
        console.log('Reusing existing WebSocket connection from Lobby')
        gameSocket.value = window.gameSocketInstance

        // Set up event handlers for the shared socket
        setupSocketEventHandlers()
        return
      }

      if (!user.value || !user.value.id) {
        console.error('Cannot initialize game WebSocket - user ID is missing')
        return
      }

      // Close previous socket if it exists
      if (gameSocket.value) {
        gameSocket.value.close()
      }

      const baseUrl = import.meta.env.VITE_API_BASE_URL.replace('http', 'ws')
      gameSocket.value = new WebSocket(`${baseUrl}/game`)

      gameSocket.value.onopen = () => {
        console.log('Game WebSocket connection established')
        // Store the socket instance globally for potential reuse
        if (gameSocket.value) {
          window.gameSocketInstance = gameSocket.value
        }
      }

      // Set up event handlers
      setupSocketEventHandlers()
    }

    const handleGameMessage = (message: any) => {
      console.log('Game message received:', message)

      // Only process messages meant for this user
      if (message.userId && message.userId !== user.value?.id) {
        console.log('Message not for this user, ignoring')
        return
      }

      switch (message.type) {
        case 'GAME_STARTED':
          handleGameStarted(message)
          break
        case 'PUZZLE_LOADED':
          handlePuzzleLoaded(message)
          break
        case 'SOLUTION_SUBMITTED':
          handleSolutionSubmitted(message)
          break
        case 'ROUND_COMPLETE':
          handleRoundComplete(message)
          break
        case 'GAME_OVER':
          handleGameOver(message)
          break
        case 'NO_OPPONENT':
          handleNoOpponent(message)
          break
        case 'ERROR':
          handleError(message)
          break
        default:
          console.log('Unknown message type:', message.type)
      }
    }

    // New function to handle puzzle loaded messages
    const handlePuzzleLoaded = (message: any) => {
      try {
        const puzzleData = JSON.parse(message.content)
        currentPuzzle.value = puzzleData
        showPuzzleButton.value = false

        // Initialize the editor with the puzzle code
        code.value = puzzleData.code || ''
        initializeEditor()

        showNotification('info', `Puzzle "${puzzleData.name}" loaded. Good luck!`)
      } catch (e) {
        console.error('Error handling puzzle loaded:', e)
      }
    }

    const handleNoOpponent = (message: any) => {
      showNotification('info', message.content || "No opponent found. Returning to lobby...")
      console.log("NO_OPPONENT message received, returning to lobby")

      // Stop any timers
      if (timerInterval.value) {
        clearInterval(timerInterval.value)
        timerInterval.value = null
      }

      // Reset game state
      inGame.value = false
      currentGame.value = null

      // Add a slight delay before redirecting back to lobby
      setTimeout(() => {
        router.push('/vs')
      }, 2000)
    }

    const handleGameStarted = (message: any) => {
      try {
        const gameData = JSON.parse(message.content)

        // Set up the new game
        inGame.value = true
        currentGame.value = {
          gameId: gameData.gameId,
          opponentId: gameData.opponentId,
          opponentName: gameData.opponentName,
          opponentPicture: gameData.opponentPicture || defaultAvatar,
          opponentElo: gameData.opponentElo,
          rounds: gameData.rounds,
          currentRound: gameData.currentRound,
          currentPuzzleId: gameData.currentPuzzleId,
          timePerRound: gameData.timePerRound,
          yourScore: 0,
          opponentScore: 0
        }

        // Start the countdown timer
        timeRemaining.value = gameData.timePerRound
        startTimer()

        // Show game start notification
        showNotification('success', `Game started! You're playing against ${gameData.opponentName}.`)
      } catch (e) {
        console.error('Error handling game start:', e)
      }
    }

    const handleSolutionSubmitted = (message: any) => {
      try {
        const resultData = JSON.parse(message.content)
        showNotification('info', resultData.message)
      } catch (e) {
        console.error('Error handling solution submission:', e)
      }
    }

    const handleRoundComplete = (message: any) => {
      try {
        const roundData = JSON.parse(message.content)

        if (currentGame.value) {
          // Update scores
          currentGame.value.yourScore = roundData.yourScore
          currentGame.value.opponentScore = roundData.opponentScore

          // Go to next round
          currentGame.value.currentRound = roundData.nextRound
          currentGame.value.currentPuzzleId = roundData.nextPuzzleId

          // Reset timer
          timeRemaining.value = currentGame.value.timePerRound

          // Reset puzzle state
          currentPuzzle.value = null
          showPuzzleButton.value = true

          showNotification('success', `Round ${roundData.currentRound} complete! Starting round ${roundData.nextRound}.`)
        }
      } catch (e) {
        console.error('Error handling round complete:', e)
      }
    }

    const handleGameOver = (message: any) => {
      try {
        const gameOverData = JSON.parse(message.content)

        // Stop the timer
        if (timerInterval.value) {
          clearInterval(timerInterval.value)
          timerInterval.value = null
        }

        // Set game results data
        gameResults.value = {
          gameId: gameOverData.gameId,
          result: gameOverData.result,
          yourScore: gameOverData.yourScore,
          opponentScore: gameOverData.opponentScore,
          eloChange: gameOverData.eloChange
        }

        // Show the game results modal
        showGameResults.value = true
        inGame.value = false

      } catch (e) {
        console.error('Error handling game over:', e)
      }
    }

    const handleError = (message: any) => {
      showNotification('error', message.content || 'An error occurred')
    }

    const startTimer = () => {
      if (timerInterval.value) {
        clearInterval(timerInterval.value)
        timerInterval.value = null
      }

      timerInterval.value = setInterval(() => {
        if (timeRemaining.value > 0) {
          timeRemaining.value--
        } else {
          // Time's up, auto-forfeit the round
          if (timerInterval.value) {
            clearInterval(timerInterval.value)
            timerInterval.value = null
          }

          submitSolution(true) // true indicates time expired
        }
      }, 1000)
    }

    const formatTime = (seconds: number): string => {
      const minutes = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${minutes}:${secs.toString().padStart(2, '0')}`
    }

    const showNotification = (type: string, message: string) => {
      gameNotification.value = { type, message }

      // Auto-hide notification after 5 seconds
      setTimeout(() => {
        if (gameNotification.value?.message === message) {
          gameNotification.value = null
        }
      }, 5000)
    }

    const forfeitGame = async () => {
      if (!currentGame.value?.gameId || !user.value?.id) return

      try {
        await apiClient.post('/game/forfeit', {
          gameId: currentGame.value.gameId,
          userId: user.value.id
        })

        // The response will come through the WebSocket
      } catch (e) {
        console.error('Error forfeiting game:', e)
        showNotification('error', 'Failed to forfeit game')
      }
    }

    const playAgain = () => {
      showGameResults.value = false
      router.push('/vs')
    }

    const returnToLobby = () => {
      showGameResults.value = false
      router.push('/vs')
    }

    // Puzzle solving functionality
    const loadPuzzle = async () => {
      if (!currentGame.value || !currentGame.value.currentPuzzleId) return

      try {
        const puzzleResponse = await apiClient.get(`/puzzles/${currentGame.value.currentPuzzleId}`)
        currentPuzzle.value = puzzleResponse.data
        showPuzzleButton.value = false

        // Initialize the editor with the puzzle code
        code.value = currentPuzzle.value?.code || ''
        initializeEditor()
      } catch (e) {
        console.error('Error loading puzzle:', e)
        showNotification('error', 'Failed to load puzzle')
      }
    }

    const initializeEditor = () => {
      // Wait for the next tick to ensure the DOM is updated
      setTimeout(() => {
        if (editorDiv.value && !editor.value) {
          editor.value = new EditorView({
            state: EditorState.create({
              doc: code.value,
              extensions: [
                lineNumbers(),
                highlightActiveLineGutter(),
                highlightSpecialChars(),
                history(),
                bracketMatching(),
                closeBrackets(),
                javascript(),
                syntaxHighlighting(defaultHighlightStyle),
                keymap.of(defaultKeymap),
                EditorView.lineWrapping,
                EditorView.updateListener.of((v) => {
                  if (v.docChanged) {
                    code.value = v.state.doc.toString();
                  }
                })
              ],
            }),
            parent: editorDiv.value
          })
        } else if (editor.value) {
          editor.value.dispatch({
            changes: { from: 0, to: editor.value.state.doc.length, insert: code.value }
          })
        }
      }, 100)
    }

    const handleSubmit = async () => {
      if (!currentPuzzle.value || !user.value || isSubmitting.value) return

      try {
        isSubmitting.value = true

        const response = await apiClient.post('/ai/solve', {
          puzzleId: currentPuzzle.value.id,
          userId: user.value.id,
          userInput: userInput.value,
          code: code.value,
        })

        textBubble.value = response.data.text || 'No response from AI'

        if (response.data.code) {
          code.value = response.data.code

          if (editor.value) {
            editor.value.dispatch({
              changes: { from: 0, to: editor.value.state.doc.length, insert: code.value }
            })
          }
        }

        userInput.value = ''
      } catch (error) {
        textBubble.value = 'An error occurred. Please try again.'
        console.error(error)
      } finally {
        isSubmitting.value = false
      }
    }

    const markCompleted = async () => {
      if (!currentPuzzle.value || !user.value || isSubmitting.value) return

      try {
        isSubmitting.value = true

        const response = await apiClient.post('/ai/complete', {
          puzzleId: currentPuzzle.value.id,
          userId: user.value.id
        })

        if (response.data.success) {
          textBubble.value = "Congratulations! You've completed this puzzle! 🎉"

          // Process score details
          scoreDetails.value = {
            totalScore: response.data.scoreDetails.totalScore || 0,
            timeScore: response.data.scoreDetails.timeScore || 0,
            efficiencyScore: response.data.scoreDetails.efficiencyScore || 0,
            tokenScore: response.data.scoreDetails.tokenScore || 0,
            correctnessScore: response.data.scoreDetails.correctnessScore || 0,
            codeQualityScore: response.data.scoreDetails.codeQualityScore || 0,
            timeSeconds: response.data.scoreDetails.timeSeconds || 0,
            interactionCount: response.data.scoreDetails.interactionCount || 0,
            hasFailed: response.data.scoreDetails.hasFailed || false
          }

          showScorePopup.value = true

          // Submit solution to the game server
          submitSolution(false)
        }
      } catch (error) {
        textBubble.value = 'An error occurred. Please try again.'
        console.error(error)
      } finally {
        isSubmitting.value = false
      }
    }

    const submitSolution = async (timeExpired = false) => {
      if (!currentGame.value?.gameId || !user.value?.id) return

      try {
        let score = 0
        if (scoreDetails.value && !scoreDetails.value.hasFailed) {
          score = scoreDetails.value.totalScore
        }

        await apiClient.post('/game/submit-solution', {
          gameId: currentGame.value.gameId,
          userId: user.value.id,
          puzzleId: currentGame.value.currentPuzzleId,
          roundNumber: currentGame.value.currentRound,
          score: score,
          timeExpired: timeExpired
        })

        // The response will come through the WebSocket
      } catch (e) {
        console.error('Error submitting solution:', e)
        showNotification('error', 'Failed to submit solution')
      }
    }

    const closeScorePopup = () => {
      showScorePopup.value = false
    }

    const goToPuzzle = () => {
      loadPuzzle()
    }

    onMounted(() => {
      loadUserData()

      // Check if we're coming from lobby with a preserved connection
      const preserveConnection = route.query.preserveConnection === 'true'

      // If this flag is true, we should look for an existing connection from parent component
      if (preserveConnection && window.gameSocketInstance) {
        console.log('Reusing existing WebSocket connection from Lobby')
        gameSocket.value = window.gameSocketInstance
        preserveSocket.value = true

        // Set up event handlers for the existing socket
        setupSocketEventHandlers()

        // Join the game with the existing socket
        joinGame()
      } else {
        // No preserved connection, create a new one
        initGameWebSocket()
      }
    })

    // Add a function to set up WebSocket event handlers
    const setupSocketEventHandlers = () => {
      if (!gameSocket.value) return

      gameSocket.value.onmessage = (event) => {
        const message = JSON.parse(event.data)
        console.log('Game WebSocket message received:', message)
        handleGameMessage(message)
      }

      gameSocket.value.onclose = () => {
        console.log('Game WebSocket connection closed')
        // Only attempt to reconnect if the component is still mounted
        if (!preserveSocket.value) {
          setTimeout(() => initGameWebSocket(), 5000)
        }
      }

      gameSocket.value.onerror = (error) => {
        console.error('Game WebSocket error:', error)
      }
    }

    // Function to join a game
    const joinGame = () => {
      if (!gameSocket.value || gameSocket.value.readyState !== WebSocket.OPEN || !user.value?.id) {
        console.error('Cannot join game - WebSocket not open or user ID missing')
        return
      }

      const gameIdFromUrl = route.query.gameId as string

      if (gameIdFromUrl) {
        const message = {
          type: 'JOIN_GAME',
          content: gameIdFromUrl,
          userId: user.value.id
        }

        gameSocket.value.send(JSON.stringify(message))
        console.log('JOIN_GAME message sent')
      }
    }

    onBeforeUnmount(() => {
      if (timerInterval.value) {
        clearInterval(timerInterval.value)
        timerInterval.value = null
      }

      // Only close the WebSocket if we're not preserving it
      if (gameSocket.value && !preserveSocket.value) {
        gameSocket.value.close()
        gameSocket.value = null

        // Only remove the global instance if we're not preserving it
        if (window.gameSocketInstance && !preserveSocket.value) {
          delete window.gameSocketInstance
        }
      }

      if (editor.value) {
        editor.value.destroy()
      }
    })

    return {
      user,
      inGame,
      currentGame,
      timeRemaining,
      gameNotification,
      showGameResults,
      gameResults,
      defaultAvatar,
      loading,
      error,
      formatTime,
      forfeitGame,
      playAgain,
      returnToLobby,
      goToPuzzle,

      // Puzzle solving related
      currentPuzzle,
      showPuzzleButton,
      userInput,
      code,
      textBubble,
      editorDiv, // Properly return the editorDiv ref
      isSubmitting,
      showScorePopup,
      scoreDetails,
      loadPuzzle,
      handleSubmit,
      markCompleted,
      closeScorePopup
    }
  }
}
</script>

<style scoped>
:deep(.cm-editor) {
  height: 100%;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
}

:deep(.cm-scroller) {
  height: 100%;
}

:deep(.cm-gutters) {
  background-color: #282c34;
  color: #6b727f;
  border-right: 1px solid #3e4451;
  padding: 0 5px 0 3px;
}

:deep(.cm-activeLineGutter) {
  background-color: #2c313c;
  color: #a0a8b7;
}

:deep(.cm-content) {
  background-color: #1f2329;
  color: #abb2bf;
}

:deep(.cm-line) {
  padding: 0 4px;
  line-height: 1.6;
}

:deep(.cm-activeLine) {
  background-color: rgba(54, 59, 69, 0.6);
}

:deep(.cm-selectionBackground) {
  background-color: rgba(83, 127, 231, 0.33) !important;
}

:deep(.cm-cursor) {
  border-left: 1.5px solid #528bff;
}

/* Syntax highlighting */
:deep(.cm-keyword) { color: #c678dd; }
:deep(.cm-operator) { color: #56b6c2; }
:deep(.cm-string) { color: #98c379; }
:deep(.cm-number) { color: #d19a66; }
:deep(.cm-comment) { color: #7f848e; font-style: italic; }
:deep(.cm-function) { color: #61afef; }
:deep(.cm-property) { color: #e06c75; }
:deep(.cm-variable) { color: #abb2bf; }
:deep(.cm-type) { color: #e5c07b; }

/* Animation for score popup */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in {
  animation: fadeIn 0.5s ease-out forwards;
}
</style>
