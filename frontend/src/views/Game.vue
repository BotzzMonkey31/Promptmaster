<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">
      <template v-if="gameState && inGame">
        <div class="bg-white p-6 shadow-lg rounded-lg mb-6">
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-semibold text-gray-800">Game in Progress</h3>
            <div class="text-sm">
              <span class="font-bold"
                >Round {{ gameState.currentRound }}/{{ gameState.totalRounds }}</span
              >
              <span class="mx-2">|</span>
              <span class="font-bold">Time: {{ formatTime(timeRemaining) }}</span>
            </div>
          </div>
          <div class="flex justify-between items-center mb-6">
            <div class="flex items-center">
              <img
                :src="currentPlayer?.picture || defaultAvatar"
                class="w-10 h-10 rounded-full mr-3 border-2 border-blue-500"
                alt="Your avatar"
              />
              <div>
                <p class="font-semibold text-gray-800">{{ currentPlayer?.username }}</p>
                <p class="text-sm text-gray-600">
                  Score: {{ gameState.playerStatus[currentPlayer?.id || '']?.score || 0 }}
                </p>
              </div>
            </div>

            <div class="text-xl font-bold text-gray-600">VS</div>

            <div class="flex items-center">
              <div class="text-right mr-3">
                <p class="font-semibold text-gray-800">{{ getOpponentName() }}</p>
                <p class="text-sm text-gray-600">Score: {{ getOpponentScore() }}</p>
              </div>
              <img
                :src="getOpponentAvatar()"
                class="w-10 h-10 rounded-full border-2 border-red-500"
                alt="Opponent avatar"
              />
            </div>
          </div>

          <div class="mt-6">
            <div
              class="flex flex-col items-stretch p-6 bg-light-50 rounded-xl shadow-lg mx-auto gap-6"
            >
              <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 shadow-sm">
                <div class="flex items-center justify-between mb-2">
                  <h3 class="text-lg font-semibold text-gray-700">
                    {{ gameState.currentPuzzle?.name || 'Loading puzzle...' }}
                  </h3>
                  <span
                    class="px-3 py-1 text-sm font-medium rounded-full"
                    :class="{
                      'bg-black text-white': gameState.currentPuzzle?.type === 'BY_PASS',
                      'bg-red-800 text-white': gameState.currentPuzzle?.type === 'FAULTY',
                      'bg-blue-400 text-white': gameState.currentPuzzle?.type === 'MULTI_STEP',
                      'bg-gray-100 text-gray-800': !gameState.currentPuzzle?.type,
                    }"
                  >
                    {{ gameState.currentPuzzle?.type || 'UNKNOWN' }}
                  </span>
                </div>
                <div class="space-y-4">
                  <p class="text-gray-600">
                    {{ gameState.currentPuzzle?.description || 'Please wait...' }}
                  </p>
                </div>
              </div>

              <div v-if="textBubble" class="flex items-start w-full gap-4">
                <img
                  :src="defaultAvatar"
                  class="w-10 h-10 rounded-full border-2 border-blue-500 shadow"
                  alt="AI Avatar"
                />
                <div class="flex-1 p-4 rounded-lg bg-white text-gray-800 shadow relative">
                  <div
                    class="absolute left-[-10px] top-4 border-y-[10px] border-r-[10px] border-l-0 border-y-transparent border-r-white"
                  ></div>
                  {{ textBubble }}
                </div>
              </div>

              <div class="flex flex-col gap-3">
                <textarea
                  v-model="promptInput"
                  placeholder="Describe what you want to do..."
                  class="w-full min-h-24 max-h-40 p-4 border border-gray-200 rounded-lg text-sm text-gray-800 bg-white shadow-sm focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-colors resize-y"
                ></textarea>
                <div class="flex justify-between w-full">
                  <button
                    @click="handlePrompt"
                    :disabled="isPrompting"
                    class="px-6 py-2.5 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-70 disabled:cursor-not-allowed flex items-center"
                  >
                    <span v-if="!isPrompting">Generate Code</span>
                    <span v-else class="flex items-center">
                      <svg
                        class="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                      >
                        <circle
                          class="opacity-25"
                          cx="12"
                          cy="12"
                          r="10"
                          stroke="currentColor"
                          stroke-width="4"
                        ></circle>
                        <path
                          class="opacity-75"
                          fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                        ></path>
                      </svg>
                      Generating...
                    </span>
                  </button>
                  <button
                    @click="handleSubmit"
                    :disabled="isSubmitting"
                    class="px-6 py-2.5 bg-green-500 text-white rounded-lg hover:bg-green-600 disabled:opacity-70 disabled:cursor-not-allowed"
                  >
                    Submit Solution
                  </button>
                </div>
              </div>

              <div
                id="editor-container"
                class="w-full h-[400px] rounded-lg overflow-hidden shadow bg-[#282c34]"
                ref="editorContainer"
              ></div>
            </div>
          </div>
        </div>
      </template>

      <div v-if="!inGame && !showGameResults" class="text-center mt-8">
        <p class="mb-4 text-gray-600">Waiting for a game to start...</p>
        <button
          @click="$router.push('/vs')"
          class="bg-blue-500 text-white px-6 py-3 rounded-lg shadow hover:bg-blue-600 transition"
        >
          Return to Lobby
        </button>
      </div>

      <div
        v-if="showGameResults && gameResults"
        class="fixed inset-0 flex items-center justify-center z-50"
      >
        <div class="absolute inset-0 bg-black bg-opacity-50"></div>
        <div class="bg-white p-8 rounded-lg shadow-lg z-10 w-full max-w-md">
          <h3 class="text-2xl font-bold mb-6 text-center">
            {{
              gameResults.result === 'WIN'
                ? '🏆 Victory!'
                : gameResults.result === 'LOSS'
                  ? '❌ Defeat'
                  : '🤝 Draw'
            }}
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
              <span v-if="gameResults.eloChange > 0" class="text-green-600"
                >+{{ gameResults.eloChange }} ELO</span
              >
              <span v-else-if="gameResults.eloChange < 0" class="text-red-600"
                >{{ gameResults.eloChange }} ELO</span
              >
              <span v-else class="text-gray-600">±0 ELO</span>
            </p>
          </div>

          <div class="flex justify-between space-x-4">
            <button
              @click="playAgain"
              class="w-1/2 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600"
            >
              Play Again
            </button>
            <button
              @click="returnToLobby"
              class="w-1/2 bg-gray-500 text-white py-2 rounded-lg hover:bg-gray-600"
            >
              Return to Lobby
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="showScorePopup && scoreDetails"
        class="fixed inset-0 flex items-center justify-center z-50"
      >
        <div class="absolute inset-0 bg-black bg-opacity-70"></div>
        <div class="bg-white p-8 rounded-lg shadow-xl max-w-md w-full animate-fade-in">
          <h2 class="text-3xl font-bold text-center mb-2">
            {{ scoreDetails.hasFailed ? 'Puzzle Failed!' : 'Puzzle Complete!' }}
          </h2>
          <p class="text-gray-600 text-center mb-6">Here's how you did:</p>

          <div
            class="bg-gradient-to-r rounded-lg p-6 mb-6 text-center"
            :class="
              scoreDetails.hasFailed ? 'from-red-500 to-red-700' : 'from-blue-500 to-purple-600'
            "
          >
            <h3 class="text-white text-lg font-medium mb-1">Your Score</h3>
            <div class="text-5xl font-bold text-white">{{ scoreDetails.totalScore || 0 }}</div>
            <div v-if="scoreDetails.hasFailed" class="text-white mt-2 text-sm">
              Scores below 40 in any category result in failure
            </div>
          </div>

          <div class="space-y-3">
            <div
              class="flex justify-between items-center"
              :class="{ 'bg-red-50 p-2 rounded-md': scoreDetails.timeScore < 40 }"
            >
              <span class="text-gray-700">Time ({{ formatTime(scoreDetails.timeSeconds) }})</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{ 'text-red-600': scoreDetails.timeScore < 40 }">{{
                  scoreDetails.timeScore || 0
                }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
              </div>
            </div>

            <div
              class="flex justify-between items-center"
              :class="{ 'bg-red-50 p-2 rounded-md': scoreDetails.efficiencyScore < 40 }"
            >
              <span class="text-gray-700">Efficiency</span>
              <div class="flex items-center">
                <span
                  class="font-bold"
                  :class="{ 'text-red-600': scoreDetails.efficiencyScore < 40 }"
                  >{{ scoreDetails.efficiencyScore || 0 }}</span
                >
                <span class="text-gray-500 text-sm ml-1">/100</span>
              </div>
            </div>

            <div
              class="flex justify-between items-center"
              :class="{ 'bg-red-50 p-2 rounded-md': scoreDetails.tokenScore < 40 }"
            >
              <span class="text-gray-700">Token Usage</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{ 'text-red-600': scoreDetails.tokenScore < 40 }">{{
                  scoreDetails.tokenScore || 0
                }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
              </div>
            </div>
          </div>

          <button
            @click="closeScorePopup"
            class="w-full mt-6 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600"
          >
            Continue
          </button>
        </div>
      </div>

      <div v-if="showWaitingPopup" class="fixed inset-0 flex items-center justify-center z-50">
        <div class="absolute inset-0 bg-black bg-opacity-50"></div>
        <div class="bg-white p-8 rounded-lg shadow-lg z-10 w-full max-w-md text-center">
          <h3 class="text-2xl font-bold mb-4">Round Complete!</h3>
          <div class="mb-6">
            <div
              class="animate-spin inline-block w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mb-4"
            ></div>
            <p class="text-gray-600">Waiting for other player to complete the round...</p>
          </div>
          <div class="text-sm text-gray-500">Your score for this round: {{ lastRoundScore }}</div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GameView',
})
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useGameStore } from '../stores/game'
import type { Player } from '../types/game'
import {
  EditorView,
  lineNumbers,
  highlightActiveLineGutter,
  highlightSpecialChars,
} from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { javascript } from '@codemirror/lang-javascript'
import { keymap } from '@codemirror/view'
import { defaultKeymap } from '@codemirror/commands'
import { syntaxHighlighting, defaultHighlightStyle } from '@codemirror/language'
import { bracketMatching } from '@codemirror/language'
import { closeBrackets } from '@codemirror/autocomplete'
import { history } from '@codemirror/commands'

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()

