<template>
  <div class="ranking-container">
    <h1 class="page-title">Leaderboards</h1>

    <!-- Toggle between Global and Local -->
    <div class="toggle-container">
      <button
        @click="activeTab = 'global'"
        :class="['toggle-btn', { active: activeTab === 'global' }]"
      >
        Global Ranking
      </button>
      <button
        @click="activeTab = 'local'"
        :class="['toggle-btn', { active: activeTab === 'local' }]"
      >
        Local Ranking ({{ userCountry || 'Your Country' }})
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>Loading rankings...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error">
      <p>{{ error }}</p>
      <button @click="fetchRankings" class="retry-btn">Retry</button>
    </div>

    <!-- Rankings List -->
    <div v-else class="rankings-list">
      <div class="ranking-header">
        <span class="rank-col">Position</span>
        <span class="user-col">Player</span>
        <span class="elo-col">Elo</span>
      </div>

      <div
        v-for="(user, index) in currentRankings"
        :key="user.id"
        :class="['ranking-item', { 'current-user': user.id === currentUserId }]"
      >
        <span class="rank-col">
          <span v-if="getGlobalPosition(index) <= 3" class="medal">{{ getMedal(getGlobalPosition(index) - 1) }}</span>
          <span v-else>{{ getGlobalPosition(index) }}</span>
        </span>
        <span class="user-col">
          <div class="user-info">
            <img
              :src="user.picture || '/default-avatar.svg'"
              :alt="user.username"
              class="user-avatar"
            >
            <span class="username">{{ user.username }}</span>
          </div>
        </span>
        <span class="elo-col">{{ user.elo }}</span>
        <span class="country-col" v-if="activeTab === 'global'">
          {{ user.country || 'Unknown' }}
        </span>
      </div>

      <!-- No results -->
      <div v-if="currentRankings.length === 0" class="no-results">
        <p>No rankings available for {{ activeTab === 'global' ? 'global' : 'local' }} leaderboard.</p>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        @click="currentPage = Math.max(0, currentPage - 1)"
        :disabled="currentPage === 0"
        class="page-btn"
      >
        Previous
      </button>
      <span class="page-info">{{ currentPage + 1 }} of {{ totalPages }}</span>
      <button
        @click="currentPage = Math.min(totalPages - 1, currentPage + 1)"
        :disabled="currentPage === totalPages - 1"
        class="page-btn"
      >
        Next
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import api from '../services/api'
import Cookies from 'js-cookie'

interface RankingUser {
  id: number
  username: string
  picture?: string
  elo: number
  country?: string
}

interface RankingResponse {
  content: RankingUser[]
  totalElements: number
  totalPages: number
  number: number
}

interface CurrentUser {
  id: number
  username: string
  country?: string
}

const activeTab = ref('global')
const loading = ref(false)
const error = ref('')
const globalRankings = ref<RankingUser[]>([])
const localRankings = ref<RankingUser[]>([])
const currentPage = ref(0) // Spring Boot uses 0-based pagination
const totalElements = ref(0)
const totalPages = ref(0)
const currentUser = ref<CurrentUser | null>(null)

const currentUserId = computed(() => currentUser.value?.id)
const userCountry = computed(() => currentUser.value?.country)

const currentRankings = computed(() => {
  return activeTab.value === 'global' ? globalRankings.value : localRankings.value
})

const fetchCurrentUser = async () => {
  try {
    const userId = await getUserId()
    if (userId) {
      // Call the /users/{id} endpoint
      const response = await api.get<CurrentUser>(`/users/${userId}`)
      currentUser.value = response.data
      return
    }

    // If no user ID, user is not authenticated
    currentUser.value = null
  } catch (err) {
    console.log('User not authenticated or error fetching user')
    currentUser.value = null
  }
}

// Helper function to get user ID (using the same pattern as other components)
const getUserId = async (): Promise<string | null> => {
  let userId = null
  const userCookie = Cookies.get('user')

  if (userCookie) {
    try {
      const userData = JSON.parse(userCookie)

      // First, check if we already have this user's ID in localStorage
      const storedUserId = localStorage.getItem(`userId_${userData.email}`)
      if (storedUserId) {
        return storedUserId
      }

      try {
        const response = await api.get('/users/email', {
          params: { email: userData.email },
          timeout: 10000, // Increase timeout to 10s
        })

        // Check if we got a valid response
        if (response && response.data) {
          // Different ways the ID might be represented in the response
          const id =
            response.data.id ||
            response.data.userId ||
            (typeof response.data === 'number' ? response.data : null)

          if (id) {
            const idString = id.toString()
            localStorage.setItem(`userId_${userData.email}`, idString)
            return idString
          }
        }
      } catch (apiError) {
        console.error('API error fetching user ID:', apiError)

        // Try an alternative API endpoint if the first one failed
        try {
          const checkResponse = await api.get(`/users/check/${userData.email}`)

          if (checkResponse?.data?.data) {
            // The /users/check endpoint returns ApiResponse<Boolean>, but we need the user
            // Let's try to get the user by email again or use a different approach
            const emailResponse = await api.get(`/users/email/${userData.email}`)
            if (emailResponse?.data?.id) {
              const idString = emailResponse.data.id.toString()
              localStorage.setItem(`userId_${userData.email}`, idString)
              return idString
            }
          }
        } catch (altError) {
          console.error('Alternative API also failed:', altError)
        }
      }
    } catch (e) {
      console.error('Error parsing user cookie:', e)
    }
  }

  // If we reach here, we couldn't get the user ID from API, so user is not authenticated
  return null
}

