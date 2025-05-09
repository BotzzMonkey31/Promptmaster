<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">
      <h2 class="text-2xl font-semibold text-center mb-6">Game Lobby</h2>
      <p class="text-center text-gray-600 mb-4">Find an opponent or invite a friend to a match!</p>

      <div class="flex justify-center gap-4 mb-6">
        <button class="bg-blue-500 text-white px-4 py-2 rounded">Find Random Opponent</button>
        <button class="bg-green-500 text-white px-4 py-2 rounded">Invite a Friend</button>
      </div>

      <div class="bg-white p-6 shadow rounded-lg mb-6">
        <h3 class="text-lg font-semibold mb-4">Available Players</h3>
        <ul>
          <li
            v-for="player in players"
            :key="player.name"
            class="flex justify-between items-center py-2 border-b"
          >
            <div class="flex items-center">
              <img :src="player.avatar" class="w-8 h-8 rounded-full mr-3" alt="Avatar" />
              <span>{{ player.name }}</span>
            </div>
            <button class="bg-blue-500 text-white px-3 py-1 rounded">Challenge</button>
          </li>
        </ul>
      </div>

      <div class="bg-white p-6 shadow rounded-lg">
        <h3 class="text-lg font-semibold mb-4">Your Rank</h3>
        <div v-if="error" class="text-red-500 mb-4">
          {{ error }}
        </div>
        <div v-else-if="loading" class="text-gray-500 mb-4">Loading user data...</div>
        <div v-else>
          <div v-if="user" class="flex items-center">
            <img :src="user.picture" class="w-10 h-10 rounded-full mr-3" alt="Avatar" />
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
import axios from 'axios'
import Cookies from 'js-cookie'
import ChatBox from '../components/ChatBox.vue'
import apiClient from '../services/api'

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

export default {
  name: 'GameLobby',
  components: { ChatBox },
  data() {
    return {
      players: [
        { name: 'Player123', avatar: 'avatar1.jpg' },
        { name: 'AI_Master', avatar: 'avatar2.jpg' },
        { name: 'CodeCracker', avatar: 'avatar3.jpg' },
      ],
      user: null as User | null,
      loading: true,
      error: null as string | null,
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

  async created() {
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

      console.log('Found user cookie:', savedUser)

      try {
        const userData = JSON.parse(savedUser)
        console.log('Parsed user cookie data:', userData)

        if (!userData.email) {
          console.error('No email found in user cookie')
          this.error = 'User data is incomplete'
          this.loading = false
          return
        }

        // Log the API URL and request
        console.log('API base URL:', import.meta.env.VITE_API_BASE_URL || 'not defined')

        // Use query parameter instead of path parameter
        console.log(`Making modified request with email: ${userData.email}`)

        // Add timeout to API call and use query parameters instead
        const response = await apiClient.get('/users/email', {
          params: { email: userData.email },  // Send as query parameter
          timeout: 10000 // 10 second timeout
        })

        console.log('Full API response:', response)

        if (!response || response.status !== 200) {
          throw new Error(`Server responded with status ${response?.status || 'unknown'}`)
        }

        if (!response.data) {
          console.error('Response exists but no data received')
          throw new Error('No data received from server')
        }

        console.log('User data from API:', response.data)

        this.user = response.data
        this.loading = false
        console.log('User data loaded:', this.user)
      } catch (parseError) {
        console.error('Error parsing user cookie:', parseError)
        this.error = 'Invalid user data format'
        this.loading = false
      }
    } catch (error: any) {
      console.error('Error fetching user data:', error)
      console.error('Error message:', error.message)

      if (error.response) {
        // The request was made and the server responded with a status code
        console.error('Response data:', error.response.data)
        console.error('Response status:', error.response.status)
        console.error('Response headers:', error.response.headers)
      } else if (error.request) {
        // The request was made but no response was received
        console.error('Request made but no response:', error.request)
      }

      this.error = `Failed to load user data: ${error.message || 'Unknown error'}`
      this.loading = false
    }
  },
}
</script>

<style>
body {
  font-family: Arial, sans-serif;
}
</style>
