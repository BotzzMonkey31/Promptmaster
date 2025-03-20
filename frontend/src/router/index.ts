import { createRouter, createWebHistory } from 'vue-router'
<<<<<<< Updated upstream
import HomeView from '../views/HomeView.vue'
=======
import HomeView from '../views/Home.vue'
import PrivacyPolicy from '@/views/PrivacyPolicy.vue'
import TermsOfService from '@/views/TermsOfService.vue'
import PuzzleOverview from '@/views/PuzzleOverview.vue'
>>>>>>> Stashed changes

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/puzzle',
      name: 'puzzle',
      component: PuzzleOverview,
    },
  ],
})

export default router
