<template>
  <header class="bg-white shadow p-4 flex justify-between items-center relative">
    <h1 class="text-xl font-semibold">Promptmaster</h1>
    <nav>
      <ul class="flex space-x-6 list-none p-0">
        <li><a href="/puzzle" class="text-gray-600">Puzzles</a></li>
        <li><a href="#" class="text-gray-600">VS</a></li>
        <li><a href="#" class="text-gray-600">Ranking</a></li>
        <li><a href="#" class="text-gray-600">Community</a></li>
        <li><a href="#" class="text-gray-600">Contact</a></li>
        <li><a href="#" class="text-gray-600">About</a></li>
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
          <button
            v-else
            @click="handleLogin"
            class="button w-full text-left px-4 py-2 hover:bg-gray-100"
          >
            Login with Facebook
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script lang="ts">
window.fbAsyncInit = () => {
  ;(window.FB as FacebookSDK).init({
    appId: '1542890363047814',
    cookie: true,
    xfbml: true,
    version: 'v19.0',
  })
  ;(window.FB as FacebookSDK).AppEvents.logPageView()
}
;(function (d: Document, s: string, id: string) {
  const fjs = d.getElementsByTagName(s)[0]
  if (d.getElementById(id)) {
    return
  }
  const js = d.createElement(s) as HTMLScriptElement
  js.id = id
  js.src = 'https://connect.facebook.net/en_US/all.js'
  js.async = true
  js.defer = true
  fjs.parentNode?.insertBefore(js, fjs)
})(document, 'script', 'facebook-jssdk')

export default {
  name: 'HeaderComponent',
  data() {
    return {
      user: null as { name: string; picture: string } | null,
      dropdownOpen: false,
      facebookAppId: '1542890363047814',
    }
  },
  async created() {
    window.fbAsyncInit = () => {
      ;(window.FB as FacebookSDK).init({
        appId: '1542890363047814',
        cookie: true,
        xfbml: true,
        version: 'v19.0',
      })
      ;(window.FB as FacebookSDK).AppEvents.logPageView()
    }
    const token = localStorage.getItem('jwtToken')
    if (token) {
      await this.fetchUser()
    }
  },
  methods: {
    handleLogin() {
      window.fbAsyncInit = () => {
        ;(window.FB as FacebookSDK).init({
          appId: '1542890363047814',
          cookie: true,
          xfbml: true,
          version: 'v19.0',
        })
        ;(window.FB as FacebookSDK).AppEvents.logPageView()
      }
      ;(window.FB as FacebookSDK).login(
        (response: FacebookLoginResponse) => {
          if (response.authResponse) {
            console.log('User logged in', response)
            const { accessToken } = response.authResponse
            localStorage.setItem('jwtToken', accessToken)
            this.fetchUser()
          } else {
            console.error('User cancelled login or did not fully authorize.')
          }
        },
        { scope: 'public_profile,email' }, // Request the necessary permissions
      )
    },
    async fetchUser() {
      try {
        ;(window.FB as FacebookSDK).api(
          '/me',
          { fields: 'name,picture' },
          (response: FacebookAPIResponse) => {
            if (response && !response.error) {
              this.user = {
                name: response.name || '',
                picture: response.picture?.data.url || '',
              }
            } else {
              console.error('Error fetching user data:', response.error)
            }
          },
        )
      } catch (error) {
        console.error('Error fetching user:', error)
      }
    },
    async logout() {
      ;(window.FB as FacebookSDK).logout(() => {
        localStorage.removeItem('jwtToken')
        this.user = null
        this.dropdownOpen = false
      })
    },
    toggleDropdown() {
      this.dropdownOpen = !this.dropdownOpen
    },
  },
}
</script>

<style scoped></style>