const { gameState, currentPlayer } = storeToRefs(gameStore)
const { markPuzzleCompleted, cleanup } = gameStore

const defaultAvatar = '/assets/BypassMascot.png'
const editorContainer = ref<HTMLElement | null>(null)
const editor = ref<EditorView | null>(null)
const code = ref('')
const textBubble = ref('What would you like to do first?')
const isSubmitting = ref(false)
const showPuzzleButton = ref(false)
const inGame = ref(false)
const timeRemaining = ref(300)
const gameNotification = ref('')
const showGameResults = ref(false)
const showScorePopup = ref(false)
const promptInput = ref('')
const isPrompting = ref(false)
const showWaitingPopup = ref(false)
const lastRoundScore = ref(0)

const gameResults = ref<{
  result: 'WIN' | 'LOSS' | 'DRAW'
  yourScore: number
  opponentScore: number
  eloChange: number
} | null>(null)

const scoreDetails = ref<{
  totalScore: number
  timeScore: number
  efficiencyScore: number
  tokenScore: number
  timeSeconds: number
  hasFailed: boolean
} | null>(null)

let timerInterval: number | undefined

const getOpponentName = () => {
  if (!gameState.value || !currentPlayer.value) return ''
  const opponent = gameState.value.players.find((p) => p.id !== currentPlayer.value?.id)
  return opponent?.username || 'Opponent'
}

