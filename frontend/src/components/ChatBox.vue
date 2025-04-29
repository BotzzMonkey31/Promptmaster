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
      <div ref="messagesContainer" class="h-96 overflow-y-auto p-4 space-y-3">
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="flex"
          :class="message.isSelf ? 'justify-end' : 'justify-start'"
        >
          <div class="flex max-w-[80%]">
            <template v-if="!message.isSelf">
              <img
                :src="message.userPicture || defaultAvatar"
                class="w-10 h-10 rounded-full mr-2 flex-shrink-0"
                alt="User avatar"
                @error="handleImageError($event)"
              />
            </template>

            <div>
              <div
                class="p-2 rounded-lg mb-1"
                :class="
                  message.isSelf ? 'bg-blue-500 text-white self-end' : 'bg-gray-100 text-black'
                "
              >
                {{ message.content }}
              </div>
              <div
                class="text-xs text-gray-500 flex"
                :class="message.isSelf ? 'justify-end' : 'justify-start'"
              >
                <template v-if="!message.isSelf">
                  <span class="mr-2">{{ message.username }}</span>
                </template>
                <span>{{ formatTimestamp(message.timestamp) }}</span>
              </div>
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
import Cookies from 'js-cookie'

interface ChatMessage {
  username: string
  userPicture: string
  content: string
  timestamp: Date
  isSelf: boolean
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
      currentUser: {} as { name: string; picture: string },
      defaultAvatar: 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y',
    }
  },
  methods: {
    handleImageError(event: Event) {
      console.log('Image failed to load, using default avatar')
      const target = event.target as HTMLImageElement
      target.src = this.defaultAvatar
    },
    formatTimestamp(date: Date): string {
      return new Intl.DateTimeFormat('en', {
        hour: '2-digit',
        minute: '2-digit',
      }).format(date)
    },
    toggleChat() {
      this.isOpen = !this.isOpen
      if (this.isOpen) {
        this.unreadCount = 0
      }
    },
    async sendMessage() {
      if (!this.newMessage.trim() || !this.socket) return
      const userCookie = Cookies.get('user')
      if (!userCookie) return
      this.currentUser = JSON.parse(userCookie)
      const message = {
        type: 'CHAT',
        content: this.newMessage.trim(),
        username: this.currentUser.name,
        userPicture: this.currentUser.picture,
      }

      this.socket.send(JSON.stringify(message))

      this.newMessage = ''

      this.$nextTick(() => {
        const container = this.$refs.messagesContainer as HTMLElement
        container.scrollTop = container.scrollHeight
      })
    },
    initWebSocket() {
      const baseUrl = import.meta.env.VITE_API_BASE_URL.replace('http', 'ws');
      this.socket = new WebSocket(`${baseUrl}/chat`);

      this.socket.onmessage = (event) => {
        const message = JSON.parse(event.data);

        const isSelf = message.username === this.currentUser.name;

        this.messages.push({
          username: message.username,
          userPicture: message.userPicture,
          content: message.content,
          timestamp: new Date(),
          isSelf: isSelf,
        });

        if (!this.isOpen && !isSelf) {
          this.unreadCount++;
        }

        this.$nextTick(() => {
          const container = this.$refs.messagesContainer as HTMLElement;
          container.scrollTop = container.scrollHeight;
        });
      };

      this.socket.onclose = () => {
        console.log('WebSocket connection closed');
        setTimeout(() => this.initWebSocket(), 5000);
      };
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
