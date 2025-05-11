<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">
      <h2 class="text-2xl font-semibold text-center mb-6">1v1 Game</h2>

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

        <p class="text-center mb-4">
          Solve the <span class="font-bold">Multi-Step Puzzle #{{ currentGame.currentPuzzleId }}</span>
        </p>

        <div class="text-center">
          <button
            @click="goToPuzzle"
            class="bg-blue-500 text-white px-6 py-3 rounded-lg shadow hover:bg-blue-600 transition"
          >
            Go to Puzzle
          </button>
          <button
            @click="forfeitGame"
            class="bg-red-500 text-white px-6 py-3 rounded-lg shadow hover:bg-red-600 transition ml-4"
          >
            Forfeit Game
          </button>
        </div>
      </div>

      <!-- Return to Lobby -->
      <div v-if="!inGame && !showGameResults" class="text-center mt-8">
        <p class="mb-4 text-gray-600">Waiting for a game to start...</p>
        <button
          @click="$router.push('/lobby')"
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
    </main>
  </div>
</template>

<script lang="ts">
import Cookies from 'js-cookie'
import apiClient from '../services/api'
import { useRoute, useRouter } from 'vue-router'
import { onMounted, onBeforeUnmount, ref, reactive } from 'vue'

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

export default {
  name: 'GameView',
  setup() {
    const route = useRoute()
    const router = useRouter()

    const gameSocket = ref<WebSocket | null>(null)
    const user = ref<User | null>(null)
    const gameId = ref<string | null>(null)
    const inGame = ref(false)
    const currentGame = ref<CurrentGame | null>(null)
    const timeRemaining = ref(0)
    const timerInterval = ref<number | null>(null)
    const gameNotification = ref<GameNotification | null>(null)
    const showGameResults = ref(false)
    const gameResults = ref<GameResults | null>(null)
    const defaultAvatar = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'
    const loading = ref(true)
    const error = ref<string | null>(null)

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
      }

      gameSocket.value.onmessage = (event) => {
        const message = JSON.parse(event.data)
        console.log('Game WebSocket message received:', message)

        handleGameMessage(message)
      }

      gameSocket.value.onclose = () => {
        console.log('Game WebSocket connection closed')
        // Try to reconnect after 5 seconds
        setTimeout(() => initGameWebSocket(), 5000)
      }

      gameSocket.value.onerror = (error) => {
        console.error('Game WebSocket error:', error)
      }
    }

    const handleGameMessage = (message: any) => {
      // Only process messages meant for this user
      if (message.userId !== user.value?.id) {
        return
      }

      switch (message.type) {
        case 'GAME_STARTED':
          handleGameStarted(message)
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
        case 'ERROR':
          handleError(message)
          break
      }
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
          // Update game state
          currentGame.value.currentRound = roundData.currentRound
          currentGame.value.currentPuzzleId = roundData.nextPuzzleId
          currentGame.value.yourScore = roundData.yourScore
          currentGame.value.opponentScore = roundData.opponentScore

          // Reset timer for next round
          timeRemaining.value = currentGame.value.timePerRound
          startTimer()

          // Show round complete notification
          const roundMessage = `
            <div>
              <p><strong>Round ${roundData.currentRound - 1} Complete!</strong></p>
              <p>Your Score: ${roundData.yourScore}</p>
              <p>Opponent's Score: ${roundData.opponentScore}</p>
              <p>Starting Round ${roundData.currentRound}...</p>
            </div>
          `
          showNotification('info', roundMessage)
        }
      } catch (e) {
        console.error('Error handling round complete:', e)
      }
    }

    const handleGameOver = (message: any) => {
      try {
        const gameOverData = JSON.parse(message.content)

        // Stop timer
        if (timerInterval.value) {
          clearInterval(timerInterval.value)
          timerInterval.value = null
        }

        inGame.value = false

        // Show game results
        gameResults.value = {
          gameId: gameOverData.gameId,
          result: gameOverData.result,
          yourScore: gameOverData.yourScore,
          opponentScore: gameOverData.opponentScore,
          eloChange: gameOverData.eloChange
        }

        showGameResults.value = true

        // Update user's ELO locally
        if (user.value) {
          user.value.elo += gameOverData.eloChange
        }
      } catch (e) {
        console.error('Error handling game over:', e)
      }
    }

    const handleError = (message: any) => {
      showNotification('error', message.content)
    }

    const forfeitGame = () => {
      if (!gameSocket.value || !user.value?.id || !currentGame.value) return

      const message = {
        type: 'GAME_ACTION',
        content: JSON.stringify({
          gameId: currentGame.value.gameId,
          action: 'FORFEIT'
        }),
        userId: user.value.id
      }

      gameSocket.value.send(JSON.stringify(message))
    }

    const submitSolution = (solution: string) => {
      if (!gameSocket.value || !user.value?.id || !currentGame.value) return

      const message = {
        type: 'SUBMIT_SOLUTION',
        content: JSON.stringify({
          gameId: currentGame.value.gameId,
          solution: solution
        }),
        userId: user.value.id
      }

      gameSocket.value.send(JSON.stringify(message))
    }

    const startTimer = () => {
      if (timerInterval.value) {
        clearInterval(timerInterval.value)
      }

      timerInterval.value = window.setInterval(() => {
        if (timeRemaining.value > 0) {
          timeRemaining.value -= 1
        } else {
          if (timerInterval.value) {
            clearInterval(timerInterval.value)
            timerInterval.value = null
          }

          // Auto-submit current solution if time runs out
          // You would need to implement this functionality to get the current solution
          submitSolution("Time ran out - automatic submission")
        }
      }, 1000)
    }

    const formatTime = (seconds: number) => {
      const minutes = Math.floor(seconds / 60)
      const remainingSeconds = seconds % 60
      return `${minutes}:${remainingSeconds < 10 ? '0' : ''}${remainingSeconds}`
    }

    const goToPuzzle = () => {
      if (currentGame.value) {
        // Navigate to the puzzle page with game information
        router.push({
          name: 'puzzle',
          params: { id: currentGame.value.currentPuzzleId.toString() },
          query: {
            gameId: currentGame.value.gameId,
            round: currentGame.value.currentRound.toString()
          }
        })
      }
    }

    const playAgain = () => {
      showGameResults.value = false
      gameResults.value = null
      router.push('/lobby')
    }

    const returnToLobby = () => {
      showGameResults.value = false
      gameResults.value = null
      router.push('/lobby')
    }

    const showNotification = (type: string, message: string) => {
      gameNotification.value = { type, message }

      // Auto-hide notifications after 5 seconds
      setTimeout(() => {
        if (gameNotification.value && gameNotification.value.message === message) {
          gameNotification.value = null
        }
      }, 5000)
    }

    onMounted(() => {
      loadUserData()
    })

    onBeforeUnmount(() => {
      if (gameSocket.value) {
        gameSocket.value.close()
      }

      if (timerInterval.value) {
        clearInterval(timerInterval.value)
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
      formatTime,
      goToPuzzle,
      forfeitGame,
      playAgain,
      returnToLobby
    }
  }
}
</script>