const getOpponentScore = () => {
  if (!gameState.value || !currentPlayer.value) return 0
  const opponent = gameState.value.players.find((p) => p.id !== currentPlayer.value?.id)
  return opponent ? gameState.value.playerStatus[opponent.id]?.score || 0 : 0
}

const getOpponentAvatar = () => {
  if (!gameState.value || !currentPlayer.value) return defaultAvatar
  const opponent = gameState.value.players.find((p) => p.id !== currentPlayer.value?.id)
  return opponent?.picture || defaultAvatar
}

const startTimer = () => {
  if (timerInterval) clearInterval(timerInterval)
  timerInterval = window.setInterval(() => {
    if (timeRemaining.value > 0) {
      timeRemaining.value--
    } else {
      clearInterval(timerInterval)
      handleTimeout()
    }
  }, 1000)
}

const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

const handleTimeout = () => {
  markPuzzleCompleted()
  showScorePopup.value = true
}

const handleSubmit = async () => {
  if (
    isSubmitting.value ||
    !editor.value ||
    !gameState.value?.currentPuzzle ||
    !currentPlayer.value
  )
    return
  isSubmitting.value = true

  try {
    textBubble.value = 'Submitting solution...'
    await gameStore.submitSolution(code.value)
  } catch (error) {
    console.error('Error submitting solution:', error)
    textBubble.value = 'Failed to submit solution. Please try again.'
    gameNotification.value = 'Failed to submit solution. Please try again.'
  } finally {
    isSubmitting.value = false
  }
}

