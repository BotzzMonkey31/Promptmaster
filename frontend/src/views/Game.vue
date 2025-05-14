<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">

      <!-- <div v-if="gameNotification" class="bg-blue-100 border border-blue-300 text-blue-800 px-4 py-3 rounded relative mb-6">
        <span v-html="gameNotification"></span>
      </div> -->

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

          <div class="mt-6">
          <div class="flex flex-col items-stretch p-6 bg-light-50 rounded-xl shadow-lg mx-auto gap-6">
            <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 shadow-sm">
                <h3 class="text-lg font-semibold text-gray-700 mb-2">{{ gameState.puzzle?.name || 'Loading puzzle...' }}</h3>
                <div class="space-y-4">
                  <p class="text-gray-600">{{ gameState.puzzle?.description || 'Please wait...' }}</p>
                </div>
            </div>

              <div v-if="textBubble" class="flex items-start w-full gap-4">
                <img :src="defaultAvatar" class="w-10 h-10 rounded-full border-2 border-blue-500 shadow" alt="AI Avatar" />
                <div class="flex-1 p-4 rounded-lg bg-white text-gray-800 shadow relative">
                  <div class="absolute left-[-10px] top-4 border-y-[10px] border-r-[10px] border-l-0 border-y-transparent border-r-white"></div>
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
                      <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
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

              <div id="editor-container" class="w-full h-[400px] rounded-lg overflow-hidden shadow bg-[#282c34]" ref="editorContainer"></div>
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
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
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

const route = useRoute();
const router = useRouter();
const gameStore = useGameStore();

const { gameState, currentPlayer, isConnected, lastError } = storeToRefs(gameStore);
const { initializeGame, markPuzzleCompleted, isPlayerTurn, cleanup, startNextRound } = gameStore;

const defaultAvatar = '/default-avatar.png';
const editorContainer = ref<HTMLElement | null>(null);
const editor = ref<EditorView | null>(null);
const code = ref('');
const textBubble = ref("What would you like to do first?");
const isSubmitting = ref(false);
const showPuzzleButton = ref(false);
const inGame = ref(false);
const timeRemaining = ref(300);
const gameNotification = ref('');
const showGameResults = ref(false);
const showScorePopup = ref(false);
const promptInput = ref('');
const isPrompting = ref(false);
const updateCodeTimeout = ref<number | null>(null);

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

