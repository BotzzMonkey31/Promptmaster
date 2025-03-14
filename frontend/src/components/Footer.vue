<template>
  <footer class="bg-gray-900 text-white py-6">
    <div class="container mx-auto px-4">
      <div class="flex flex-col md:flex-row justify-between items-center">
        <div class="mb-4 md:mb-0">
          <p class="text-sm">© 2025 EBSS. All rights reserved.</p>
        </div>
        <div class="flex gap-6 mb-4 md:mb-0">
          <router-link to="/puzzles" class="text-sm">Puzzles</router-link>
          <router-link to="/vs" class="text-sm">VS</router-link>
          <router-link to="/community" class="text-sm">Community</router-link>
          <router-link to="/contact-us" class="text-sm">Contact us</router-link>
          <router-link to="/about" class="text-sm">About</router-link>
        </div>
        <div class="flex gap-4">
          <button class="text-white hover:text-white hover:bg-gray-800">
            <Youtube class="h-5 w-5" />
          </button>
          <button class="text-white hover:text-white hover:bg-gray-800">
            <Facebook class="h-5 w-5" />
          </button>
          <button class="text-white hover:text-white hover:bg-gray-800">
            <Twitter class="h-5 w-5" />
          </button>
          <button class="text-white hover:text-white hover:bg-gray-800">
            <Instagram class="h-5 w-5" />
          </button>
          <button class="text-white hover:text-white hover:bg-gray-800">
            <Linkedin class="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>
  </footer>
</template>

<script lang="ts">
import * as SI from 'simple-icons'

export default {
  name: 'AppFooter',
  components: {
    Youtube: SI.siYoutube,
    Facebook: SI.siFacebook,
    Twitter: SI.siX,
    Instagram: SI.siInstagram,
    Linkedin: SI.siAmazon,
  },
}
</script>