const fetchRankings = async () => {
  loading.value = true
  error.value = ''

  try {
    const endpoint = activeTab.value === 'global'
      ? '/users/rankings/global'
      : '/users/rankings/local'

    const params: any = {
      page: currentPage.value,
      size: 5
    }

    // For local rankings, we need a country parameter
    if (activeTab.value === 'local') {
      if (!userCountry.value) {
        error.value = 'Please log in to view local rankings for your country.'
        loading.value = false
        return
      }
      params.country = userCountry.value
    }

    const response = await api.get<RankingResponse>(endpoint, { params })

    if (activeTab.value === 'global') {
      globalRankings.value = response.data.content
    } else {
      localRankings.value = response.data.content
    }

    totalElements.value = response.data.totalElements
    totalPages.value = response.data.totalPages
  } catch (err: any) {
    error.value = 'Failed to load rankings. Please try again.'
    console.error('Error fetching rankings:', err)
  } finally {
    loading.value = false
  }
}

const getMedal = (index: number): string => {
  const medals = ['🥇', '🥈', '🥉']
  return medals[index]
}

const getGlobalPosition = (pageIndex: number): number => {
  return (currentPage.value * 6) + pageIndex + 1
}

// Watch for tab changes and reset page
watch(activeTab, () => {
  currentPage.value = 0
  fetchRankings()
})

// Watch for page changes
watch(currentPage, () => {
  fetchRankings()
})

onMounted(async () => {
  await fetchCurrentUser()
  fetchRankings()
})
</script>

<style scoped>
.ranking-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  min-height: 74vh;
}

.page-title {
  text-align: center;
  font-size: 2.5rem;
  font-weight: bold;
  color: #2d3748;
  margin-bottom: 2rem;
}

.toggle-container {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.toggle-btn {
  padding: 0.75rem 1.5rem;
  border: 2px solid #4a5568;
  background: #f8fafc;
  color: #4a5568;
  border-radius: 0.5rem;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
  font-size: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toggle-btn:hover {
  background: #4a5568;
  color: white;
  transform: translateY(-2px);
}

.toggle-btn.active {
  background: #2d3748;
  color: white;
  border-color: #2d3748;
  transform: translateY(-2px);
}

.loading {
  text-align: center;
  padding: 3rem;
  color: white;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top: 4px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error {
  text-align: center;
  padding: 2rem;
  color: #ff6b6b;
  background: white;
  border-radius: 1rem;
  margin: 2rem 0;
}

.retry-btn {
  margin-top: 1rem;
  padding: 0.5rem 1rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.retry-btn:hover {
  background: #5a67d8;
  transform: translateY(-2px);
}

.rankings-list {
  background: white;
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.ranking-header {
  display: grid;
  grid-template-columns: 80px 1fr 120px 100px;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: #2d3748;
  color: white;
  font-weight: 600;
}

.ranking-item {
  display: grid;
  grid-template-columns: 80px 1fr 120px 100px;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  align-items: center;
  transition: all 0.3s ease;
}

.ranking-item:hover {
  background: #f8fafc;
  transform: translateX(5px);
}

.ranking-item.current-user {
  background: linear-gradient(135deg, #fef7e0 0%, #fbeaa5 100%);
  border-left: 4px solid #f59e0b;
  font-weight: 600;
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank-col {
  font-size: 1.25rem;
  font-weight: bold;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}

.medal {
  font-size: 1.5rem;
}

.user-col {
  font-size: 1.25rem;
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}

.username {
  font-weight: 500;
  color: #2d3748;
}

.elo-col {
  font-size: 1.1rem;
  font-weight: bold;
  color: #667eea;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}

.country-col {
  text-align: center;
  color: #718096;
  font-size: 0.9rem;
  display: flex;
  justify-content: center;
  align-items: center;
}

.no-results {
  padding: 3rem;
  text-align: center;
  color: #718096;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
}

.page-btn {
  padding: 0.75rem 1.5rem;
  border: 2px solid #4a5568;
  background: #f8fafc;
  color: #4a5568;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-btn:hover:not(:disabled) {
  background: #4a5568;
  color: white;
  transform: translateY(-2px);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #4a5568;
  font-weight: 500;
  padding: 0.5rem 1rem;
  background: #e2e8f0;
  border-radius: 0.5rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  .ranking-container {
    padding: 1rem;
  }

  .page-title {
    font-size: 2rem;
  }

  .toggle-container {
    flex-direction: column;
    align-items: center;
  }

  .ranking-header,
  .ranking-item {
    grid-template-columns: 60px 1fr 100px;
    padding: 0.75rem 1rem;
  }

  .country {
    display: none;
  }

  .user-avatar {
    width: 32px;
    height: 32px;
  }

  .pagination {
    flex-direction: column;
    gap: 0.5rem;
  }

  .toggle-btn,
  .page-btn {
    font-size: 0.9rem;
    padding: 0.5rem 1rem;
  }
}

/* Local rankings adjustments */
.rankings-list:has(.ranking-item:not(.ranking-item .country)) .ranking-header {
  grid-template-columns: 80px 1fr 120px;
}

.rankings-list:has(.ranking-item:not(.ranking-item .country)) .ranking-item {
  grid-template-columns: 80px 1fr 120px;
}
</style>
