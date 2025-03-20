<template>
  <div>
    <h1>Available Puzzles</h1>
    <input v-model="searchQuery" placeholder="Search puzzles..." />
    <button @click="fetchPuzzles">Search</button>
    <div>
      <select v-model="selectedType">
        <option value="">Select Type</option>
        <option value="BY-PASS">BY-PASS</option>
        <option value="Faulty">Faulty</option>
        <option value="Multi-Step">Multi-Step</option>
      </select>
      <select v-model="selectedDifficulty">
        <option value="">Select Difficulty</option>
        <option value="Easy">Easy</option>
        <option value="Medium">Medium</option>
        <option value="Hard">Hard</option>
      </select>
    </div>
    <div v-if="paginatedPuzzles.length">
      <div v-for="puzzle in paginatedPuzzles" :key="puzzle.name">
        <h2>{{ puzzle.name }}</h2>
        <p>{{ puzzle.description }}</p>
        <button>Solve Now</button>
      </div>
    </div>
    <div>
      <button @click="prevPage" :disabled="currentPage === 1">Previous</button>
      <span>Page {{ currentPage }} of {{ totalPages }}</span>
      <button @click="nextPage" :disabled="currentPage === totalPages">Next</button>
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
        const response = await axios.get('/api/puzzles')
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