const loadPuzzle = () => {
  console.log('📝 LOAD PUZZLE: Called with game state:', gameStore.gameState)
  if (!gameStore.gameState?.currentPuzzle) {
    console.error('No puzzle found in game state')
    textBubble.value = 'Waiting for puzzle to load...'
    return
  }

  console.log('📝 LOAD PUZZLE: Loading puzzle:', gameStore.gameState.currentPuzzle)
  console.log('📝 LOAD PUZZLE: Current game state:', {
    currentRound: gameStore.gameState.currentRound,
    totalRounds: gameStore.gameState.totalRounds,
    players: gameStore.gameState.players,
    currentTurn: gameStore.gameState.currentTurn,
  })

  showPuzzleButton.value = false
  startTimer()
  const editorInit = initEditorWithRetries(true)

  // Check if editor initialization was successful
  if (!editorInit.isInitialized()) {
    console.error('📝 LOAD PUZZLE: Failed to initialize editor')
    return
  }
}

const playAgain = () => {
  showGameResults.value = false
  gameResults.value = null

  router.push('/vs')
}

const returnToLobby = () => {
  router.push('/vs')
}

const closeScorePopup = async () => {
  showScorePopup.value = false
  await gameStore.markPuzzleCompleted()
  showWaitingPopup.value = true
  lastRoundScore.value = scoreDetails.value?.totalScore || 0
}

const handlePrompt = async () => {
  if (
    isPrompting.value ||
    !gameState.value?.currentPuzzle ||
    !currentPlayer.value ||
    !promptInput.value
  )
    return
  isPrompting.value = true
  textBubble.value = 'Thinking...'

  try {
    await gameStore.sendPrompt(promptInput.value)
    promptInput.value = '' // Clear the input after successful send
  } catch (error) {
    console.error('Error generating code:', error)
    textBubble.value =
      error instanceof Error ? `Error: ${error.message}` : 'Failed to generate code'
  } finally {
    isPrompting.value = false
  }
}

watch(
  () => gameState.value?.currentTurn,
  (newTurn) => {
    if (gameState.value && editor.value && newTurn) {
      const playerStatus = gameState.value.playerStatus[newTurn]
      const currentCode = playerStatus?.code || ''
      if (currentCode !== code.value) {
        editor.value.dispatch({
          changes: { from: 0, to: editor.value.state.doc.length, insert: currentCode },
        })
        code.value = currentCode
      }
    }
  },
)

