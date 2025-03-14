<template>
  <header class="border-b">
    <div class="container mx-auto px-4 py-3 flex items-center justify-between">
      <div class="flex items-center gap-8">
        <router-link to="/" class="flex items-center gap-2">
          <div class="relative w-6 h-6">
            <div class="absolute inset-0 border-2 border-gray-700 rotate-45"></div>
            <div
              class="absolute inset-[3px] flex items-center justify-center text-[10px] font-bold"
            >
              P
            </div>
          </div>
          <span class="font-semibold text-lg">Promptmaster</span>
          <span class="text-xs text-muted-foreground">by EBSS</span>
        </router-link>
        <nav class="hidden md:flex items-center gap-6">
          <router-link to="/puzzles" class="text-sm font-medium">Puzzles</router-link>
          <router-link to="/vs" class="text-sm font-medium">VS</router-link>
          <router-link to="/ranking" class="text-sm font-medium">Ranking</router-link>
          <router-link to="/community" class="text-sm font-medium">Community</router-link>
          <router-link to="/contact" class="text-sm font-medium">Contact</router-link>
          <router-link to="/about" class="text-sm font-medium">About</router-link>
        </nav>
      </div>
      <div class="flex items-center gap-4">
        <button>
          <BellIcon class="h-5 w-5" />
        </button>
        <button>
          <CogIcon class="h-5 w-5" />
        </button>
        <button>
          <UserIcon class="h-5 w-5" />
        </button>
      </div>
    </div>
  </header>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { BellIcon, CogIcon, UserIcon } from '@heroicons/vue/24/outline'

export default defineComponent({
  name: 'AppHeader',
  components: {
    BellIcon,
    CogIcon,
    UserIcon,
  },
})
</script>