let timerInterval: number | undefined;

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
  if (isSubmitting.value || !editor.value || !gameState.value?.puzzle || !currentPlayer.value) return;
  isSubmitting.value = true;

  try {
    textBubble.value = 'Submitting solution...';

    // Store solution locally to ensure we keep it even if connection drops
    const solutionCode = code.value;

    // Show explicit notification about submission
    gameNotification.value = 'Submitting solution, please wait...';

    // Check if connection is available
    if (!gameStore.isConnected) {
      console.log('Connection not available, attempting reconnection...');
      textBubble.value = 'Reconnecting to server...';
      gameNotification.value = 'Reconnecting to server before submission...';

      // Try explicit reconnection with our new method
      const reconnected = await gameStore.reconnectToGame();

      if (!reconnected) {
        throw new Error('Failed to reconnect to server');
      }

      console.log('Connection restored, proceeding with submission');
      textBubble.value = 'Connection restored, submitting solution...';
    }

    try {
      // Submit solution with a timeout in case it hangs
      const submitPromise = gameStore.submitSolution(solutionCode);
      const timeoutPromise = new Promise((_, reject) =>
        setTimeout(() => reject(new Error('Submission timeout')), 10000)
      );

      await Promise.race([submitPromise, timeoutPromise]);
      textBubble.value = 'Solution submitted for evaluation!';
      gameNotification.value = 'Solution submitted, waiting for results...';

      // Start a timeout for showing score popup if it doesn't show automatically
      setTimeout(async () => {
        if (isSubmitting.value) {
          console.log('Solution submitted but no score received yet, checking status...');

          // Try to reconnect if the connection was lost
          if (!gameStore.isConnected) {
            console.log('Connection lost after submission, attempting to reconnect...');
            await gameStore.reconnectToGame();
          }

          const playerStatus = gameState.value?.playerStatus[currentPlayer.value?.id || ''];

          if (playerStatus && playerStatus.score > 0) {
            console.log('Score found in playerStatus:', playerStatus.score);
            isSubmitting.value = false;

            // Show score popup
            const scoreData = {
              score: playerStatus.score,
              timeBonus: Math.max(20, 100 - Math.floor((300 - timeRemaining.value) / 3)),
              qualityScore: 85,
              correctnessScore: 90
            };

            handleScoreUpdate(scoreData);
          } else {
            // Try to submit the solution again as a fallback
            console.log('No score found after timeout, trying alternative submission...');
            textBubble.value = 'Trying alternative submission method...';

            try {
              // Check if fallback API is enabled
              const useFallbackApi = import.meta.env.VITE_USE_FALLBACK_API === 'true';

              if (useFallbackApi) {
                // Fallback to REST API call
                const response = await apiClient.post(`/api/game/${gameState.value?.id}/submit`, {
                  playerId: currentPlayer.value?.id,
                  code: solutionCode
                });

                console.log('Solution submitted via fallback API:', response.data);
                gameNotification.value = '';
                textBubble.value = 'Solution submitted via fallback method!';

                // If we get a score in the response, show it
                if (response.data && response.data.score) {
                  const scoreData = {
                    score: response.data.score,
                    timeBonus: response.data.timeBonus || 80,
                    qualityScore: response.data.qualityScore || 85,
                    correctnessScore: response.data.correctnessScore || 90
                  };

                  handleScoreUpdate(scoreData);
                }
              } else {
                console.log('Fallback API disabled, skipping REST API call');
                textBubble.value = 'Waiting for score update...';
              }
            } catch (apiError) {
              console.error('Fallback API submission also failed:', apiError);
              gameNotification.value = 'Having trouble submitting your solution. Please try again.';
              isSubmitting.value = false;
            }
          }
        }
      }, 5000);

      // Log submission
      console.log('Solution submitted successfully, waiting for score update');
    } catch (error: any) {
      console.error('Error during solution submission:', error);

      // If we get a timeout or connection error, try a direct API call as backup
      if (error.message && (error.message.includes('timeout') || error.message.includes('connection'))) {
        textBubble.value = 'WebSocket timeout, trying alternative submission...';
        gameNotification.value = 'WebSocket timeout, trying alternative submission...';

        try {
          // Check if fallback API is enabled
          const useFallbackApi = import.meta.env.VITE_USE_FALLBACK_API === 'true';

          if (useFallbackApi) {
            // Fallback to REST API call
            const response = await apiClient.post(`/api/game/${gameState.value.id}/submit`, {
              playerId: currentPlayer.value?.id,
              code: solutionCode
            });

            console.log('Solution submitted via fallback API:', response.data);
            gameNotification.value = '';
            textBubble.value = 'Solution submitted via fallback method!';

            // If we get a score in the response, show it
            if (response.data && response.data.score) {
              const scoreData = {
                score: response.data.score,
                timeBonus: response.data.timeBonus || 80,
                qualityScore: response.data.qualityScore || 85,
                correctnessScore: response.data.correctnessScore || 90
              };

              handleScoreUpdate(scoreData);
            }
          } else {
            console.log('Fallback API disabled, skipping REST API call');
            textBubble.value = 'Waiting for score update from WebSocket...';
          }
        } catch (apiError) {
          console.error('Fallback API submission also failed:', apiError);
          gameNotification.value = 'Having trouble submitting your solution. Please try again.';
          isSubmitting.value = false;
          throw new Error('Failed to submit solution through all available methods');
        }
      } else {
        throw error;
      }
    }
  } catch (error) {
    console.error('Error submitting solution:', error);
    textBubble.value = 'Failed to submit solution. Please try again.';
    gameNotification.value = 'Failed to submit solution. Please try again.';
    isSubmitting.value = false;
  }
};

const handleComplete = () => {
  markPuzzleCompleted();
};

