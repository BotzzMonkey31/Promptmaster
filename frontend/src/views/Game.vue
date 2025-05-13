<!-- Game.vue -->
<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">

      <!-- Game Notification -->
      <div v-if="gameNotification" class="bg-blue-100 border border-blue-300 text-blue-800 px-4 py-3 rounded relative mb-6">
        <span v-html="gameNotification"></span>
      </div>

      <!-- Game Content -->
      <template v-if="gameState && inGame">
        <div class="bg-white p-6 shadow-lg rounded-lg mb-6">
        <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-semibold text-gray-800">Game in Progress</h3>
          <div class="text-sm">
              <span class="font-bold">Round {{ gameState.currentRound }}/{{ gameState.totalRounds }}</span>
            <span class="mx-2">|</span>
            <span class="font-bold">Time: {{ formatTime(timeRemaining) }}</span>
          </div>
        </div>

          <!-- Player Info -->
        <div class="flex justify-between items-center mb-6">
          <div class="flex items-center">
              <img :src="currentPlayer?.picture || defaultAvatar" class="w-10 h-10 rounded-full mr-3 border-2 border-blue-500" alt="Your avatar" />
            <div>
                <p class="font-semibold text-gray-800">{{ currentPlayer?.username }}</p>
                <p class="text-sm text-gray-600">Score: {{ gameState.playerStatus[currentPlayer?.id || '']?.score || 0 }}</p>
            </div>
          </div>

            <div class="text-xl font-bold text-gray-600">VS</div>

          <div class="flex items-center">
            <div class="text-right mr-3">
                <p class="font-semibold text-gray-800">{{ getOpponentName() }}</p>
                <p class="text-sm text-gray-600">Score: {{ getOpponentScore() }}</p>
            </div>
              <img :src="getOpponentAvatar()" class="w-10 h-10 rounded-full border-2 border-red-500" alt="Opponent avatar" />
          </div>
        </div>

          <!-- Puzzle Content -->
          <div v-if="gameState.puzzle && !showPuzzleButton" class="mt-6">
          <div class="flex flex-col items-stretch p-6 bg-light-50 rounded-xl shadow-lg mx-auto gap-6">
            <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 shadow-sm">
                <h3 class="text-lg font-semibold text-gray-700 mb-2">{{ gameState.puzzle.name }}</h3>
                <div class="space-y-4">
                  <p class="text-gray-600">{{ gameState.puzzle.description }}</p>
                </div>
            </div>

              <!-- AI Assistant Response -->
              <div v-if="textBubble" class="flex items-start w-full gap-4">
                <img :src="defaultAvatar" class="w-10 h-10 rounded-full border-2 border-blue-500 shadow" alt="AI Avatar" />
                <div class="flex-1 p-4 rounded-lg bg-white text-gray-800 shadow relative">
                  <div class="absolute left-[-10px] top-4 border-y-[10px] border-r-[10px] border-l-0 border-y-transparent border-r-white"></div>
                {{ textBubble }}
              </div>
            </div>

              <!-- Prompt Input -->
              <div class="flex flex-col gap-3">
                <textarea
                  v-model="promptInput"
                  placeholder="Describe what you want to do..."
                  class="w-full min-h-24 max-h-40 p-4 border border-gray-200 rounded-lg text-sm text-gray-800 bg-white shadow-sm focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-colors resize-y"
                ></textarea>
                <div class="flex justify-between w-full">
                  <button
                    @click="handlePrompt"
                    :disabled="isPrompting || !isPlayerTurn"
                    class="px-6 py-2.5 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-70 disabled:cursor-not-allowed flex items-center"
                  >
                    <span v-if="!isPrompting">Generate Code</span>
                    <span v-else class="flex items-center">
                      <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      Generating...
                    </span>
                  </button>
                  <button
                    @click="handleSubmit"
                    :disabled="isSubmitting || !isPlayerTurn"
                    class="px-6 py-2.5 bg-green-500 text-white rounded-lg hover:bg-green-600 disabled:opacity-70 disabled:cursor-not-allowed"
                  >
                    Submit Solution
                  </button>
              </div>
            </div>

              <!-- Code Editor -->
              <div class="w-full h-[400px] rounded-lg overflow-hidden shadow bg-[#282c34]" ref="editorContainer"></div>
          </div>
        </div>

          <!-- Start Puzzle Button -->
          <div v-else-if="showPuzzleButton" class="text-center mt-6">
            <button
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
      </template>

      <!-- Waiting State -->
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
            <button @click="playAgain" class="w-1/2 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600">
              Play Again
            </button>
            <button @click="returnToLobby" class="w-1/2 bg-gray-500 text-white py-2 rounded-lg hover:bg-gray-600">
              Return to Lobby
            </button>
          </div>
        </div>
      </div>

      <!-- Score Popup -->
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

          <div class="space-y-3">
            <div class="flex justify-between items-center" :class="{'bg-red-50 p-2 rounded-md': scoreDetails.timeScore < 40}">
              <span class="text-gray-700">Time ({{ formatTime(scoreDetails.timeSeconds) }})</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{'text-red-600': scoreDetails.timeScore < 40}">{{ scoreDetails.timeScore || 0 }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
              </div>
            </div>

            <div class="flex justify-between items-center" :class="{'bg-red-50 p-2 rounded-md': scoreDetails.efficiencyScore < 40}">
              <span class="text-gray-700">Efficiency</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{'text-red-600': scoreDetails.efficiencyScore < 40}">{{ scoreDetails.efficiencyScore || 0 }}</span>
                <span class="text-gray-500 text-sm ml-1">/100</span>
              </div>
            </div>

            <div class="flex justify-between items-center" :class="{'bg-red-50 p-2 rounded-md': scoreDetails.tokenScore < 40}">
              <span class="text-gray-700">Token Usage</span>
              <div class="flex items-center">
                <span class="font-bold" :class="{'text-red-600': scoreDetails.tokenScore < 40}">{{ scoreDetails.tokenScore || 0 }}</span>
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
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useGameStore } from '../stores/game';
import type { Player } from '../types/game';
import { EditorView, lineNumbers, highlightActiveLineGutter, highlightSpecialChars } from "@codemirror/view";
import { EditorState } from "@codemirror/state";
import { javascript } from "@codemirror/lang-javascript";
import { keymap } from "@codemirror/view";
import { defaultKeymap } from "@codemirror/commands";
import { syntaxHighlighting, defaultHighlightStyle } from "@codemirror/language";
import { bracketMatching } from "@codemirror/language";
import { closeBrackets } from "@codemirror/autocomplete";
import { history } from "@codemirror/commands";
import apiClient from '../services/api';

