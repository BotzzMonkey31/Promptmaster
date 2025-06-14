<template>
  <header class="bg-white shadow p-4">
    <div class="flex items-center w-full">
      <div class="w-40 flex justify-start">
        <router-link
          to="/"
          class="no-underline transition-colors duration-200"
        >
          <h1 class="text-xl font-semibold" :class="{ 'text-black ___': $route.path === '/', 'text-black': $route.path !== '/' }">
            Promptmaster
          </h1>
        </router-link>
      </div>
      <nav class="flex-1 flex justify-center">
        <ul class="flex space-x-2 list-none p-0">
          <li>
            <router-link
              to="/puzzle"
              class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl transition-colors duration-200"
              :class="{ 'bg-blue-100 text-blue-700 font-semibold': $route.path.startsWith('/puzzle') }"
            >
              Puzzles
            </router-link>
          </li>
          <li>
            <router-link
              to="/vs"
              class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl transition-colors duration-200"
              :class="{ 'bg-blue-100 text-blue-700 font-semibold': $route.path.startsWith('/vs') }"
            >
              VS
            </router-link>
          </li>
          <li>
            <router-link
              to="/ranking"
              class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl transition-colors duration-200"
              :class="{ 'bg-blue-100 text-blue-700 font-semibold': $route.path.startsWith('/ranking') }"
            >
              Ranking
            </router-link>
          </li>
        </ul>
      </nav>

      <div class="w-40 flex justify-end">
      <div class="relative">
        <i class="fas fa-user-circle text-gray-600 cursor-pointer text-2xl hover:text-blue-600 transition-colors duration-200" @click="toggleDropdown"></i>
        <div
          v-if="dropdownOpen"
          class="absolute right-0 mt-2 w-48 bg-white shadow-lg rounded-md p-2"
        >
          <template v-if="user">
            <div class="flex items-center space-x-2 p-2 border-b">
              <img :src="user.picture" alt="Profile" class="w-10 h-10 rounded-full" />
              <span>{{ user.name }}</span>
            </div>
            <button @click="logout" class="w-full text-left px-4 py-2 hover:bg-gray-100">
              Logout
            </button>
          </template>
          <div v-else class="w-full">
            <GoogleSignInButton
              @success="handleLoginSuccess"
              @error="handleLoginError"
              class="w-full"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
  </header>
</template>

<script setup lang="ts">
defineOptions({
  name: 'HeaderView',
})
import { ref, onMounted } from 'vue'
import { GoogleSignInButton, type CredentialResponse } from 'vue3-google-signin'
import Cookies from 'js-cookie'
import { useRouter } from 'vue-router'

const router = useRouter()
const dropdownOpen = ref<boolean>(false)
const user = ref<any>(null)
const status = ref<string>('')

const toggleDropdown = () => {
  dropdownOpen.value = !dropdownOpen.value
}

const handleLoginSuccess = (response: CredentialResponse) => {
  const { credential } = response
  console.log('Access Token', credential)

  if (!credential) {
    console.error('No credential received');
    status.value = 'Login failed: No credential received';
    return;
  }

  const decoded = JSON.parse(atob(credential.split('.')[1]))
  const userData = {
    name: decoded.name,
    picture: decoded.picture,
    email: decoded.email,
  }

  Cookies.set('user', JSON.stringify(userData), { expires: 7 })
  Cookies.set('googleCredential', credential, { expires: 7 })

  user.value = userData
  status.value = `Logged in as ${decoded.name}`
  console.log('User info:', decoded)

  router.push('/signup')
}

const handleLoginError = () => {
  console.error('Login failed')
  status.value = 'Login failed'
}

const logout = () => {
  console.log('Logging out...')
  Cookies.remove('user')
  Cookies.remove('googleCredential')

  user.value = null
  status.value = ''
  dropdownOpen.value = false
}

onMounted(() => {
  const savedUser = Cookies.get('user')
  if (savedUser) {
    try {
      user.value = JSON.parse(savedUser)
      status.value = `Logged in as ${user.value.name}`
    } catch (error) {
      console.error('Error parsing saved user data:', error)
      Cookies.remove('user')
      Cookies.remove('googleCredential')
    }
  }
})
</script>