const loadPuzzle = () => {
  console.log('📝 LOAD PUZZLE: Called with current code length:', code.value?.length || 0);
  showPuzzleButton.value = false;
  startTimer();

  // Try to initialize editor if not already done
  initEditorWithRetries(true);
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

const closeScorePopup = async () => {
  console.log('🎯 SCORE POPUP: Closing score popup');
  showScorePopup.value = false;

  // After closing, mark the puzzle as completed
  console.log('🎯 SCORE POPUP: Marking puzzle as completed');
  await markPuzzleCompleted();

  // Check if we need to advance to next round
  if (gameState.value && currentPlayer.value) {
    const currentRound = gameState.value.currentRound;
    const totalRounds = gameState.value.totalRounds;

    if (currentRound < totalRounds) {
      // After a delay to ensure completion is processed, start next round
      setTimeout(async () => {
        console.log('🎯 SCORE POPUP: Attempting to start next round...');
        try {
          // Reset UI for new round
          timeRemaining.value = 300;
          textBubble.value = "What would you like to do for this round?";
          code.value = '';

          // Clear the editor
          if (editor.value) {
            editor.value.dispatch({
              changes: { from: 0, to: editor.value.state.doc.length, insert: '' }
            });
          }

          // Make sure any existing timer is cleared before starting a new one
          if (timerInterval) {
            console.log('🎯 SCORE POPUP: Clearing existing timer before starting a new round');
            clearInterval(timerInterval);
            timerInterval = undefined;
          }

          // Request next round from server
          await startNextRound();
          console.log('🎯 SCORE POPUP: Next round request successful');

          gameNotification.value = `Round ${currentRound} completed! Starting round ${currentRound + 1}...`;
          setTimeout(() => {
            if (gameNotification.value.includes(`Round ${currentRound} completed`)) {
              gameNotification.value = '';
            }
          }, 5000);

          // Wait a moment for the round change to propagate before starting the timer
          setTimeout(() => {
            // Start timer for the new round
            console.log('🎯 SCORE POPUP: Starting timer for new round after delay');
            startTimer();
          }, 500);
        } catch (error) {
          console.error('🎯 SCORE POPUP: Error starting next round:', error);
          gameNotification.value = 'Failed to start next round. Try refreshing the page.';
        }
      }, 1000);
    } else {
      console.log('🎯 SCORE POPUP: Final round completed');
    }
  }
};

const handlePrompt = async () => {
  if (isPrompting.value || !gameState.value?.puzzle || !currentPlayer.value) return;
  isPrompting.value = true;
  textBubble.value = 'Thinking...';

  // Safety timeout to ensure isPrompting doesn't get stuck
  const promptingTimeout = setTimeout(() => {
    if (isPrompting.value) {
      console.log('Prompting timeout triggered - resetting isPrompting state');
      isPrompting.value = false;
      textBubble.value = 'Request timed out. Please try again.';
    }
  }, 60000); // 60 second max timeout for generating

  try {
    // Use the improved submitPrompt method that returns a Promise
    const aiResponse = await gameStore.submitPrompt(promptInput.value) as {
      text?: string;
      code?: string | { code?: string };
      completeCode?: string;
    };
    console.log('AI response received:', aiResponse);

    if (aiResponse) {
      if (aiResponse.text) {
        textBubble.value = aiResponse.text;
      }

      let newCode = null;

      if (typeof aiResponse.code === 'string' && aiResponse.code.trim().length > 0) {
        console.log('Found code as string in response');
        newCode = aiResponse.code;
      } else if (aiResponse.code && typeof aiResponse.code === 'object') {
        console.log('Response code is an object, trying to extract code property');
        if (aiResponse.code.code && typeof aiResponse.code.code === 'string') {
          newCode = aiResponse.code.code;
        }
      }

      if (!newCode && aiResponse.completeCode) {
        console.log('Using completeCode property');
        newCode = aiResponse.completeCode;
      }

      if (editor.value && newCode) {
        console.log('Updating editor with new code, length:', newCode.length);
        console.log('First 50 chars:', newCode.substring(0, 50));

        try {
          setTimeout(() => {
            if (editor.value) {
              editor.value.dispatch({
                changes: { from: 0, to: editor.value.state.doc.length, insert: newCode }
              });
              code.value = newCode;

              if (currentPlayer.value) {
                gameStore.updateCurrentCode(currentPlayer.value.id, newCode);
                console.log('Code updated in game state');
              }
            }
          }, 50);
        } catch (error) {
          console.error('Error updating editor:', error);
          textBubble.value = 'Error applying code changes. Please try again.';
        }
      } else {
        console.log('Not updating editor. Editor exists:', !!editor.value, 'Code exists:', !!newCode);
        if (!newCode) {
          textBubble.value = 'The AI did not generate any code. Please try a more specific prompt.';
        }
      }
    } else {
      console.error('No valid AI response received');
      textBubble.value = 'Failed to get a response. Please try again with a more specific prompt.';
    }

    promptInput.value = '';

  } catch (error) {
    console.error('Error generating code:', error);
    textBubble.value = error instanceof Error ? `Error: ${error.message}` : 'Failed to generate code. Please try again.';
  } finally {
    // Clear the safety timeout since we're done
    clearTimeout(promptingTimeout);
    isPrompting.value = false;
  }
};

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

watch(() => gameStore.aiResponse, (newResponse) => {
  console.log('AI Response watcher triggered, received:', newResponse);

  if (newResponse) {
    if (newResponse.text && textBubble.value === "What would you like to do first?") {
      console.log('Updating text bubble with:', newResponse.text);
      textBubble.value = newResponse.text;
    }

    if (editor.value) {
      let codeToUpdate = null;

      if (typeof newResponse.code === 'string' && newResponse.code.trim().length > 0) {
        console.log('Found code as string in response');
        codeToUpdate = newResponse.code;
      } else if (newResponse.code && typeof newResponse.code === 'object') {
        console.log('Response code is an object, trying to extract code property');
        if (newResponse.code.code && typeof newResponse.code.code === 'string') {
          codeToUpdate = newResponse.code.code;
        }
      }

      if (!codeToUpdate && newResponse.completeCode) {
        console.log('Using completeCode property');
        codeToUpdate = newResponse.completeCode;
      }

      if (codeToUpdate) {
        console.log('Updating editor with code, length:', codeToUpdate.length);
        console.log('Editor before update - doc length:', editor.value.state.doc.length);
        console.log('First 50 chars of code:', codeToUpdate.substring(0, 50));

        try {
          setTimeout(() => {
            if (editor.value) {
              editor.value.dispatch({
                changes: { from: 0, to: editor.value.state.doc.length, insert: codeToUpdate }
              });
              code.value = codeToUpdate;
              console.log('Editor updated successfully');
            }
          }, 50);
        } catch (err) {
          console.error('Error updating editor:', err);
        }
      } else {
        console.log('No valid code found in the response to update the editor');
        console.log('Response structure:', JSON.stringify(newResponse).substring(0, 200) + '...');
      }
    } else {
      console.log('Editor not initialized yet');
    }
  }
}, { immediate: true });

const initEditorWithRetries = (forceInit = false) => {
  let retryCount = 0;
  const maxRetries = 10;
  const initialDelay = 200;
  let delay = initialDelay;
  let editorInitialized = false;

  console.log('📝 EDITOR: Starting initialization with up to', maxRetries, 'retries');

  const tryInit = () => {
    retryCount++;
    console.log('📝 EDITOR: Attempt', retryCount, 'to initialize editor');

    // First check if editor already exists
    if (editor.value && !forceInit) {
      console.log('📝 EDITOR: Editor already exists, skipping initialization');
      editorInitialized = true;
      return;
    }

    if (!editorContainer.value) {
      console.log('📝 EDITOR: Container ref not available yet, using querySelector');

      // Try using querySelector as a fallback
      const containerElement = document.getElementById('editor-container');
      if (containerElement) {
        console.log('📝 EDITOR: Found container using getElementById');
        editorContainer.value = containerElement as HTMLElement;
      } else {
        console.log('📝 EDITOR: Container not found via querySelector either');

        if (retryCount < maxRetries) {
          delay = Math.min(delay * 1.5, 2000);
          console.log('📝 EDITOR: Will retry in', delay, 'ms');
          setTimeout(tryInit, delay);
        } else {
          console.error('📝 EDITOR ERROR: Failed to find editor container after', maxRetries, 'attempts');
        }
        return;
      }
    }

    try {
      console.log('📝 EDITOR: Creating editor instance using container ref');

      if (editor.value) {
        try {
          console.log('📝 EDITOR: Destroying previous editor instance');
          editor.value.destroy();
        } catch (err) {
          console.warn('📝 EDITOR: Error cleaning up previous editor:', err);
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
            code.value = v.state.doc.toString();
            if (gameState.value && currentPlayer.value) {
              gameStore.updateCurrentCode(currentPlayer.value.id, code.value);
            }
          }
        })
      ];

      const currentCode = code.value || '';

      editor.value = new EditorView({
        state: EditorState.create({
          doc: currentCode,
          extensions,
        }),
        parent: editorContainer.value
      });

      console.log('📝 EDITOR: Editor created successfully');
      editorInitialized = true;

      if (gameState.value?.puzzle?.starterCode && code.value === '') {
        console.log('📝 EDITOR: Setting starter code');
        const starterCode = gameState.value.puzzle.starterCode || '';
        code.value = starterCode;

        editor.value.dispatch({
          changes: { from: 0, to: editor.value.state.doc.length, insert: starterCode }
        });
      }
    } catch (err) {
      console.error('📝 EDITOR: Error creating editor:', err);
      if (retryCount < maxRetries) {
        delay = Math.min(delay * 1.5, 2000);
        console.log('📝 EDITOR: Will retry after error in', delay, 'ms');
        setTimeout(tryInit, delay);
      } else {
        console.error('📝 EDITOR ERROR: Failed to initialize editor after', maxRetries, 'attempts');
        textBubble.value = "Failed to initialize code editor. Please refresh the page.";
      }
    }
  };

  tryInit();

  return {
    isInitialized: () => editorInitialized
  };
};

