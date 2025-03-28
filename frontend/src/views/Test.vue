<script setup lang="ts">
import { GoogleSignInButton, type CredentialResponse } from 'vue3-google-signin'
// Define the component name
defineOptions({
  name: 'TestView',
})

// handle success event
const handleLoginSuccess = (response: CredentialResponse) => {
  const { credential } = response
  console.log('Access Token', credential)
}

// handle an error event
const handleLoginError = () => {
  console.error('Login failed')
}
</script>

<template>
  <GoogleSignInButton @success="handleLoginSuccess" @error="handleLoginError"></GoogleSignInButton>
</template>
