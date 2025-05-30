<template>
  <div
    class="flex flex-col items-stretch p-6 bg-light-50 rounded-xl shadow-lg max-w-900px mx-auto my-5 gap-6"
  >
    <div class="flex justify-center mb-2">
      <div class="bg-white px-6 py-3 rounded-lg shadow text-3xl font-mono border border-gray-200">
        {{ formatTimeDisplay(currentTimer) }}
      </div>
    </div>

    <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 shadow-sm">
      <div class="flex justify-between items-center">
        <h3 class="text-lg font-semibold text-gray-700">Your Progress</h3>
        <button
          @click="markCompleted"
          v-if="!metrics.isCompleted"
          class="px-4 py-1 bg-green-500 text-white rounded-lg text-sm"
        >
          Mark as Complete
        </button>
        <span v-else class="bg-green-100 text-green-800 px-3 py-1 rounded-lg text-sm"
          >Completed</span
        >
      </div>
      <div class="grid grid-cols-3 gap-4 mt-3">
        <div class="bg-white p-3 rounded-lg shadow-sm border border-gray-100">
          <span class="block text-sm text-gray-500">Attempts</span>
          <span class="block text-xl font-bold">{{ metrics.attemptCount || 1 }}</span>
        </div>
        <div class="bg-white p-3 rounded-lg shadow-sm border border-gray-100">
          <span class="block text-sm text-gray-500">Best Interactions</span>
          <span class="block text-xl font-bold">{{ metrics.bestInteractionCount || '-' }}</span>
        </div>
        <div class="bg-white p-3 rounded-lg shadow-sm border border-gray-100">
          <span class="block text-sm text-gray-500">Best Time</span>
          <span class="block text-xl font-bold">{{
            formatTime(metrics.bestTimeSeconds) || '-'
          }}</span>
        </div>
      </div>
    </div>

    <div class="flex items-start w-full gap-4">
      <img
        class="w-15 h-15 rounded-full border-2 border-blue-500 shadow"
        alt="Avatar"
        src="/assets/MultiMascot.png"
      />
      <div class="flex-1 p-4 rounded-3 bg-white text-15px text-gray-800 shadow relative">
        <div
          class="absolute left--2.5 top-5 border-y-10px border-r-10px border-l-0 border-y-transparent border-r-white"
        ></div>
        {{ textBubble }}
      </div>
    </div>

    <div class="flex w-full gap-6">
      <div class="flex-1 p-4 rounded-3 bg-white text-15px text-gray-800 shadow">
        <h2 class="font-bold text-lg mb-2">{{ puzzle.name }}</h2>
        <div>{{ puzzle.description }}</div>
      </div>
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
              Processing...
            </span>
          </button>
          <button
            @click="confirmReset"
            :disabled="isSubmitting"
            class="px-6 py-2.5 bg-red-500 text-white border-none rounded-2 cursor-pointer text-14px font-500 transition-colors shadow hover:bg-red-600 active:translate-y-0.25 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            Start Over
          </button>
        </div>
      </div>
    </div>

    <div class="w-full h-75 rounded-2 overflow-hidden shadow" ref="editorDiv"></div>

    <div
      v-if="showResetConfirm"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    >
      <div class="bg-white p-6 rounded-lg shadow-lg max-w-md w-full">
        <h3 class="text-xl font-bold mb-4">Confirm Reset</h3>
        <p class="mb-6 text-gray-700">
          Are you sure you want to reset your progress? This will count as a new attempt.
          <span v-if="metrics.attemptCount > 1" class="block mt-2 font-semibold">
            Your best results will be preserved.
          </span>
        </p>
        <div class="flex justify-end gap-4">
          <button
            @click="showResetConfirm = false"
            class="px-4 py-2 bg-gray-300 text-gray-800 rounded hover:bg-gray-400"
          >
            Cancel
          </button>
          <button
            @click="resetSession"
            class="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
          >
            Reset
          </button>
        </div>
      </div>
    </div>

    <!-- New Score Popup -->
    <div
      v-if="showScorePopup"
      class="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-50"
    >
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

        <div class="relative">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-xl font-semibold">Score Breakdown</h3>
            <div class="relative">
              <button
                class="w-6 h-6 bg-gray-200 text-gray-700 rounded-full flex items-center justify-center font-bold hover:bg-gray-300"
                @mouseenter="showScoreHelp = true"
                @mouseleave="showScoreHelp = false"
              >
                ?
              </button>
              <div
                v-if="showScoreHelp"
                class="absolute right-0 bottom-full mb-2 p-4 bg-white rounded-lg shadow-lg w-72 z-10 text-left text-sm"
              >
                <h4 class="font-bold mb-1">How Scoring Works</h4>
                <p class="mb-2">Scores are calculated based on:</p>
                <ul class="list-disc pl-5 space-y-1">
                  <li>
                    <span class="font-semibold">Time:</span> Faster completions earn more points
                  </li>
                  <li>
                    <span class="font-semibold">Efficiency:</span> Fewer interactions get higher
                    scores
                  </li>
                  <li>
                    <span class="font-semibold">Token Usage:</span> More efficient token usage is
                    rewarded
                  </li>
                  <li>
                    <span class="font-semibold">Code Correctness:</span> How well your solution
                    meets requirements
                  </li>
                  <li>
                    <span class="font-semibold">Code Quality:</span> Code structure, readability and
                    best practices
                  </li>
                  <li>
                    <span class="font-semibold text-red-600">IMPORTANT:</span> Any score below 40 is
                    considered a failing attempt
                  </li>
                </ul>
                <div
                  class="w-3 h-3 absolute -bottom-1.5 right-2 bg-white transform rotate-45"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <div class="space-y-3 mb-6">
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
              <span v-if="scoreDetails.timeScore < 40" class="text-red-500 ml-2 text-sm"
                >Failed</span
              >
            </div>
          </div>

          <div
            class="flex justify-between items-center"
            :class="{ 'bg-red-50 p-2 rounded-md': scoreDetails.efficiencyScore < 40 }"
          >
            <span class="text-gray-700"
              >Efficiency ({{ scoreDetails.interactionCount || 0 }} interactions)</span
            >
            <div class="flex items-center">
              <span
                class="font-bold"
                :class="{ 'text-red-600': scoreDetails.efficiencyScore < 40 }"
                >{{ scoreDetails.efficiencyScore || 0 }}</span
              >
              <span class="text-gray-500 text-sm ml-1">/100</span>
              <span v-if="scoreDetails.efficiencyScore < 40" class="text-red-500 ml-2 text-sm"
                >Failed</span
              >
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
              <span v-if="scoreDetails.tokenScore < 40" class="text-red-500 ml-2 text-sm"
                >Failed</span
              >
            </div>
          </div>

          <div
            class="flex justify-between items-center"
            :class="{ 'bg-red-50 p-2 rounded-md': scoreDetails.correctnessScore < 40 }"
          >
            <span class="text-gray-700">Code Correctness</span>
            <div class="flex items-center">
              <span
                class="font-bold"
                :class="{ 'text-red-600': scoreDetails.correctnessScore < 40 }"
                >{{ scoreDetails.correctnessScore || 0 }}</span
              >
              <span class="text-gray-500 text-sm ml-1">/100</span>
              <span v-if="scoreDetails.correctnessScore < 40" class="text-red-500 ml-2 text-sm"
                >Failed</span
              >
            </div>
          </div>

          <div
            class="flex justify-between items-center"
            :class="{ 'bg-red-50 p-2 rounded-md': scoreDetails.codeQualityScore < 40 }"
          >
            <span class="text-gray-700">Code Quality</span>
            <div class="flex items-center">
              <span
                class="font-bold"
                :class="{ 'text-red-600': scoreDetails.codeQualityScore < 40 }"
                >{{ scoreDetails.codeQualityScore || 0 }}</span
              >
              <span class="text-gray-500 text-sm ml-1">/100</span>
              <span v-if="scoreDetails.codeQualityScore < 40" class="text-red-500 ml-2 text-sm"
                >Failed</span
              >
            </div>
          </div>
        </div>

        <div class="text-center text-gray-600 text-sm mb-4">
          <p>
            AI has analyzed your solution based on efficiency, code quality, correctness, time
            taken, and token usage.
          </p>
        </div>

        <div class="flex justify-center">
          <button
            @click="showScorePopup = false"
            class="px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-full font-medium hover:from-blue-600 hover:to-purple-700"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
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
import { ref, onMounted, watch, onUnmounted } from 'vue'
import apiClient from '../services/api'
import Cookies from 'js-cookie'

