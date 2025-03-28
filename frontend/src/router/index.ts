import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/Home.vue'
import PrivacyPolicy from '../views/PrivacyPolicy.vue'
import TermsOfService from '../views/TermsOfService.vue'
import PuzzleOverview from '../views/PuzzleOverview.vue'
import Test from '../views/Test.vue'
import Lobby from '../views/Lobby.vue'
import SignUp from '../views/SignUp.vue'

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
    {
      path: '/test',
      name: 'test',
      component: Test,
    },
    {
      path: '/vs',
      name: 'vs',
      component: Lobby,
    },
    {
      path: '/signup',
      name: 'signup',
      component: SignUp,
    },
  ],
})

export default router
