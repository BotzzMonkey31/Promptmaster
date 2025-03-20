import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/Home.vue'
import PrivacyPolicy from '@/views/PrivacyPolicy.vue'
import TermsOfService from '@/views/TermsOfService.vue'
import PuzzleOverview from '@/views/PuzzleOverview.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/privacy',
      name: 'privacy',
      component: PrivacyPolicy,
    },
    {
      path: '/terms',
      name: 'terms',
      component: TermsOfService,
    },
    {
      path: '/puzzle',
      name: 'puzzle',
      component: PuzzleOverview,
    },
  ],
})

export default router
