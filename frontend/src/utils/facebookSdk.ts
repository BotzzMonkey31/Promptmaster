declare global {
  interface Window {
    fbAsyncInit: () => void
    FB: any
  }
}
let isFBInitialized = false

export const initializeFacebookSdk = (): Promise<void> => {
  return new Promise((resolve) => {
    window.fbAsyncInit = () => {
      window.FB.init({
        appId: '1542890363047814', // Replace with your actual Facebook App ID
        cookie: true,
        xfbml: true,
        version: 'v21.0', // Use the latest version available
      })

      window.FB.AppEvents.logPageView()
      isFBInitialized = true
      resolve()
    }

    // Load the Facebook SDK script
    const d = document
    const s = 'script'
    const id = 'facebook-jssdk'
    let js: HTMLScriptElement | null = d.getElementById(id) as HTMLScriptElement
    if (!js) {
      js = d.createElement(s) as HTMLScriptElement
      js.id = id
      js.src = 'https://connect.facebook.net/en_US/sdk.js'
      js.async = true
      js.defer = true
      const fjs = d.getElementsByTagName(s)[0]
      fjs.parentNode?.insertBefore(js, fjs)
    }
  })
}

export const isFacebookSdkInitialized = (): boolean => isFBInitialized