// Add score popup handling function
const handleScoreUpdate = (scoreData: {
  score: number;
  timeBonus?: number;
  qualityScore?: number;
  correctnessScore?: number;
}) => {
  console.log('🎯 SCORE UPDATE: Received score update:', scoreData);

  // Skip score popup for zero scores
  if (scoreData.score === 0) {
    console.log('🎯 SCORE UPDATE: Skipping score popup for zero score');
    return;
  }

  // Create score details to show in the popup
  scoreDetails.value = {
    totalScore: scoreData.score,
    timeScore: scoreData.timeBonus || 85,
    efficiencyScore: scoreData.qualityScore || 80,
    tokenScore: scoreData.correctnessScore || 70,
    timeSeconds: 300 - timeRemaining.value,
    hasFailed: false
  };

  console.log('🎯 SCORE UPDATE: Created score details:', scoreDetails.value);

  // Show the score popup with small delay to ensure UI is ready
  setTimeout(() => {
    showScorePopup.value = true;
    console.log('🎯 SCORE UPDATE: Score popup displayed');
  }, 300);
};

// Create a ref to store the unsubscribe function
const unsubscribeActionRef = ref<Function | null>(null);

// Create a ref to store the interval IDs
const intervalIds = ref<{[key: string]: number}>({});

onMounted(() => {
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

  initializeGame(gameId, player);

  // Register score update handler by subscribing to the store action
  const unsubscribeAction = gameStore.$onAction(({ name, args }) => {
    if (name === 'handleScoreUpdate') {
      console.log('🎯 SCORE EVENT: Score update action detected:', args[0]);
      handleScoreUpdate(args[0]);
    }
  });

  // Store in a ref to be cleaned up in the onUnmounted hook
  unsubscribeActionRef.value = unsubscribeAction;

  // Initial editor setup attempt
  initEditorWithRetries();

  // Add a manual score check interval as a fallback
  const scoreCheckInterval = window.setInterval(() => {
    if (gameState.value && currentPlayer.value && !showScorePopup.value && isSubmitting.value) {
      const playerStatus = gameState.value.playerStatus[currentPlayer.value.id];
      if (playerStatus && playerStatus.score > 0) {
        console.log('🔢 SCORE INTERVAL: Detected score in interval check:', playerStatus.score);
        isSubmitting.value = false;

        const scoreData = {
          score: playerStatus.score,
          timeBonus: Math.max(20, 100 - Math.floor((300 - timeRemaining.value) / 3)),
          qualityScore: 85,
          correctnessScore: 90
        };

        handleScoreUpdate(scoreData);
      }
    }
  }, 2000);

  // Store interval ID in the ref object
  intervalIds.value.scoreCheckInterval = scoreCheckInterval;

  inGame.value = true;

  // Auto-start puzzles after a brief delay when the game state is loaded
  const startPuzzleInterval = window.setInterval(() => {
    if (gameState.value && gameState.value.puzzle && !timerInterval) {
      console.log('Auto-starting puzzle after game state loaded...');
      clearInterval(intervalIds.value.startPuzzleInterval);
      loadPuzzle();
    }
  }, 500);

  // Store the interval for cleanup
  intervalIds.value.startPuzzleInterval = startPuzzleInterval;
});

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval);
  if (editor.value) editor.value.destroy();

  // Clean up action subscription
  if (unsubscribeActionRef.value) {
    unsubscribeActionRef.value();
  }

  // Clean up all intervals
  Object.values(intervalIds.value).forEach(id => {
    window.clearInterval(id);
  });

  cleanup();
});