// Router and store setup
const route = useRoute();
const router = useRouter();
const gameStore = useGameStore();

// Store refs and methods
const { gameState, currentPlayer, isConnected, lastError } = storeToRefs(gameStore);
const { initializeGame, markPuzzleCompleted, isPlayerTurn, cleanup } = gameStore;

// Component state
const defaultAvatar = '/default-avatar.png';
const editorContainer = ref<HTMLElement | null>(null);
const editor = ref<EditorView | null>(null);
const code = ref('');
const textBubble = ref("What would you like to do first?");
const isSubmitting = ref(false);
const showPuzzleButton = ref(true);
const inGame = ref(false);
const timeRemaining = ref(300);
const gameNotification = ref('');
const showGameResults = ref(false);
const showScorePopup = ref(false);
const promptInput = ref('');
const isPrompting = ref(false);

// Game state
const gameResults = ref<{
  result: 'WIN' | 'LOSS' | 'DRAW';
  yourScore: number;
  opponentScore: number;
  eloChange: number;
} | null>(null);

const scoreDetails = ref<{
  totalScore: number;
  timeScore: number;
  efficiencyScore: number;
  tokenScore: number;
  timeSeconds: number;
  hasFailed: boolean;
} | null>(null);

// Timer
let timerInterval: number | undefined;

// Computed
const getOpponentName = () => {
  if (!gameState.value || !currentPlayer.value) return '';
  const opponent = gameState.value.players.find(p => p.id !== currentPlayer.value?.id);
  return opponent?.username || 'Opponent';
};

const getOpponentScore = () => {
  if (!gameState.value || !currentPlayer.value) return 0;
  const opponent = gameState.value.players.find(p => p.id !== currentPlayer.value?.id);
  return opponent ? gameState.value.playerStatus[opponent.id]?.score || 0 : 0;
};

const getOpponentAvatar = () => {
  if (!gameState.value || !currentPlayer.value) return defaultAvatar;
  const opponent = gameState.value.players.find(p => p.id !== currentPlayer.value?.id);
  return opponent?.picture || defaultAvatar;
};

// Methods
const startTimer = () => {
  if (timerInterval) clearInterval(timerInterval);
  timerInterval = window.setInterval(() => {
    if (timeRemaining.value > 0) {
      timeRemaining.value--;
    } else {
      clearInterval(timerInterval);
      handleTimeout();
    }
  }, 1000);
};

const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
};

const handleTimeout = () => {
  markPuzzleCompleted();
  showScorePopup.value = true;
};

