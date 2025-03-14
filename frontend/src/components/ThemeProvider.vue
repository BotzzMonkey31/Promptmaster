<template>
  <div :class="{ dark: isDarkMode }">
    <slot />
  </div>
</template>

<script lang="ts">
export default {
  data() {
    return {
      isDarkMode: false,
    }
  },
  mounted() {
    const userPreference = localStorage.getItem('theme')
    if (userPreference) {
      this.isDarkMode = userPreference === 'dark'
    }
  },
  methods: {
    toggleTheme() {
      this.isDarkMode = !this.isDarkMode
      localStorage.setItem('theme', this.isDarkMode ? 'dark' : 'light')
    },
  },
}
</script>
