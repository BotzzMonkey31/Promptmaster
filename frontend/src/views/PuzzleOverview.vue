<template>
  <div class="p-6 bg-white rounded-2xl shadow-xl w-full mx-auto">
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
        <option value="BY-PASS">BY-PASS</option>
        <option value="Faulty">Faulty</option>
        <option value="Multi-Step">Multi-Step</option>
      </select>
      <select
        v-model="selectedDifficulty"
        class="px-4 py-3 border rounded-full w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <option value="">Select Difficulty</option>
        <option value="Easy">Easy</option>
        <option value="Medium">Medium</option>
        <option value="Hard">Hard</option>
      </select>
    </div>

    <div
      v-if="paginatedPuzzles.length"
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
    >
      <div
        v-for="puzzle in paginatedPuzzles"
        :key="puzzle.name"
        class="p-6 border rounded-2xl shadow-md bg-gray-50"
      >
        <!-- Name and Tags -->
        <div class="flex items-center space-x-2 mb-2">
          <h2 class="text-2xl font-semibold text-gray-800">{{ puzzle.name }}</h2>
          <span
            :class="difficultyClass(puzzle.difficulty)"
            class="px-3 py-1 rounded-full text-white text-sm"
          >
            {{ puzzle.difficulty }}
          </span>
          <span :class="typeClass(puzzle.type)" class="px-3 py-1 rounded-full text-white text-sm">
            {{ puzzle.type }}
          </span>
        </div>

        <!-- Description -->
        <p class="text-gray-600 mt-2">{{ puzzle.description }}</p>

        <!-- Solve Button -->
        <button
          class="mt-4 px-6 py-3 bg-green-600 text-white rounded-full hover:bg-green-700 transition-all"
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
import axios from 'axios'

interface Puzzle {
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
      return this.puzzles.filter(
        (puzzle) =>
          puzzle.name.toLowerCase().includes(this.searchQuery.toLowerCase()) &&
          (this.selectedType === '' || puzzle.type === this.selectedType) &&
          (this.selectedDifficulty === '' || puzzle.difficulty === this.selectedDifficulty),
      )
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
        const response = await axios.get('http://localhost:8080/puzzles')
        this.puzzles = response.data
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
    difficultyClass(difficulty: string) {
      return (
        {
          Easy: 'bg-green-600',
          Medium: 'bg-yellow-500',
          Hard: 'bg-red-600',
        }[difficulty] || 'bg-gray-500'
      )
    },
    typeClass(type: string) {
      console.log('Original type:', type) // Debug original value

      // Convert spaces to hyphens and uppercase
      const normalizedType = type.toUpperCase().replace(/ /g, '-')
      console.log('Normalized type:', normalizedType) // Debug normalized value

      const styles: Record<string, string> = {
        'BY-PASS': 'bg-black',
        FAULTY: 'bg-blue-900',
        'MULTI-STEP': 'bg-blue-400',
      }

      console.log('Available styles:', Object.keys(styles)) // Debug available keys
      console.log('Selected style:', styles[normalizedType] || 'bg-gray-500') // Debug selected style

      return styles[normalizedType] || 'bg-gray-500'
    },
  },
  mounted() {
    this.fetchPuzzles()
  },
}
</script>