// Watch for score updates in gameState
watch(() => gameState.value?.scores, (newScores, oldScores) => {
  if (!newScores || !currentPlayer.value) return;

  // Get current player's score
  const playerScore = newScores[currentPlayer.value.id];
  const prevScore = oldScores?.[currentPlayer.value.id] || 0;

  console.log(`🔢 SCORE WATCHER: Checking scores - previous: ${prevScore}, new: ${playerScore}`);

  // If the score has increased, show the popup
  if (playerScore > prevScore && playerScore > 0) {
    console.log(`🔢 SCORE WATCHER: Score increased from ${prevScore} to ${playerScore}`);

    // Only show the popup if not already showing
    if (!showScorePopup.value) {
      const scoreData = {
        score: playerScore,
        timeBonus: Math.max(20, 100 - Math.floor((300 - timeRemaining.value) / 3)),
        qualityScore: 85,
        correctnessScore: 90
      };

      console.log('🔢 SCORE WATCHER: Triggering score update from watcher');
      handleScoreUpdate(scoreData);
    }
  }
}, { deep: true });

// Also watch playerStatus to detect score changes there
watch(() => gameState.value?.playerStatus, (newStatus, oldStatus) => {
  if (!newStatus || !currentPlayer.value) return;

  const playerStatus = newStatus[currentPlayer.value.id];
  const oldPlayerStatus = oldStatus?.[currentPlayer.value.id];

  if (playerStatus && (!oldPlayerStatus || playerStatus.score > oldPlayerStatus.score) && playerStatus.score > 0) {
    const newScore = playerStatus.score;
    const oldScore = oldPlayerStatus?.score || 0;

    console.log(`🔢 PLAYER STATUS WATCHER: Score increased from ${oldScore} to ${newScore}`);

    // Only show the popup if not already showing
    if (!showScorePopup.value) {
      const scoreData = {
        score: newScore,
        timeBonus: Math.max(20, 100 - Math.floor((300 - timeRemaining.value) / 3)),
        qualityScore: 85,
        correctnessScore: 90
      };

      console.log('🔢 PLAYER STATUS WATCHER: Triggering score update from status watcher');
      handleScoreUpdate(scoreData);
    }
  }
}, { deep: true });

