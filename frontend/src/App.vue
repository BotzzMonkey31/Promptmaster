<template>
  <HeaderComponent />
  <RouterView />
  <FooterComponent />
</template>

<script lang="ts">
import HeaderComponent from './components/Header.vue'
import FooterComponent from './components/Footer.vue'
import { RouterView } from 'vue-router'

export default {
  name: 'App',
  components: {
    HeaderComponent,
    RouterView,
    FooterComponent,
  },
}
</script>