watch(
  () => gameStore.aiResponse,
  (newResponse) => {
    console.log('AI Response watcher triggered, received:', newResponse)

    if (newResponse) {
      if (newResponse.text) {
        console.log('Updating text bubble with:', newResponse.text)
        textBubble.value = newResponse.text
      }
      if (editor.value) {
        let codeToUpdate = null

        if (typeof newResponse.code === 'string' && newResponse.code.trim().length > 0) {
          console.log('Found code as string in response')
          codeToUpdate = newResponse.code
        } else if (newResponse.code && typeof newResponse.code === 'object') {
          console.log('Response code is an object, trying to extract code property')
          if (newResponse.code.code && typeof newResponse.code.code === 'string') {
            codeToUpdate = newResponse.code.code
          }
        }

        if (!codeToUpdate && newResponse.completeCode) {
          console.log('Using completeCode property')
          codeToUpdate = newResponse.completeCode
        }

        if (codeToUpdate) {
          console.log('Updating editor with code, length:', codeToUpdate.length)
          console.log('Editor before update - doc length:', editor.value.state.doc.length)
          console.log('First 50 chars of code:', codeToUpdate.substring(0, 50))

          try {
            setTimeout(() => {
              if (editor.value) {
                editor.value.dispatch({
                  changes: { from: 0, to: editor.value.state.doc.length, insert: codeToUpdate },
                })
                code.value = codeToUpdate
                console.log('Editor updated successfully')
              }
            }, 50)
          } catch (err) {
            console.error('Error updating editor:', err)
          }
        } else {
          console.log('No code to update in the response')
        }
      } else {
        console.log('Editor not initialized yet')
      }
    }
  },
  { immediate: true },
)

const initEditorWithRetries = (forceInit = false) => {
  let retryCount = 0
  const maxRetries = 10
  const initialDelay = 200
  let delay = initialDelay
  let editorInitialized = false

  console.log('📝 EDITOR: Starting initialization with up to', maxRetries, 'retries')

  const tryInit = () => {
    retryCount++
    console.log('📝 EDITOR: Attempt', retryCount, 'to initialize editor')

    if (editor.value && !forceInit) {
      console.log('📝 EDITOR: Editor already exists, skipping initialization')
      editorInitialized = true
      return
    }

    if (!editorContainer.value) {
      console.log('📝 EDITOR: Container ref not available yet, using querySelector')

      const containerElement = document.getElementById('editor-container')
      if (containerElement) {
        console.log('📝 EDITOR: Found container using getElementById')
        editorContainer.value = containerElement as HTMLElement
      } else {
        console.log('📝 EDITOR: Container not found via querySelector either')

        if (retryCount < maxRetries) {
          delay = Math.min(delay * 1.5, 2000)
          console.log('📝 EDITOR: Will retry in', delay, 'ms')
          setTimeout(tryInit, delay)
        } else {
          console.error(
            '📝 EDITOR ERROR: Failed to find editor container after',
            maxRetries,
            'attempts',
          )
        }
        return
      }
    }

    try {
      console.log('📝 EDITOR: Creating editor instance using container ref')

      if (editor.value) {
        try {
          console.log('📝 EDITOR: Destroying previous editor instance')
          editor.value.destroy()
        } catch (err) {
          console.warn('📝 EDITOR: Error cleaning up previous editor:', err)
        }
      }

      const extensions = [
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
            code.value = v.state.doc.toString()
            if (gameState.value && currentPlayer.value) {
              gameStore.updateCurrentCode(currentPlayer.value.id, code.value)
            }
          }
        }),
      ]

      const currentCode = code.value || ''

      editor.value = new EditorView({
        state: EditorState.create({
          doc: currentCode,
          extensions,
        }),
        parent: editorContainer.value,
      })

      console.log('📝 EDITOR: Editor created successfully')
      editorInitialized = true

      if (gameState.value?.currentPuzzle?.starterCode && code.value === '') {
        console.log('📝 EDITOR: Setting starter code')
        const starterCode = gameState.value.currentPuzzle.starterCode || ''
        code.value = starterCode

        editor.value.dispatch({
          changes: { from: 0, to: editor.value.state.doc.length, insert: starterCode },
        })
      }
    } catch (err) {
      console.error('📝 EDITOR: Error creating editor:', err)
      if (retryCount < maxRetries) {
        delay = Math.min(delay * 1.5, 2000)
        console.log('📝 EDITOR: Will retry after error in', delay, 'ms')
        setTimeout(tryInit, delay)
      } else {
        console.error('📝 EDITOR ERROR: Failed to initialize editor after', maxRetries, 'attempts')
        textBubble.value = 'Failed to initialize code editor. Please refresh the page.'
      }
    }
  }

  tryInit()

  return {
    isInitialized: () => editorInitialized,
  }
}

