<template>
  <div class="fixed bottom-4 right-4 w-80">
    <div
      @click="toggleChat"
      class="bg-blue-600 text-white p-3 rounded-t-lg cursor-pointer flex justify-between items-center"
    >
      <span class="font-semibold">Global Chat</span>
      <span class="text-xs bg-red-500 rounded-full px-2" v-if="unreadCount">{{ unreadCount }}</span>
    </div>

    <div v-show="isOpen" class="bg-white border border-gray-200 border-t-0 rounded-b-lg shadow-lg">
      <div ref="messagesContainer" class="h-96 overflow-y-auto p-4 space-y-2">
        <div v-for="(message, index) in messages" :key="index" class="flex flex-col">
          <div class="flex items-start gap-2">
            <img :src="message.userPicture" class="w-6 h-6 rounded-full" alt="User avatar" />
            <div>
              <span class="text-xs text-gray-600">{{ message.username }}</span>
              <p class="bg-gray-100 rounded-lg p-2 inline-block">
                {{ message.content }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div class="border-t p-3">
        <div class="flex gap-2">
          <input
            v-model="newMessage"
            @keyup.enter="sendMessage"
            type="text"
            placeholder="Type a message..."
            class="flex-1 border rounded-lg px-3 py-2 focus:outline-none focus:border-blue-500"
          />
          <button
            @click="sendMessage"
            class="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
interface ChatMessage {
  username: string
  userPicture: string
  content: string
  timestamp: Date
}

export default {
  name: 'ChatBox',
  data() {
    return {
      isOpen: false,
      messages: [] as ChatMessage[],
      newMessage: '',
      unreadCount: 0,
      socket: null as WebSocket | null,
    }
  },
  methods: {
    toggleChat() {
      this.isOpen = !this.isOpen
      if (this.isOpen) {
        this.unreadCount = 0
      }
    },
    async sendMessage() {
      if (!this.newMessage.trim() || !this.socket) return

      const userData = JSON.parse(localStorage.getItem('user') || '{}')
      const message = {
        type: 'CHAT',
        content: this.newMessage.trim(),
        username: userData.name,
        userPicture: userData.picture,
      }

      this.socket.send(JSON.stringify(message))
      this.newMessage = ''
    },
    initWebSocket() {
      this.socket = new WebSocket('ws://localhost:8080/chat')

      this.socket.onmessage = (event) => {
        const message = JSON.parse(event.data)
        this.messages.push({
          username: message.username,
          userPicture: message.userPicture,
          content: message.content,
          timestamp: new Date(),
        })

        if (!this.isOpen) {
          this.unreadCount++
        }

        this.$nextTick(() => {
          const container = this.$refs.messagesContainer as HTMLElement
          container.scrollTop = container.scrollHeight
        })
      }

      this.socket.onclose = () => {
        console.log('WebSocket connection closed')
        setTimeout(() => this.initWebSocket(), 5000) // Reconnect after 5 seconds
      }
    },
  },
  mounted() {
    this.initWebSocket()
  },
  beforeUnmount() {
    if (this.socket) {
      this.socket.close()
    }
  },
}
</script>
