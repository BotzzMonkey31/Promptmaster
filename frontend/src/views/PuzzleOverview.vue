<template>
  <div class="max-w-10xl mx-auto p-6 bg-white rounded-2xl shadow-xl m-2">
    <h1 class="text-4xl font-bold text-gray-800 mb-6 text-center">Available Puzzles</h1>
    <p class="text-gray-500 text-center mb-6">
      Select a puzzle below and challenge yourself with AI-powered solutions!
    </p>

    <div class="mb-6 flex items-center space-x-4 justify-center">
      <input
        v-model="searchQuery"
        placeholder="Search puzzles..."
        class="px-4 py-3 border rounded-full w-1/2 focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      <button
        @click="fetchPuzzles"
        class="px-6 py-3 bg-blue-600 text-white rounded-full hover:bg-blue-700 transition-all"
      >
        Search
      </button>
    </div>

    <div class="mb-6 flex space-x-4 justify-center">
      <select
        v-model="selectedType"
        class="px-4 py-3 border rounded-full w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <option value="">Select Type</option>
        <option value="BY_PASS">Bypass</option>
        <option value="FAULTY">Faulty</option>
        <option value="MULTI_STEP">Multi-Step</option>
      </select>
      <select
        v-model="selectedDifficulty"
        class="px-4 py-3 border rounded-full w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <option value="">Select Difficulty</option>
        <option value="EASY">Easy</option>
        <option value="MEDIUM">Medium</option>
        <option value="HARD">Hard</option>
      </select>
    </div>

    <div
      v-if="paginatedPuzzles.length"
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
    >
      <div
        v-for="puzzle in paginatedPuzzles"
        @click="navigateToPuzzle(puzzle.id)"
        :key="puzzle.name"
        class="p-6 border rounded-2xl shadow-md bg-gray-50"
      >
        <div class="flex items-center space-x-2 mb-2">
          <h2 class="text-2xl font-semibold text-gray-800">{{ puzzle.name }}</h2>
          <span
            :class="difficultyClass(puzzle.difficulty)"
            class="px-3 py-1 rounded-full text-white text-sm"
          >
            {{ puzzle.difficulty }}
          </span>
          <span :class="typeClass(puzzle.type)" class="px-3 py-1 rounded-full text-white text-sm">
            {{ formatType(puzzle.type) }}
          </span>
        </div>

        <p class="text-gray-600 mt-2">{{ puzzle.description }}</p>

        <button
          class="mt-4 px-6 py-3 bg-green-600 text-white rounded-full hover:bg-green-700 transition-all"
          @click.stop="$router.push(`/solve/${puzzle.id}`)"
        >
          Solve Now
        </button>
      </div>
    </div>

    <div class="mt-8 flex items-center justify-center space-x-4">
      <button
        @click="prevPage"
        :disabled="currentPage === 1"
        class="px-6 py-3 bg-gray-400 text-white rounded-full cursor-pointer disabled:opacity-50"
      >
        Previous
      </button>
      <span class="text-lg font-semibold">Page {{ currentPage }} of {{ totalPages }}</span>
      <button
        @click="nextPage"
        :disabled="currentPage === totalPages"
        class="px-6 py-3 bg-gray-400 text-white rounded-full cursor-pointer disabled:opacity-50"
      >
        Next
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import apiClient from '../services/api'

interface Puzzle {
  id: number
  name: string
  difficulty: string
  type: string
  description: string
}

export default {
  name: 'PuzzleComponent',
  data() {
    return {
      puzzles: [] as Puzzle[],
      searchQuery: '',
      selectedType: '',
      selectedDifficulty: '',
      currentPage: 1,
      itemsPerPage: 6,
    }
  },
  computed: {
    filteredPuzzles() {
      return this.puzzles.filter((puzzle) => {
        const nameMatch = puzzle.name.toLowerCase().includes(this.searchQuery.toLowerCase())

        const typeMatch =
          this.selectedType === '' || puzzle.type === this.typeDisplayToValue(this.selectedType)

        const difficultyMatch =
          this.selectedDifficulty === '' || puzzle.difficulty === this.selectedDifficulty

        return nameMatch && typeMatch && difficultyMatch
      })
    },
    paginatedPuzzles() {
      const start = (this.currentPage - 1) * this.itemsPerPage
      return this.filteredPuzzles.slice(start, start + this.itemsPerPage)
    },
    totalPages() {
      return Math.ceil(this.filteredPuzzles.length / this.itemsPerPage)
    },
  },
  methods: {
    async fetchPuzzles() {
      try {
        const response = await apiClient.get('/puzzles')

        // Store the data as-is without normalization
        this.puzzles = response.data.map((puzzle: Puzzle) => {
          return {
            ...puzzle,
          }
        })

        return response
      } catch (error) {
        console.error('Error fetching puzzles:', error)
      }
    },
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++
      }
    },
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--
      }
    },
    navigateToPuzzle(id: number) {
      this.$router.push(`/puzzle/${id}`)
    },
    difficultyClass(difficulty: string) {
      const styles: Record<string, string> = {
        EASY: 'bg-green-600',
        MEDIUM: 'bg-yellow-500',
        HARD: 'bg-red-600',
      }

      return styles[difficulty] || 'bg-gray-500'
    },
    typeClass(type: string) {
      if (!type) return 'bg-gray-500'
      const styles: Record<string, string> = {
        BY_PASS: 'bg-black',
        FAULTY: 'bg-blue-900',
        MULTI_STEP: 'bg-blue-400',
      }

      return styles[type] || 'bg-gray-500'
    },
    formatType(type: string) {
      if (type === 'BY_PASS') return 'Bypass'
      if (type === 'MULTI_STEP') return 'Multi-Step'
      return type // Return as is for other values like 'FAULTY'
    },
    typeDisplayToValue(displayType: string) {
      if (displayType === 'Bypass') return 'BY_PASS'
      if (displayType === 'Multi-Step') return 'MULTI_STEP'
      return displayType
    },
  },
  mounted() {
    this.fetchPuzzles()

    // Check for query parameters to set initial filter
    const queryType = this.$route.query.type as string
    if (queryType) {
      // Map the type from Home.vue to the internal values used in PuzzleOverview
      this.selectedType = queryType
    }
  },
}
</script>
