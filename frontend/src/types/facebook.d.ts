interface FacebookLoginResponse {
  authResponse?: {
    accessToken: string
    expiresIn: number
    signedRequest: string
    userID: string
  }
  status: string
}

interface FacebookAPIResponse {
  name?: string
  picture?: {
    data: {
      url: string
    }
  }
  error?: {
    message: string
    type: string
    code: number
    fbtrace_id: string
  }
}

interface FacebookSDK {
  init(params: { appId: string; cookie?: boolean; xfbml?: boolean; version: string }): void
  login(callback: (response: FacebookLoginResponse) => void, options?: { scope: string }): void
  logout(callback: () => void): void
  api(path: string, params: object, callback: (response: FacebookAPIResponse) => void): void
  AppEvents: {
    logPageView(): void
  }
}

declare global {
  interface Window {
    FB: FacebookSDK
  }
}
