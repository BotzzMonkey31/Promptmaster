<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">
      <h2 class="text-2xl font-semibold text-center mb-6">Our puzzles</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div
          v-for="puzzle in puzzles"
          :key="puzzle.title"
          class="bg-white p-6 shadow rounded-lg flex flex-col hover:shadow-xl transition-shadow duration-300"
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