const handleSubmit = async () => {
  console.log('Submit button clicked');
  console.log('Is submitting:', isSubmitting.value);
  console.log('Editor exists:', !!editor.value);
  console.log('Game state exists:', !!gameState.value);
  console.log('Current player exists:', !!currentPlayer.value);
  console.log('Code length:', code.value.length);

  if (isSubmitting.value || !gameState.value?.puzzle || !currentPlayer.value) {
    console.log('Cannot submit solution - missing game state or player');
    return;
  }

  // Don't require editor to exist, just use the code.value
  console.log('Submitting solution with code length:', code.value.length);
  isSubmitting.value = true;

  try {
    await gameStore.submitSolution(code.value);
    console.log('Solution submitted successfully to server');
    textBubble.value = 'Solution submitted for evaluation!';
  } catch (error) {
    console.error('Error submitting solution:', error);
    textBubble.value = 'Failed to submit solution. Please try again.';
  } finally {
    isSubmitting.value = false;
  }
};

const handleComplete = () => {
  markPuzzleCompleted();
};

const loadPuzzle = () => {
  console.log('loadPuzzle called');
  console.log('Editor container exists:', !!editorContainer.value);
  console.log('Current code length:', code.value.length);

  showPuzzleButton.value = false;
  startTimer();

  // Try to initialize the editor with a retry mechanism if container not available yet
  const initEditor = () => {
    console.log('initEditor called, checking container');

    if (!editor.value) {
      if (editorContainer.value) {
        console.log('Creating editor instance now');

        try {
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
                    if (gameState.value && currentPlayer.value) {
                      gameStore.updateCurrentCode(currentPlayer.value.id, code.value);
                    }
                  }
                })
              ],
            }),
            parent: editorContainer.value
          });

          console.log('Editor created successfully:', !!editor.value);

          // Force refresh the editor after creation to ensure styling is applied
          setTimeout(() => {
            if (editor.value) {
              console.log('Refreshing editor with dispatch');
              editor.value.dispatch({});
            }
          }, 100);
        } catch (error) {
          console.error('Error creating editor:', error);
        }
      } else {
        console.log('Container not available yet, retrying in 100ms');
        setTimeout(initEditor, 100);
        return;
      }
    }
  };

  // Start the editor initialization process
  initEditor();
};

const forfeitGame = async () => {
  try {
    await gameStore.forfeitGame();
    router.push('/vs');
  } catch (error) {
    console.error('Error forfeiting game:', error);
  }
};

const playAgain = () => {
  showGameResults.value = false;
  router.push('/vs');
};

const returnToLobby = () => {
  router.push('/vs');
};

const closeScorePopup = () => {
  showScorePopup.value = false;
};

// Handle prompt submission
const handlePrompt = async () => {
  if (isPrompting.value || !gameState.value?.puzzle || !currentPlayer.value) return;
  isPrompting.value = true;

  try {
    // First, send the prompt through WebSocket to notify other players
    await gameStore.submitPrompt(promptInput.value);

    // Then, get the AI response directly (like in MultiStepSolve)
    const response = await apiClient.post('/ai/solve', {
      puzzleId: gameState.value.puzzle.id,
      userId: currentPlayer.value.id,
      userInput: promptInput.value,
      code: code.value
    });

    // Update text bubble with AI response
    textBubble.value = response.data.text;

    // Update editor with new code
    let newCode = null;

    // Try to extract code from various possible response formats
    if (response.data.code) {
      console.log('Found code in response.data.code');
      newCode = typeof response.data.code === 'string'
        ? response.data.code
        : (response.data.code.code || null);
    }

    // Check for completeCode property like in MultiStepSolve
    if (!newCode && response.data.completeCode) {
      console.log('Using completeCode property');
      newCode = response.data.completeCode;
    }

    if (newCode) {
      console.log('Found valid code in response, length:', newCode.length);
      code.value = newCode; // Always update the code ref

      // If editor exists, update it
      if (editor.value) {
        console.log('Updating editor with new code');
        try {
          // Small delay to ensure editor is ready
          setTimeout(() => {
            if (editor.value) {
              editor.value.dispatch({
                changes: { from: 0, to: editor.value.state.doc.length, insert: newCode }
              });
              console.log('Editor updated successfully');
            }
          }, 50);
        } catch (error) {
          console.error('Error updating editor:', error);
        }
      } else {
        console.log('Editor not initialized yet, code.value has been updated');
      }

      // Update the game state with new code through WebSocket
      if (currentPlayer.value) {
        gameStore.updateCurrentCode(currentPlayer.value.id, newCode);
        console.log('Code updated in game state');
      }
    } else {
      console.log('No valid code found in the response');
      console.log('Response structure:', JSON.stringify(response.data).substring(0, 200) + '...');
    }

    // Clear prompt input
    promptInput.value = '';

  } catch (error) {
    console.error('Error generating code:', error);
    textBubble.value = 'Failed to generate code. Please try again.';
  } finally {
    isPrompting.value = false;
  }
};