const props = defineProps<{ puzzle: any }>()
const userInput = ref('')
const code = ref(props.puzzle.code || '')
const editor = ref<EditorView | null>(null)
const editorDiv = ref(null)
const textBubble = ref("That's too much in one go, try to split up the task in different steps.")
const showResetConfirm = ref(false)
const isSubmitting = ref(false) // New state variable for loading state
// New state for score popup
const showScorePopup = ref(false)
const scoreDetails = ref({
  totalScore: 0,
  timeScore: 0,
  efficiencyScore: 0,
  tokenScore: 0,
  correctnessScore: 0,
  codeQualityScore: 0,
  timeSeconds: 0,
  interactionCount: 0,
  hasFailed: false,
})
const metrics = ref({
  attemptCount: 1,
  bestInteractionCount: null,
  bestTimeSeconds: 0,
  isCompleted: false,
  currentInteractionCount: 0,
})
const currentTimer = ref(0)
let timerInterval: number | null = null
const showScoreHelp = ref(false)

onMounted(async () => {
  if (editorDiv.value) {
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
              code.value = v.state.doc.toString()
            }
          }),
        ],
      }),
      parent: editorDiv.value,
    })
  }

  await fetchMetrics()

  startTimer()
})

watch(
  () => props.puzzle.code,
  (newCode) => {
    if (editor.value && newCode !== code.value) {
      editor.value.dispatch({
        changes: { from: 0, to: editor.value.state.doc.length, insert: newCode || '' },
      })
      code.value = newCode || ''
    }
  },
)