// Watch for round changes in gameState
watch(() => gameState.value?.currentRound, (newRound, oldRound) => {
  if (newRound && oldRound && newRound > oldRound) {
    console.log(`🔄 ROUND CHANGE: Round changed from ${oldRound} to ${newRound}`);

    // Reset UI for new round
    showPuzzleButton.value = false; // Don't show the button, auto-start instead
    timeRemaining.value = 300;
    textBubble.value = "What would you like to do for this round?";
    code.value = '';

    // If editor is initialized, clear it
    if (editor.value) {
      editor.value.dispatch({
        changes: { from: 0, to: editor.value.state.doc.length, insert: '' }
      });
    }

    // Show notification about new round
    gameNotification.value = `Round ${oldRound} completed! Starting round ${newRound}...`;
    setTimeout(() => {
      if (gameNotification.value.includes(`Round ${oldRound} completed`)) {
        gameNotification.value = '';
      }
    }, 5000);

    // Only start timer if it's not already running
    if (!timerInterval) {
      console.log('🔄 ROUND CHANGE: Starting timer for new round');
      startTimer();
    } else {
      console.log('🔄 ROUND CHANGE: Timer already running, not restarting');
    }

    // Make sure editor is initialized for the new round
    setTimeout(() => {
      initEditorWithRetries(true);
    }, 500);
  }
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
