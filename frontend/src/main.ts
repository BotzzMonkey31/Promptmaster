import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'uno.css'
import '@fortawesome/fontawesome-free/css/all.min.css'

import App from './App.vue'
import router from './router'

import './assets/main.css'

import GoogleSignInPlugin from 'vue3-google-signin'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.use(GoogleSignInPlugin, {
  clientId: '60755716227-7iumc2pctg2sq3krufa0q53hfqftfr35.apps.googleusercontent.com',
})
app.mount('#app')
