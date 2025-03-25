<!-- <template>
  <button @click="loginWithFacebook">Login with Facebook</button>
  <p>{{ status }}</p>
</template>
<script setup lang="ts">
import { defineComponent } from 'vue'

defineComponent({
  name: 'TestComponent',
})
import { onMounted, ref } from 'vue'

declare global {
  interface Window {
    fbAsyncInit: () => void
    FB: any
  }
}
const status = ref<string>('')
onMounted(() => {
  window.fbAsyncInit = () => {
    window.FB.init({
      appId: 'Your_APP_ID', // Replace YOUR_APP_ID with your actual Facebook App ID
      cookie: true,
      xfbml: true,
      version: 'v21.0', // Use the latest version available
    })

    window.FB.AppEvents.logPageView()
  }
  ;(function (d, s, id) {
    let js,
      fjs = d.getElementsByTagName(s)[0]
    if (d.getElementById(id)) {
      return
    }
    js = d.createElement(s)
    js.id = id
    js.src = 'https://connect.facebook.net/en_US/sdk.js'
    fjs.parentNode.insertBefore(js, fjs)
  })(document, 'script', 'facebook-jssdk')
})
function loginWithFacebook(): void {
  window.FB.login(
    (response: any) => {
      if (response.authResponse) {
        console.log('Welcome! Fetching your information.... ')
        window.FB.api('/me', { fields: 'name,email' }, (response: any) => {
          console.log(`Good to see you, ${response.name}.`)
          status.value = `Logged in as ${response.name}`
          console.log(response)
        })
      } else {
        console.log('User cancelled login or did not fully authorize.')
        status.value = 'Login failed'
      }
    },
    { scope: 'email, public_profile' },
  ) // only these two scopes can be used for testing before app is reviewed
}
</script> -->

<template>
  <p>header</p>
</template>

<script lang="ts">
export default {
  name: 'AppHeader',
}
</script>
