<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-5xl mx-auto py-10 px-4">
      <div class="bg-white rounded-lg p-8 mb-6 shadow-md">
        <div class="mb-4">
          <h1 class="text-3xl font-bold mb-2">Puzzle: {{ puzzle.name }}</h1>
          <div class="flex gap-2 mb-4">
            <span
              :class="typeClass(puzzle.type)"
              class="px-3 py-1 text-white rounded-full text-sm font-medium"
            >
              {{ formatType(puzzle.type) }}
            </span>
            <span
              :class="difficultyClass(puzzle.difficulty)"
              class="px-3 py-1 text-white rounded-full text-sm font-medium"
            >
              {{ puzzle.difficulty }}
            </span>
          </div>
          <p class="text-gray-800">
            {{ puzzle.description }}
          </p>
        </div>

        <div class="mb-6">
          <h2 class="text-xl font-bold mb-2">Instructions:</h2>
          <ol class="list-decimal pl-6 space-y-1">
            <li v-for="(instruction, index) in instructions" :key="`instruction-${index}`">
              {{ instruction }}
            </li>
          </ol>
        </div>

        <div class="mb-6">
          <h2 class="text-xl font-bold mb-2">Tips:</h2>
          <ul class="list-disc pl-6 space-y-1">
            <li v-for="(tip, index) in tips" :key="`tip-${index}`">
              {{ tip }}
            </li>
          </ul>
        </div>

        <button
          @click="startSolving"
          class="px-6 py-3 bg-gradient-to-r from-green-400 to-blue-500 text-white font-medium rounded-full hover:opacity-90 transition-all"
        >
          Start Solving
        </button>
      </div>

      <div class="bg-white rounded-lg p-8 shadow-md">
        <h2 class="text-2xl font-bold mb-6">Previous Solutions</h2>

        <div v-if="solutions.length">
          <div
            v-for="(solution, index) in solutions"
            :key="`solution-${index}`"
            :class="{ 'border-b pb-4 mb-4': index !== solutions.length - 1 }"
          >
            <h3 class="text-xl font-bold mb-1">Solution by {{ solution.author }}</h3>
            <p class="text-gray-600">{{ solution.description }}</p>
          </div>
        </div>
        <div v-else class="text-gray-600">No solutions found for this puzzle yet.</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import apiClient from '../services/api'

export default defineComponent({
  name: 'PuzzleDetail',
  data() {
    return {
      puzzle: {
        id: 0,
        name: '',
        type: '',
        difficulty: '',
        description: '',
      },
      instructions: [] as string[],
      tips: [] as string[],
      solutions: [] as Array<{ author: string; description: string }>,
    }
  },
  methods: {
    difficultyClass(difficulty: string) {
      return (
        {
          EASY: 'bg-green-600',
          MEDIUM: 'bg-yellow-500',
          HARD: 'bg-red-600',
        }[difficulty] || 'bg-gray-500'
      )
    },
    typeClass(type: string) {
      if (!type) return 'bg-gray-500'

      return (
        {
          BY_PASS: 'bg-black',
          FAULTY: 'bg-blue-900',
          MULTI_STEP: 'bg-blue-400',
        }[type] || 'bg-gray-500'
      )
    },
    formatType(type: string) {
      if (type === 'BY_PASS') return 'Bypass'
      if (type === 'MULTI_STEP') return 'Multi-Step'
      return type
    },
    startSolving() {
      this.$router.push(`/solve/${this.puzzle.id}`)
    },
    async fetchPuzzleDetails() {
      try {
        const puzzleId = this.$route.params.id
        const puzzleIdString = Array.isArray(puzzleId) ? puzzleId[0] : puzzleId

        const response = await apiClient.get(`/puzzles/${puzzleIdString}`)
        this.puzzle = response.data

        this.loadInstructionsAndTips()
        this.loadSolutions()
      } catch (error) {
        console.error('Error fetching puzzle details:', error)
      }
    },
    loadInstructionsAndTips() {
      const typeKey = this.puzzle.type.toUpperCase().replace('-', '_')

      if (typeKey === 'BY_PASS' || typeKey === 'BYPASS') {
        this.instructions = [
          "Try to be smart about your prompt --> don't just think like a developer",
          'Look at the responses from the LLM. Maybe you making a obvious mistake',
          'Make sure to check the code before testing because It may be wrong or not entirely the assignment',
        ]

        this.tips = [
          'Think about how to loop through numbers and check conditions.',
          'Use conditional statements to determine when to print "Fizz", "Buzz", or "FizzBuzz".',
          'Consider edge cases such as negative numbers or non-integer inputs.',
        ]
      } else if (typeKey === 'FAULTY') {
        this.instructions = [
          'Identify the issue in the provided code',
          "Fix only what's necessary without rewriting everything",
          'Test your solution with different inputs',
        ]

        this.tips = [
          'Look for syntax errors and logical flaws',
          'Check edge cases where the code might fail',
          'Consider optimization opportunities',
        ]
      } else if (typeKey === 'MULTI_STEP') {
        this.instructions = [
          'Break down the problem into manageable parts',
          'Solve each step before moving to the next',
          'Make sure your solution handles all requirements',
        ]

        this.tips = [
          'Start with the simplest part of the problem',
          'Test each step thoroughly before combining',
          'Consider how the steps interact with each other',
        ]
      }
    },
    async loadSolutions() {
      try {
        this.solutions = [
          {
            author: 'Alice',
            description: 'A well-structured solution using a simple for loop.',
          },
          {
            author: 'Bob',
            description: 'An optimized solution that reduces the number of prints.',
          },
          {
            author: 'Charlie',
            description: 'A creative approach using recursion.',
          },
        ]
      } catch (error) {
        console.error('Error fetching solutions:', error)
      }
    },
  },
  mounted() {
    this.fetchPuzzleDetails()
  },
})
</script>
