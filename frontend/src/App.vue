<script setup lang="ts">
import HeaderComponent from './components/Header.vue'
import FooterComponent from './components/Footer.vue'
import { RouterView } from 'vue-router'
</script>

<template>
  <HeaderComponent />
  <RouterView />
  <FooterComponent />
</template>