const handleScoreUpdate = (scoreData: {
  score: number
  timeBonus?: number
  qualityScore?: number
  correctnessScore?: number
}) => {
  console.log('🎯 SCORE UPDATE: Received score update:', scoreData)

  if (scoreData.score === 0) {
    console.log('🎯 SCORE UPDATE: Skipping score popup for zero score')
    return
  }

  scoreDetails.value = {
    totalScore: scoreData.score,
    timeScore: scoreData.timeBonus || 85,
    efficiencyScore: scoreData.qualityScore || 80,
    tokenScore: scoreData.correctnessScore || 70,
    timeSeconds: 300 - timeRemaining.value,
    hasFailed: false,
  }

  console.log('🎯 SCORE UPDATE: Created score details:', scoreDetails.value)

  setTimeout(() => {
    showScorePopup.value = true
    console.log('🎯 SCORE UPDATE: Score popup displayed')
  }, 300)
}

const unsubscribeActionRef = ref<(() => void) | null>(null)

const intervalIds = ref<{ [key: string]: number }>({})

onMounted(async () => {
  const gameId = route.params.gameId as string
  const player: Player = {
    id: route.query.playerId as string,
    username: route.query.username as string,
    picture: route.query.picture as string,
  }

  if (!gameId || !player.id || !player.username) {
    console.error('Missing required game parameters')
    router.push('/vs')
    return
  }

  console.log('Initializing game with ID:', gameId)
  console.log('Player info:', player)
  gameStore.initializeGame(gameId, player)

  // Watch for game state changes
  watch(
    () => gameStore.gameState,
    (newState) => {
      console.log('Game state changed:', newState)
      if (newState?.currentPuzzle) {
        console.log('Game state received with puzzle:', newState.currentPuzzle)
        inGame.value = true
        loadPuzzle()
      } else {
        console.log('Game state received without puzzle')
      }
    },
    { immediate: true },
  )
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
  if (editor.value) editor.value.destroy()

  if (unsubscribeActionRef.value) {
    unsubscribeActionRef.value()
  }

  Object.values(intervalIds.value).forEach((id) => {
    window.clearInterval(id)
  })

  cleanup()
})

watch(
  () => gameState.value?.scores,
  (newScores, oldScores) => {
    if (!newScores || !currentPlayer.value) return

    const playerScore = newScores[currentPlayer.value.id]
    const prevScore = oldScores?.[currentPlayer.value.id] || 0

    console.log(`🔢 SCORE WATCHER: Checking scores - previous: ${prevScore}, new: ${playerScore}`)

    if (playerScore > prevScore && playerScore > 0 && oldScores !== undefined) {
      console.log(`🔢 SCORE WATCHER: Score increased from ${prevScore} to ${playerScore}`)

      if (!showScorePopup.value) {
        const scoreData = {
          score: playerScore - prevScore, // Only show the difference
          timeBonus: Math.max(20, 100 - Math.floor((300 - timeRemaining.value) / 3)),
          qualityScore: 85,
          correctnessScore: 90,
        }

        console.log('🔢 SCORE WATCHER: Triggering score update from watcher')
        handleScoreUpdate(scoreData)
      }
    }
  },
  { deep: true },
)

