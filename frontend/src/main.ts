import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'uno.css'
import '@fortawesome/fontawesome-free/css/all.min.css'

import App from './App.vue'
import router from './router'

import './assets/main.css'

// Initialize Facebook SDK
declare global {
  interface Window {
    FB: {
      init: (params: { appId: string; cookie: boolean; xfbml: boolean; version: string }) => void
    }
    fbAsyncInit: () => void
  }
}

window.fbAsyncInit = function () {
  window.FB.init({
    appId: '1542890363047814',
    cookie: true,
    xfbml: true,
    version: 'v12.0',
  })
}
;(function (d: Document, s: string, id: string) {
  const fjs = d.getElementsByTagName(s)[0]
  if (d.getElementById(id)) return
  const js = d.createElement('script') as HTMLScriptElement
  js.id = id
  js.src = 'https://connect.facebook.net/en_US/sdk.js'
  fjs.parentNode!.insertBefore(js, fjs)
})(document, 'script', 'facebook-jssdk')

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