function formatTime(seconds: number | null): string {
  if (seconds === null) return '-'

  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60

  if (minutes === 0) {
    return `${remainingSeconds}s`
  }

  return `${minutes}m ${remainingSeconds}s`
}

function formatTimeDisplay(seconds: number): string {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function startTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
  }

  currentTimer.value = 0

  timerInterval = window.setInterval(() => {
    currentTimer.value++
  }, 1000)
}

async function fetchMetrics() {
  try {
    const userId = await getUserId()
    if (!userId) return

    const response = await apiClient.get(`/ai/metrics/${props.puzzle.id}/${userId}`)
    metrics.value = response.data
  } catch (error) {
    console.error('Error fetching metrics:', error)
  }
}

async function markCompleted() {
  try {
    const userId = await getUserId()
    if (!userId) return

    const response = await apiClient.post('/ai/complete', {
      puzzleId: props.puzzle.id,
      userId,
    })

    if (response.data.success) {
      console.log('Complete response:', response.data)

      metrics.value = response.data.data
      textBubble.value = "Congratulations! You've completed this puzzle! 🎉"

      const metricsData = response.data.data || {}
      console.log('Metrics data:', metricsData)

      scoreDetails.value = {
        totalScore: metricsData.totalScore || 0,
        timeScore: metricsData.timeScore || 0,
        efficiencyScore: metricsData.efficiencyScore || 0,
        tokenScore: metricsData.tokenScore || 0,
        correctnessScore: metricsData.correctnessScore || 0,
        codeQualityScore: metricsData.codeQualityScore || 0,
        timeSeconds: metricsData.timeSeconds || 0,
        interactionCount: metricsData.interactionCount || 0,
        hasFailed: metricsData.hasFailed || false,
      }

      await updateUserElo(userId, scoreDetails.value.totalScore)

      showScorePopup.value = true

      if (timerInterval) {
        clearInterval(timerInterval)
        timerInterval = null
      }
    }
  } catch (error) {
    console.error('Error marking puzzle as completed:', error)
  }
}