watch(
  () => gameState.value?.playerStatus,
  (newStatus, oldStatus) => {
    if (!newStatus || !currentPlayer.value || !oldStatus) return

    const playerStatus = newStatus[currentPlayer.value.id]
    const oldPlayerStatus = oldStatus[currentPlayer.value.id]

    if (
      playerStatus &&
      oldPlayerStatus &&
      playerStatus.score > oldPlayerStatus.score &&
      playerStatus.score > 0
    ) {
      const newScore = playerStatus.score
      const oldScore = oldPlayerStatus.score

      console.log(`🔢 PLAYER STATUS WATCHER: Score increased from ${oldScore} to ${newScore}`)

      if (!showScorePopup.value) {
        const scoreData = {
          score: newScore - oldScore, // Only show the difference
          timeBonus: Math.max(20, 100 - Math.floor((300 - timeRemaining.value) / 3)),
          qualityScore: 85,
          correctnessScore: 90,
        }

        console.log('🔢 PLAYER STATUS WATCHER: Triggering score update from status watcher')
        handleScoreUpdate(scoreData)
      }
    }
  },
  { deep: true },
)

watch(
  () => gameState.value?.currentRound,
  (newRound, oldRound) => {
    if (!newRound || !oldRound) return

    timeRemaining.value = 300
    textBubble.value = 'What would you like to do for this round?'
    code.value = ''
    showWaitingPopup.value = false

    if (editor.value) {
      editor.value.dispatch({
        changes: { from: 0, to: editor.value.state.doc.length, insert: '' },
      })
    }

    gameNotification.value = `Round ${oldRound} completed! Starting round ${newRound}...`
    setTimeout(() => {
      if (gameNotification.value.includes(`Round ${oldRound} completed`)) {
        gameNotification.value = ''
      }
    }, 5000)
  },
)

watch(
  () => gameState.value?.state,
  (newState) => {
    if (newState === 'ENDED') {
      showScorePopup.value = false
      showWaitingPopup.value = false

      if (timerInterval) {
        clearInterval(timerInterval)
        timerInterval = undefined
      }

      if (gameState.value && currentPlayer.value) {
        const playerId = currentPlayer.value.id
        const playerScore = gameState.value.playerStatus[playerId]?.score || 0

        const opponent = gameState.value.players.find((p) => p.id !== playerId)
        const opponentScore = opponent ? gameState.value.playerStatus[opponent.id]?.score || 0 : 0

        let result: 'WIN' | 'LOSS' | 'DRAW' = 'DRAW'
        if (playerScore > opponentScore) {
          result = 'WIN'
        } else if (playerScore < opponentScore) {
          result = 'LOSS'
        }

        gameResults.value = {
          result,
          yourScore: playerScore,
          opponentScore,
          eloChange: result === 'WIN' ? 25 : result === 'LOSS' ? -15 : 0,
        }

        showGameResults.value = true
      }
    }
  },
)
</script>

<style scoped>
:deep(.cm-editor) {
  height: 100% !important;
  width: 100% !important;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  background-color: #282c34 !important;
  color: #abb2bf !important;
  position: relative;
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
  background-color: #1f2329 !important;
  color: #abb2bf !important;
  width: 100% !important;
  position: relative;
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
:deep(.cm-keyword) {
  color: #c678dd;
}
:deep(.cm-operator) {
  color: #56b6c2;
}
:deep(.cm-string) {
  color: #98c379;
}
:deep(.cm-number) {
  color: #d19a66;
}
:deep(.cm-comment) {
  color: #7f848e;
  font-style: italic;
}
:deep(.cm-function) {
  color: #61afef;
}
:deep(.cm-property) {
  color: #e06c75;
}
:deep(.cm-variable) {
  color: #abb2bf;
}
:deep(.cm-type) {
  color: #e5c07b;
}

/* Scrollbar styling */
:deep(.cm-scroller::-webkit-scrollbar) {
  width: 10px;
  height: 10px;
}

:deep(.cm-scroller::-webkit-scrollbar-track) {
  background: #282c34;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb) {
  background: #4a4d57;
  border-radius: 5px;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb:hover) {
  background: #5a5e6a;
}

/* Animation */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fadeIn 0.5s ease-out forwards;
}
</style>