// Watch for game state changes to update code
watch(() => gameState.value?.currentTurn, (newTurn) => {
  if (gameState.value && editor.value && newTurn) {
    const playerStatus = gameState.value.playerStatus[newTurn];
    const currentCode = playerStatus?.code || '';
    if (currentCode !== code.value) {
      editor.value.dispatch({
        changes: { from: 0, to: editor.value.state.doc.length, insert: currentCode }
      });
      code.value = currentCode;
    }
  }
});

// Watch for player status changes to detect puzzle completion
watch(() => gameState.value?.playerStatus, (newStatus, oldStatus) => {
  if (gameState.value && currentPlayer.value && newStatus) {
    const playerStatus = newStatus[currentPlayer.value.id];
    const oldPlayerStatus = oldStatus?.[currentPlayer.value.id];

    // Check if puzzle was just completed
    if (playerStatus?.hasCompleted && !oldPlayerStatus?.hasCompleted) {
      console.log('Puzzle completion detected in player status');
      // Show score popup or handle completion
      handleComplete();
    }
  }
}, { deep: true });

// Watch for AI responses from WebSocket (backup sync mechanism)
watch(() => gameStore.aiResponse, (newResponse) => {
  console.log('AI Response watcher triggered, received:', newResponse);

  if (newResponse) {
    // Only update text if we don't already have a response
    if (newResponse.text && textBubble.value === "What would you like to do first?") {
      console.log('Updating text bubble with:', newResponse.text);
      textBubble.value = newResponse.text;
    }

    // Extract code from response
    let codeToUpdate = null;

    if (typeof newResponse.code === 'string' && newResponse.code.trim().length > 0) {
      console.log('Found code as string in response');
      codeToUpdate = newResponse.code;
    } else if (newResponse.code && typeof newResponse.code === 'object') {
      // Handle case where code might be an object property
      console.log('Response code is an object, trying to extract code property');
      if (newResponse.code.code && typeof newResponse.code.code === 'string') {
        codeToUpdate = newResponse.code.code;
      }
    }

    // If we still don't have code, check for a completeCode property
    if (!codeToUpdate && newResponse.completeCode) {
      console.log('Using completeCode property');
      codeToUpdate = newResponse.completeCode;
    }

    // Update the editor with the new code
    if (codeToUpdate && editor.value) {
      console.log('Updating editor with code, length:', codeToUpdate.length);
      console.log('First 50 chars of code:', codeToUpdate.substring(0, 50));

      try {
        // Small delay to ensure editor is ready
        setTimeout(() => {
          if (editor.value) {
            editor.value.dispatch({
              changes: { from: 0, to: editor.value.state.doc.length, insert: codeToUpdate }
            });
            code.value = codeToUpdate;
            console.log('Editor updated successfully');
          }
        }, 50);
      } catch (error) {
        console.error('Error updating editor:', error);
      }
    } else if (codeToUpdate) {
      // Store code in code.value even if editor isn't ready
      console.log('Editor not initialized yet, storing in code.value');
      console.log('Stored code length:', codeToUpdate.length);
      code.value = codeToUpdate;
    } else {
      console.log('No valid code found in the response');
    }
  }
}, { immediate: true });

// Lifecycle
onMounted(() => {
  console.log('Game component mounted');

  const gameId = route.params.gameId as string;
  const player: Player = {
    id: route.query.playerId as string,
    username: route.query.username as string,
    picture: route.query.picture as string
  };

  if (!gameId || !player.id || !player.username) {
    console.error('Missing required game parameters');
    router.push('/vs');
    return;
  }

  console.log('Initializing game with ID:', gameId);
  initializeGame(gameId, player);

  console.log('Game initialized, setting inGame to true');
  inGame.value = true;

  // Use nextTick to ensure DOM is updated before checking references
  nextTick(() => {
    console.log('nextTick - Editor container ref:', editorContainer.value);
  });
});

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval);
  if (editor.value) editor.value.destroy();
  cleanup();
});
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
:deep(.cm-keyword) { color: #c678dd; }
:deep(.cm-operator) { color: #56b6c2; }
:deep(.cm-string) { color: #98c379; }
:deep(.cm-number) { color: #d19a66; }
:deep(.cm-comment) { color: #7f848e; font-style: italic; }
:deep(.cm-function) { color: #61afef; }
:deep(.cm-property) { color: #e06c75; }
:deep(.cm-variable) { color: #abb2bf; }
:deep(.cm-type) { color: #e5c07b; }

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
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in {
  animation: fadeIn 0.5s ease-out forwards;
}
</style>
