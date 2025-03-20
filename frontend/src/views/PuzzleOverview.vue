<template>
  <div class="p-6 bg-white rounded-lg shadow-lg">
    <h1 class="text-3xl font-semibold mb-4 text-center">Available Puzzles</h1>
    <div class="mb-4 flex items-center space-x-4 justify-center">
      <input
        v-model="searchQuery"
        placeholder="Search puzzles..."
        class="px-4 py-2 border rounded-lg w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      <button
        @click="fetchPuzzles"
        class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
      >
        Search
      </button>
    </div>
    <div class="mb-4 flex space-x-4 justify-center">
      <select
        v-model="selectedType"
        class="px-4 py-2 border rounded-lg w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <option value="">Select Type</option>
        <option value="BY-PASS">BY-PASS</option>
        <option value="Faulty">Faulty</option>
        <option value="Multi-Step">Multi-Step</option>
      </select>
      <select
        v-model="selectedDifficulty"
        class="px-4 py-2 border rounded-lg w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <option value="">Select Difficulty</option>
        <option value="Easy">Easy</option>
        <option value="Medium">Medium</option>
        <option value="Hard">Hard</option>
      </select>
    </div>

    <div v-if="paginatedPuzzles.length" class="space-y-4">
      <div
        v-for="puzzle in paginatedPuzzles"
        :key="puzzle.name"
        class="p-4 border-b border-gray-300 rounded-lg shadow-sm"
      >
        <h2 class="text-xl font-medium text-gray-800">{{ puzzle.name }}</h2>
        <p class="text-gray-600">{{ puzzle.description }}</p>
        <button
          class="mt-2 px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
        >
          Solve Now
        </button>
      </div>
    </div>

    <div class="mt-4 flex items-center justify-center space-x-4">
      <button
        @click="prevPage"
        :disabled="currentPage === 1"
        class="px-6 py-2 bg-gray-400 text-white rounded-lg cursor-pointer disabled:opacity-50"
      >
        Previous
      </button>
      <span class="text-lg">Page {{ currentPage }} of {{ totalPages }}</span>
      <button
        @click="nextPage"
        :disabled="currentPage === totalPages"
        class="px-6 py-2 bg-gray-400 text-white rounded-lg cursor-pointer disabled:opacity-50"
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
  description: string
  type: string
  difficulty: string
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
      itemsPerPage: 3,
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
  },
  mounted() {
    this.fetchPuzzles()
  },
}
</script>
