<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">
      <h2 class="text-2xl font-semibold text-center mb-6">Our puzzles</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div
          v-for="puzzle in puzzles"
          :key="puzzle.title"
          @click="handlePuzzleClick(puzzle.title)"
          class="bg-white p-6 shadow rounded-lg flex flex-col hover:shadow-xl transition-shadow duration-300 cursor-pointer"
        >
          <div class="h-48 overflow-hidden rounded-lg mb-4">
            <img
              :src="puzzle.picture"
              alt="Puzzle image"
              class="w-full h-full object-contain bg-gray-100"
            />
          </div>
          <h3 class="text-xl font-semibold">{{ puzzle.title }}</h3>
          <p class="text-gray-600 mt-2 flex-grow">{{ puzzle.description }}</p>
        </div>
      </div>

      <h2 class="text-2xl font-semibold text-center mt-10 mb-4">Global top 5 players</h2>
      <div class="bg-white p-4 shadow rounded-lg mt-4">
        <!-- Loading State -->
        <div v-if="loading" class="py-8 text-center">
          <div class="inline-block w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mb-4"></div>
          <p>Loading rankings...</p>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="py-8 text-center text-red-500">
          <p>{{ error }}</p>
          <button @click="fetchGlobalRankings" class="mt-4 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">
            Retry
          </button>
        </div>

        <!-- Rankings List -->
        <template v-else>
          <div class="rankings-list">
            <div class="ranking-header">
              <span class="rank-col">Position</span>
              <span class="user-col">Player</span>
              <span class="elo-col">Elo</span>
              <span class="country-col">Country</span>
            </div>

            <div
              v-for="(user, index) in rankings"
              :key="index"
              class="ranking-item"
            >
              <span class="rank-col">
                <span v-if="index < 3" class="medal">{{ getMedal(index) }}</span>
                <span v-else>{{ index + 1 }}</span>
              </span>
              <span class="user-col">
                <div class="user-info">
                  <img
                    :src="user.avatar || '/default-avatar.svg'"
                    :alt="user.name"
                    class="user-avatar"
                  />
                  <span class="username">{{ user.name }}</span>
                </div>
              </span>
              <span class="elo-col">{{ user.rank }}</span>
              <span class="country-col">{{ user.country || 'Unknown' }}</span>
            </div>
          </div>
          <div class="text-center mt-4">
            <router-link to="/ranking" class="view-all-btn">View Full Rankings</router-link>
          </div>
        </template>
      </div>
    </main>
  </div>
</template>

<script lang="ts">
import api from '../services/api'

interface RankingUser {
  name: string;
  rank: string;
  avatar: string;
  country: string;
}

interface Puzzle {
  title: string;
  description: string;
  picture: string;
}

export default {
  name: 'HomeView',
  data() {
    return {
      puzzles: [
        {
          title: 'VS Mode',
          description:
            'Challenge your friends or random opponents in competitive duels. Matchmaking is based on rank. You can get any form of puzzle at random.',
          picture: '/assets/VS_Mode.PNG',
        },
        {
          title: 'BY_PASS',
          description:
            'Test your skills. You must try to bypass certain restrictions set on the prompt.',
          picture: '/assets/BYPASS.PNG',
        },
        {
          title: 'MULTI_STEP',
          description: 'Solve multi-step challenges where you build up the code step by step.',
          picture: '/assets/Step_by_Step_Puzzles.PNG',
        },
        {
          title: 'FAULTY',
          description: 'Identify and fix intentional bugs made by the LLM.',
          picture: '/assets/Faulty_Code_Generator.PNG',
        },
      ] as Puzzle[],
      rankings: [] as RankingUser[],
      loading: false,
      error: ''
    }
  },
  methods: {
    getMedal(index: number): string {
      const medals = ['🥇', '🥈', '🥉']
      return medals[index]
    },
    handlePuzzleClick(puzzleTitle: string) {
      if (puzzleTitle === 'VS Mode') {
        this.$router.push('/vs')
      } else {
        // Route to PuzzleOverview with filter applied
        this.$router.push({
          path: '/puzzle',
          query: { type: puzzleTitle }
        })
      }
    },
    async fetchGlobalRankings() {
      this.loading = true
      this.error = ''

      try {
        const params = {
          page: 0,
          size: 5
        }

        const response = await api.get('/users/rankings/global', { params })

        this.rankings = response.data.content.map((user: any) => ({
          name: user.username,
          rank: user.elo.toString(),
          avatar: user.picture || '/default-avatar.svg',
          country: user.country || 'Unknown'
        }))
      } catch (err: any) {
        console.error('Error fetching rankings:', err)
        this.error = 'Failed to load rankings'
      } finally {
        this.loading = false
      }
    }
  },
  mounted() {
    this.fetchGlobalRankings()
  },
}
</script>

<style scoped>
body {
  font-family: Arial, sans-serif;
}

/* Rankings styling from Ranking.vue */
.rankings-list {
  background: white;
  border-radius: 0.5rem;
  overflow: hidden;
}

.ranking-header {
  display: grid;
  grid-template-columns: 80px 1fr 100px 100px;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: #2d3748;
  color: white;
  font-weight: 600;
  font-size: 0.9rem;
}

.ranking-item {
  display: grid;
  grid-template-columns: 80px 1fr 100px 100px;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e2e8f0;
  align-items: center;
  transition: all 0.3s ease;
}

.ranking-item:hover {
  background: #f8fafc;
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank-col {
  font-size: 1.1rem;
  font-weight: bold;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}

.medal {
  font-size: 1.25rem;
}

.user-col {
  font-size: 1.1rem;
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}

.username {
  font-weight: 500;
  color: #2d3748;
}

.elo-col {
  font-size: 1rem;
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

.view-all-btn {
  display: inline-block;
  padding: 0.5rem 1.25rem;
  background: #4a5568;
  color: white;
  border-radius: 0.375rem;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.3s ease;
}

.view-all-btn:hover {
  background: #2d3748;
  transform: translateY(-2px);
}

/* Responsive Design */
@media (max-width: 768px) {
  .ranking-header,
  .ranking-item {
    grid-template-columns: 60px 1fr 80px;
  }

  .country-col {
    display: none;
  }
}
</style>
