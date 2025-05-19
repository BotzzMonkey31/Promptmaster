<template>
  <div>
    <component
      :is="currentComponent"
      v-if="currentComponent && puzzle"
      :puzzle="puzzle"
    />
    <div v-else-if="loading" class="flex justify-center items-center h-screen">
      <div class="text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mx-auto mb-4"></div>
        <p class="text-lg text-gray-700">Loading and preparing your fresh puzzle...</p>
      </div>
    </div>
    <div v-else-if="error" class="flex justify-center items-center h-screen">
      <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative" role="alert">
        <strong class="font-bold">Error:</strong>
        <span class="block sm:inline"> {{ error }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import apiClient from '../services/api';
import MultiStepSolve from './MultiStepSolve.vue';
import BypassSolve from './BypassSolve.vue';
import Cookies from 'js-cookie';
// import FaultySolve from './FaultySolve.vue';


const route = useRoute();
const puzzle = ref<any>(null);
const loading = ref(true);
const error = ref('');

// Helper function to get user ID (copied from MultiStepSolve to maintain consistency)
const getUserId = async (): Promise<string | null> => {
  let userId = null;
  const userCookie = Cookies.get('user');

  if (userCookie) {
    try {
      const userData = JSON.parse(userCookie);

      // First, check if we already have this user's ID in localStorage
      const storedUserId = localStorage.getItem(`userId_${userData.email}`);
      if (storedUserId) {
        return storedUserId;
      }

      try {
        const response = await apiClient.get('/users/email', {
          params: { email: userData.email },
          timeout: 10000 // Increase timeout to 10s
        });

        // Check if we got a valid response
        if (response && response.data) {
          // Different ways the ID might be represented in the response
          const id = response.data.id || response.data.userId ||
                     (typeof response.data === 'number' ? response.data : null);

          if (id) {
            const idString = id.toString();
            localStorage.setItem(`userId_${userData.email}`, idString);
            return idString;
          }
        }
      } catch (apiError) {
        console.error("API error fetching user ID:", apiError);

        // Try an alternative API endpoint if the first one failed
        try {
          const checkResponse = await apiClient.get(`/users/check/${userData.email}`);

          if (checkResponse?.data?.userId) {
            const idString = checkResponse.data.userId.toString();
            localStorage.setItem(`userId_${userData.email}`, idString);
            return idString;
          }
        } catch (altError) {
          console.error("Alternative API also failed:", altError);
        }
      }
    } catch (e) {
      console.error("Error parsing user cookie:", e);
    }
  }

  // If we reach here, we couldn't get the user ID from API, so use guest ID as fallback
  userId = localStorage.getItem('guestId');
  if (!userId) {
    userId = Date.now().toString();
    localStorage.setItem('guestId', userId);
  }

  return userId;
};

// Start a fresh puzzle session
const startFreshSession = async (puzzleId: string | string[]) => {
  try {
    const userId = await getUserId();
    if (!userId) {
      throw new Error('Could not determine user ID');
    }

    const id = Array.isArray(puzzleId) ? puzzleId[0] : puzzleId;

    // Reset the session to start fresh
    const response = await apiClient.post('/ai/start-fresh', {
      puzzleId: parseInt(id as string),
      userId: Number(userId)
    });

    return true;
  } catch (error: any) {
    console.error('Error starting fresh session:', error);
    // Log the response error data if available
    if (error.response) {
      console.error('Error response data:', error.response.data);
    }
    return false;
  }
};

const fetchPuzzle = async () => {
  loading.value = true;
  error.value = '';
  try {
    const id = route.params.id;

    // First start a fresh session
    const sessionStarted = await startFreshSession(id);

    if (!sessionStarted) {
      error.value = 'Failed to start a fresh session. Please try again.';
      loading.value = false;
      return;
    }

    // Then fetch the puzzle details
    const response = await apiClient.get(`/puzzles/${id}`);
    puzzle.value = response.data;
  } catch (e) {
    error.value = 'Could not load the puzzle. Please try again.';
    console.error('Error loading puzzle:', e);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchPuzzle);

const currentComponent = computed(() => {
  if (!puzzle.value) return null;
  switch (puzzle.value.type) {
    case 'Multi_Step':
      return MultiStepSolve;
    case 'BY_PASS':
      return BypassSolve;
    // case 'Faulty':
    //   return FaultySolve;
    default:
      return null;
  }
});
</script>
