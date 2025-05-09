<template>
  <header class="bg-white shadow p-4 flex justify-between items-center relative">
    <router-link to="/" class="no-underline">
      <h1 class="text-xl font-semibold text-black">Promptmaster</h1>
    </router-link>
    <nav>
      <ul class="flex space-x-2 list-none p-0">
        <li>
          <a href="/puzzle" class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl"
            >Puzzles</a
          >
        </li>
        <li>
          <a href="/vs" class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl">VS</a>
        </li>
        <li>
          <a href="#" class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl"
            >Ranking</a
          >
        </li>
        <li>
          <a href="#" class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl"
            >Community</a
          >
        </li>
        <li>
          <a href="#" class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl"
            >Contact</a
          >
        </li>
        <li>
          <a href="#" class="text-gray-600 no-underline hover:bg-gray-200 p-4 rounded-xl">About</a>
        </li>
      </ul>
    </nav>
    <div class="flex space-x-4 items-center relative">
      <i class="fas fa-bell text-gray-600"></i>
      <i class="fas fa-cog text-gray-600"></i>
      <div class="relative">
        <i class="fas fa-user-circle text-gray-600 cursor-pointer" @click="toggleDropdown"></i>
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
