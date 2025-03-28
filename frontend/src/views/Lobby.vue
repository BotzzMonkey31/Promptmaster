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
    </main>
    <ChatBox />
  </div>
</template>

<script lang="ts">
import axios from 'axios'
import Cookies from 'js-cookie'
import ChatBox from '../components/ChatBox.vue'

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
      if (!currentRank) return 100 // PromptMaster

      const nextRank = rankThresholds.find((r) => r.min > currentRank.min)
      if (!nextRank) return 100 // No higher rank

      const progress = ((this.user.elo - currentRank.min) / (nextRank.min - currentRank.min)) * 100
      return Math.min(Math.max(Math.round(progress), 0), 100)
    },
  },

  async created() {
    try {
      const savedUser = Cookies.get('user')
      if (!savedUser) {
        this.error = 'No user is logged in'
        this.loading = false
        return
      }

      const userData = JSON.parse(savedUser)
      console.log('Attempting to fetch user data for:', userData.email)

      try {
        const response = await axios.get<User>(
          `http://localhost:8080/users/email/${encodeURIComponent(userData.email)}`,
          {
            headers: {
              Accept: 'application/json',
              'Content-Type': 'application/json',
            },
          },
        )

        if (!response.data) {
          throw new Error('No data received from server')
        }

        this.user = response.data
        // No need to set rank as it's computed from ELO

        if (!this.user.country) {
          this.user.country = 'Unknown'
        }
      } catch (apiError: any) {
        console.error('API Error:', apiError.response?.data || apiError.message)
        this.error = `API Error: ${apiError.response?.data?.message || apiError.message}`
        throw apiError
      }

      this.loading = false
    } catch (error: any) {
      console.error('Error details:', error)
      this.error =
        error.response?.data?.message || 'Failed to load user data. Please try again later.'
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
