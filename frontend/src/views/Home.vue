<template>
  <div class="bg-gray-100 min-h-screen">
    <header class="bg-white shadow p-4 flex justify-between items-center">
      <h1 class="text-xl font-semibold">Promptmaster</h1>
      <nav>
        <ul class="flex space-x-8 list-none">
          <li><a href="#" class="text-gray-600">Puzzles</a></li>
          <li><a href="#" class="text-gray-600">VS</a></li>
          <li><a href="#" class="text-gray-600">Ranking</a></li>
          <li><a href="#" class="text-gray-600">Community</a></li>
          <li><a href="#" class="text-gray-600">Contact</a></li>
          <li><a href="#" class="text-gray-600">About</a></li>
        </ul>
      </nav>
      <div class="flex space-x-4">
        <i class="fas fa-bell text-gray-600"></i>
        <i class="fas fa-cog text-gray-600"></i>
        <i class="fas fa-user-circle text-gray-600"></i>
      </div>
    </header>

    <main class="container mx-auto py-8">
      <h2 class="text-2xl font-semibold text-center mb-6">Our puzzles</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div
          v-for="puzzle in puzzles"
          :key="puzzle.title"
          class="bg-white p-6 shadow rounded-lg justify-center items-center"
        >
          <div class="bg-gray-300 w-full h-32 flex items-center justify-center text-gray-600">
            300 × 200
          </div>
          <h3 class="text-lg font-semibold mt-4">{{ puzzle.title }}</h3>
          <p class="text-gray-600 mt-2">{{ puzzle.description }}</p>
        </div>
      </div>

      <h2 class="text-2xl font-semibold text-center mt-10">Global Rankings</h2>
      <div class="bg-white p-6 shadow rounded-lg mt-4">
        <ul>
          <li
            v-for="(rank, index) in rankings"
            :key="index"
            class="flex justify-between items-center py-2 border-b"
          >
            <div class="flex items-center">
              <img :src="rank.avatar" class="w-8 h-8 rounded-full mr-3" alt="Avatar" />
              <span>{{ rank.name }} - {{ rank.rank }}</span>
            </div>
            <img :src="rank.badge" class="w-8 h-8" alt="Badge" />
          </li>
        </ul>
      </div>
    </main>

    <footer class="bg-black text-white text-center p-4 mt-10">
      <p>&copy; 2025 EBSS. All rights reserved.</p>
      <div class="flex justify-center space-x-4 mt-2">
        <i class="fab fa-youtube"></i>
        <i class="fab fa-facebook"></i>
        <i class="fab fa-twitter"></i>
        <i class="fab fa-instagram"></i>
        <i class="fab fa-linkedin"></i>
      </div>
    </footer>
  </div>
</template>

<script lang="ts">
export default {
  name: 'HomeView',
  data() {
    return {
      puzzles: [
        {
          title: 'VS Mode',
          description:
            'Challenge your friends or random opponents in competitive duels. Matchmaking is based on rank. You can get any form of puzzle at random.',
        },
        {
          title: 'BYPASS',
          description:
            'Test your skills. You must try to bypass certain restrictions set on the prompt.',
        },
        {
          title: 'Step-by-Step Puzzles',
          description: 'Solve multi-step challenges where you build up the code step by step.',
        },
        {
          title: 'Faulty Code Generator',
          description: 'Identify and fix intentional bugs made by the LLM.',
        },
      ],
      rankings: [
        { name: 'Alice', rank: 'Master', avatar: 'avatar1.jpg', badge: 'badge1.jpg' },
        { name: 'Eve', rank: 'Emerald', avatar: 'avatar2.jpg', badge: 'badge2.jpg' },
        { name: 'Bob', rank: 'Gold', avatar: 'avatar3.jpg', badge: 'badge3.jpg' },
        { name: 'Charlie', rank: 'Silver', avatar: 'avatar4.jpg', badge: 'badge4.jpg' },
        { name: 'David', rank: 'Bronze', avatar: 'avatar5.jpg', badge: 'badge5.jpg' },
      ],
    }
  },
}
</script>

<style>
body {
  font-family: Arial, sans-serif;
}
</style>