function confirmReset() {
  showResetConfirm.value = true
}

async function getUserId(): Promise<string | null> {
  let userId = null
  const userCookie = Cookies.get('user')
  console.log('Starting getUserId, user cookie exists:', !!userCookie)

  if (userCookie) {
    try {
      const userData = JSON.parse(userCookie)
      console.log('User data from cookie:', userData)

      // First, check if we already have this user's ID in localStorage
      const storedUserId = localStorage.getItem(`userId_${userData.email}`)
      if (storedUserId) {
        console.log('Using cached user ID from localStorage:', storedUserId)
        return storedUserId
      }

      // Log before API call for debugging
      console.log('Attempting to fetch user by email:', userData.email)

      try {
        console.log('API URL:', import.meta.env.VITE_API_BASE_URL)
        const response = await apiClient.get('/users/email', {
          params: { email: userData.email },
          timeout: 10000, // Increase timeout to 10s
        })

        console.log('API response:', response)

        // Check if we got a valid response
        if (response && response.data) {
          // Different ways the ID might be represented in the response
          const id =
            response.data.id ||
            response.data.userId ||
            (typeof response.data === 'number' ? response.data : null)

          if (id) {
            const idString = id.toString()
            console.log('Found user ID:', idString)
            localStorage.setItem(`userId_${userData.email}`, idString)
            return idString
          } else {
            console.error("Response didn't contain a user ID:", response.data)
          }
        }
      } catch (apiError) {
        console.error('API error fetching user ID:', apiError)

        // Try an alternative API endpoint if the first one failed
        try {
          console.log('Trying alternative endpoint /users/check/' + userData.email)
          const checkResponse = await apiClient.get(`/users/check/${userData.email}`)
          console.log('Check response:', checkResponse)

          if (checkResponse?.data?.userId) {
            const idString = checkResponse.data.userId.toString()
            console.log('Found user ID via check API:', idString)
            localStorage.setItem(`userId_${userData.email}`, idString)
            return idString
          }
        } catch (altError) {
          console.error('Alternative API also failed:', altError)
        }
      }
    } catch (e) {
      console.error('Error parsing user cookie:', e)
    }
  }

  // If we reach here, we couldn't get the user ID from API, so use guest ID as fallback
  userId = localStorage.getItem('guestId')
  if (!userId) {
    userId = Date.now().toString()
    localStorage.setItem('guestId', userId)
  }
  console.log('Using guest ID as fallback:', userId)

  return userId
}

// Add the function to update user ELO after getUserId function
async function updateUserElo(userId: string, score: number): Promise<void> {
  try {
    console.log(`Updating user ELO for user ${userId} with score ${score}`)

    const response = await apiClient.post('/users/update-elo', {
      userId: userId,
      scoreToAdd: score,
    })

    if (response.data.success) {
      console.log('ELO updated successfully:', response.data)
    } else {
      console.error('Failed to update ELO:', response.data.message)
    }
  } catch (error) {
    console.error('Error updating user ELO:', error)
  }
}

async function resetSession() {
  try {
    const userId = await getUserId()
    if (!userId) return

    showResetConfirm.value = false

    const response = await apiClient.post('/ai/reset', {
      puzzleId: props.puzzle.id,
      userId,
    })

    if (response.data.success) {
      code.value = ''
      if (editor.value) {
        editor.value.dispatch({
          changes: { from: 0, to: editor.value.state.doc.length, insert: '' },
        })
      }

      textBubble.value = 'Ready to start a fresh attempt! What would you like to do first?'

      startTimer()

      await fetchMetrics()
    } else {
      throw new Error(response.data.message || 'Failed to reset session')
    }
  } catch (error) {
    textBubble.value = 'Error resetting session. Please try again.'
    console.error(error)
  }
}

async function handleSubmit() {
  try {
    if (isSubmitting.value) return // Prevent multiple submissions
    isSubmitting.value = true // Enable loading state

    const userId = await getUserId()
    if (!userId) {
      isSubmitting.value = false
      return
    }

    const response = await apiClient.post('/ai/solve', {
      puzzleId: props.puzzle.id,
      userId,
      userInput: userInput.value,
      code: code.value,
    })

    textBubble.value = response.data.text

    if (response.data.code) {
      code.value = response.data.code
    } else if (response.data.completeCode) {
      code.value = response.data.CompleteCode
    }

    if (editor.value) {
      editor.value.dispatch({
        changes: { from: 0, to: editor.value.state.doc.length, insert: code.value },
      })
    }

    userInput.value = ''
    await fetchMetrics()
  } catch (error) {
    textBubble.value = 'An error occurred. Please try again.'
    console.error(error)
  } finally {
    isSubmitting.value = false // Disable loading state regardless of success/failure
  }
}

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval)
  }
})
</script>

<style scoped>
:deep(.cm-editor) {
  height: 100%;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  position: relative;
}

:deep(.cm-editor::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, transparent 45%, rgba(59, 130, 246, 0.1) 50%, transparent 55%);
  animation: step-progress 2s infinite;
  pointer-events: none;
  z-index: 2;
}

:deep(.cm-scroller) {
  height: 100%;
}

:deep(.cm-gutters) {
  background-color: #1e293b;
  color: #94a3b8;
  border-right: 2px solid #3b82f6;
  padding: 0 5px 0 3px;
}

:deep(.cm-activeLineGutter) {
  background-color: #1e40af;
  color: #e2e8f0;
}

:deep(.cm-content) {
  background-color: #0f172a;
  color: #e2e8f0;
}

:deep(.cm-line) {
  padding: 0 4px;
  line-height: 1.6;
}

:deep(.cm-activeLine) {
  background-color: rgba(59, 130, 246, 0.1);
}

:deep(.cm-selectionBackground) {
  background-color: rgba(59, 130, 246, 0.2) !important;
}

:deep(.cm-cursor) {
  border-left: 2px solid #3b82f6;
  animation: cursor-blink 1s infinite;
}

/* Syntax highlighting */
:deep(.cm-keyword) {
  color: #818cf8;
}
:deep(.cm-operator) {
  color: #60a5fa;
}
:deep(.cm-string) {
  color: #34d399;
}
:deep(.cm-number) {
  color: #fbbf24;
}
:deep(.cm-comment) {
  color: #64748b;
  font-style: italic;
}
:deep(.cm-function) {
  color: #60a5fa;
}
:deep(.cm-property) {
  color: #f87171;
}
:deep(.cm-variable) {
  color: #e2e8f0;
}
:deep(.cm-type) {
  color: #fbbf24;
}

/* Scrollbar styling */
:deep(.cm-scroller::-webkit-scrollbar) {
  width: 10px;
  height: 10px;
}

:deep(.cm-scroller::-webkit-scrollbar-track) {
  background: #1e293b;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb) {
  background: #3b82f6;
  border-radius: 5px;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb:hover) {
  background: #2563eb;
}

/* Progress indicators */
.progress-step {
  position: relative;
  padding-left: 2rem;
}

.progress-step::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #3b82f6;
  opacity: 0.3;
}

.progress-step.active::before {
  opacity: 1;
  animation: step-pulse 2s infinite;
}

@keyframes step-pulse {
  0% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.3;
  }
}

@keyframes step-progress {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

@keyframes cursor-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* Score popup styling */
.score-popup {
  background: linear-gradient(45deg, #1e293b, #0f172a);
  border: 2px solid #3b82f6;
  box-shadow: 0 0 30px rgba(59, 130, 246, 0.3);
}

.score-header {
  color: #3b82f6;
  text-shadow: 0 0 10px rgba(59, 130, 246, 0.5);
}

.score-value {
  color: #e2e8f0;
  text-shadow: 0 0 15px rgba(59, 130, 246, 0.7);
}

.score-category {
  border-bottom: 1px solid rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
}

.score-category:hover {
  background: rgba(59, 130, 246, 0.1);
  transform: translateX(5px);
}

/* Button styling */
button {
  position: relative;
  overflow: hidden;
}

button::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: button-shine 2s infinite;
}

@keyframes button-shine {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}
</style>
