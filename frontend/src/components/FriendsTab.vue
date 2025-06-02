<template>
  <div class="bg-white shadow rounded-lg py-4 px-6 mb-4">
    <div class="flex justify-between items-center mb-4">
      <div>
        <h3 class="text-lg font-semibold">Friend List</h3>
        <div class="flex items-center">
          <div class="text-xs text-gray-500 mt-1">
            <span class="text-green-500 font-medium">{{ onlineFriendsCount }}</span> online of {{ friends.length }} total
          </div>
          <button
            @click="refreshData"
            class="ml-2 text-xs text-blue-500 hover:text-blue-700"
            title="Refresh"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
      </div>
      <button
        @click="showFriendModal = !showFriendModal"
        class="bg-blue-500 text-white px-3 py-1 rounded text-sm hover:bg-blue-600 transition flex items-center"
      >
        <span class="mr-1">Add Friend</span>
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
      </button>
    </div>


    <div class="friends-list-container h-64 overflow-y-auto pr-2">
      <!-- Friends list -->
      <div v-if="loading" class="text-gray-500 text-center py-4">
        Loading friends...
      </div>
      <div v-else-if="friends.length === 0" class="text-gray-500 text-center py-4">
        No friends found. Add some friends to play against!
      </div>
      <ul v-else class="space-y-2">
        <li
          v-for="friend in friends"
          :key="friend.id"
          class="flex justify-between items-center py-2 border-b last:border-b-0"
        >
        <div class="flex items-center">
          <img
            :src="friend.picture || defaultAvatar"
            class="w-8 h-8 rounded-full mr-3"
            alt="Avatar"
            @error="handleImageError"
          />
          <div>
            <span class="font-medium">{{ friend.username }}</span>
            <div class="text-xs text-gray-500">
              <span
                :class="{
                  'text-green-500': isOnline(friend.id),
                  'text-gray-400': !isOnline(friend.id)
                }"
                class="mr-2"
              >
                {{ isOnline(friend.id) ? 'Online' : 'Offline' }}
              </span>
              <span>ELO: {{ friend.elo || 0 }}</span>
            </div>
          </div>
        </div>
        <div class="flex space-x-2">
          <button
            v-if="isOnline(friend.id)"
            @click="challengeFriend(friend)"
            class="bg-green-500 text-white px-2 py-1 rounded text-sm hover:bg-green-600 transition"
            :disabled="challengingSomeone"
          >
            Challenge
          </button>
          <button
            @click="removeFriend(friend)"
            class="text-red-500 hover:text-red-700"
            title="Remove Friend"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </li>
    </ul>

    <!-- Friend requests -->
    <div v-if="friendRequests.length > 0" class="mt-4 pt-4 border-t">
      <h4 class="text-md font-semibold mb-2">Friend Requests <span class="bg-red-500 text-white rounded-full px-2 py-0.5 text-xs ml-1">{{ friendRequests.length }}</span></h4>
      <ul class="space-y-2">
        <li
          v-for="request in friendRequests"
          :key="request.id"
          class="flex justify-between items-center py-2 border-b last:border-b-0"
        >
          <div class="flex items-center">
            <img
              :src="request.user.picture || defaultAvatar"
              class="w-8 h-8 rounded-full mr-3"
              alt="Avatar"
              @error="handleImageError"
            />
            <span>{{ request.user.username }}</span>
          </div>
          <div class="flex space-x-2">
            <button
              @click="acceptFriendRequest(request)"
              class="bg-green-500 text-white px-2 py-1 rounded text-xs hover:bg-green-600 transition"
            >
              Accept
            </button>
            <button
              @click="declineFriendRequest(request)"
              class="bg-gray-300 text-gray-700 px-2 py-1 rounded text-xs hover:bg-gray-400 transition"
            >
              Decline
            </button>
          </div>
        </li>
      </ul>
    </div>

    <!-- Add friend modal -->
    <div v-if="showFriendModal" class="fixed inset-0 flex items-center justify-center z-50">
      <div class="absolute inset-0 bg-black bg-opacity-50" @click="showFriendModal = false"></div>
      <div class="bg-white p-6 rounded-lg shadow-lg z-10 w-full max-w-md">
        <h3 class="text-lg font-semibold mb-4">Add Friend</h3>
        <div class="mb-4">
          <label for="username" class="block text-sm font-medium text-gray-700 mb-1">Username</label>
          <input
            type="text"
            id="username"
            v-model="searchUsername"
            placeholder="Search for username..."
            class="w-full border rounded-md px-3 py-2 focus:outline-none focus:ring focus:border-blue-300"
          />
        </div>

        <div v-if="searchResults.length > 0" class="mb-4">
          <h4 class="text-sm font-medium text-gray-700 mb-2">Search Results</h4>
          <ul class="max-h-60 overflow-y-auto">
            <li
              v-for="user in searchResults"
              :key="user.id"
              class="flex justify-between items-center py-2 border-b"
            >
              <div class="flex items-center">
                <img
                  :src="user.picture || defaultAvatar"
                  class="w-8 h-8 rounded-full mr-2"
                  alt="Avatar"
                  @error="handleImageError"
                />
                <span>{{ user.username }}</span>
              </div>
              <button
                @click="sendFriendRequest(user.id)"
                class="bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600 transition"
                :disabled="user.requestSent"
              >
                {{ user.requestSent ? 'Sent' : 'Add' }}
              </button>
            </li>
          </ul>
        </div>

        <div class="flex justify-end">
          <button
            @click="searchUsers"
            class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 transition mr-2"
          >
            Search
          </button>
          <button
            @click="showFriendModal = false"
            class="bg-gray-300 text-gray-800 px-4 py-2 rounded hover:bg-gray-400 transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>

    <!-- Remove friend confirmation modal -->
    <div v-if="showRemoveFriendModal" class="fixed inset-0 flex items-center justify-center z-50">
      <div class="absolute inset-0 bg-black bg-opacity-50" @click="cancelRemoveFriend"></div>
      <div class="bg-white p-6 rounded-lg shadow-lg z-10 max-w-md w-full">
        <h3 class="text-lg font-semibold mb-2">Remove Friend</h3>
        <p class="text-gray-700 mb-6">
          Are you sure you want to remove <strong>{{ friendToRemove?.username }}</strong> from your friends?
        </p>
        <div class="flex justify-end space-x-2">
          <button
            @click="cancelRemoveFriend"
            class="bg-gray-300 text-gray-800 px-4 py-2 rounded hover:bg-gray-400 transition"
          >
            Cancel
          </button>
          <button
            @click="confirmRemoveFriend"
            class="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600 transition"
          >
            Remove
          </button>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import apiClient from '../services/api';

export default defineComponent({
  name: 'FriendsTab',
  props: {
    currentUser: {
      type: Object,
      required: true
    },
    stompClient: {
      type: [Object, null],
      required: true
    },
    challengingSomeone: {
      type: Boolean,
      default: false
    },
    onlinePlayers: {
      type: Array as () => Array<{userId: string | number}>,
      default: () => []
    }
  },
  data() {
    return {
      friends: [] as any[],
      friendRequests: [] as any[],
      loading: true,
      showFriendModal: false,
      showRemoveFriendModal: false,
      friendToRemove: null as any,
      searchUsername: '',
      searchResults: [] as any[],
      friendSubscription: null as any,
      refreshInterval: null as any,
      defaultAvatar: 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'
    };
  },
  computed: {

    onlineFriendsCount() {
      return this.friends.filter(friend => this.isOnline(friend.id)).length;
    }
  },

  created() {
    this.loadFriends();
    this.loadFriendRequests();
    this.setupSocketSubscriptions();
  },

  beforeUnmount() {
    // Clean up any subscriptions if needed
    if (this.friendSubscription) {
      try {
        this.friendSubscription.unsubscribe();
      } catch (e) {
        console.error('Error unsubscribing from friend notifications:', e);
      }
    }
  },

  watch: {
    // Watch for changes in online players to update friend status without reloading
    onlinePlayers: {
      handler() {
        // This will trigger the computed property onlineFriendsCount to update
        // Without loading the whole list again
      },
      deep: true
    },

    // Deep watch for stompClient connection status to reload data when reconnected
    'stompClient.connected': function(newVal) {
      if (newVal === true) {
        this.loadFriendsQuietly();
        this.loadFriendRequests();
      }
    }
  },
  methods: {
    setupSocketSubscriptions() {
      if (!this.stompClient) {
        console.warn('Cannot setup friend websocket subscriptions - stompClient is null');
        return;
      }

      if (this.stompClient.connected && this.currentUser?.id) {
        // Subscribe to friend-related messages
        this.friendSubscription = this.stompClient.subscribe(
          `/user/${this.currentUser.id}/queue/friend`,
          (message: { body: string }) => {
            try {
              const friendMessage = JSON.parse(message.body);
              this.handleFriendMessage(friendMessage);
            } catch (e) {
              console.error('Error processing friend message:', e);
              this.$emit('notification', {
                type: 'error',
                message: 'Error processing notification. Please refresh the page.'
              });
            }
          }
        );

        // If we're reconnecting, refresh the data once
        this.loadFriendsQuietly();
        this.loadFriendRequests();
      } else {
        console.warn('Cannot setup friend websocket subscriptions - client not connected or user missing');

        // Add a one-time check after a delay to handle race conditions with WebSocket connections
        setTimeout(() => {
          if (this.stompClient?.connected && this.currentUser?.id && !this.friendSubscription) {
            this.setupSocketSubscriptions();
          }
        }, 2000);
      }
    },
    handleFriendMessage(message: any) {
      switch (message.type) {
        case 'FRIEND_REQUEST':
          // Instead of full refresh, we could just add the request if we have the complete data
          if (message.userId && message.username && message.userPicture) {
            // Check if this request is already in the list
            const existingRequest = this.friendRequests.find(r =>
              r.user && r.user.id === message.userId
            );

            if (!existingRequest) {
              // Add the new request to the list without full refresh
              this.friendRequests.push({
                id: message.friendshipId,
                user: {
                  id: message.userId,
                  username: message.username,
                  picture: message.userPicture
                },
                status: 'PENDING'
              });
            }
          } else {
            // Fallback to API call if we don't have complete data
            this.loadFriendRequests();
          }

          this.$emit('notification', {
            type: 'info',
            message: `<strong>${message.username}</strong> sent you a friend request`
          });
          break;

        case 'FRIEND_REQUEST_ACCEPTED':
        case 'FRIEND_ADDED':
          // Use the quiet loading method to prevent flickering
          this.loadFriendsQuietly();
          this.$emit('notification', {
            type: 'info',
            message: `<strong>${message.friendUsername}</strong> is now your friend`
          });
          break;

        case 'FRIEND_REMOVED':
          // If we have the user ID, we can remove them directly without a full refresh
          if (message.userId) {
            this.friends = this.friends.filter(f => f.id !== message.userId);
          } else {
            // Fallback to API call
            this.loadFriendsQuietly();
          }

          this.$emit('notification', {
            type: 'info',
            message: `<strong>${message.username}</strong> has removed you from their friends list`
          });
          break;
      }
    },
    async loadFriends() {
      try {
        this.loading = true;
        const response = await apiClient.get(`/friendships/${this.currentUser.id}/friends`);
        this.friends = response.data.data || [];
      } catch (error) {
        console.error('Error loading friends:', error);
      } finally {
        this.loading = false;
      }
    },
    async loadFriendsQuietly() {
      try {
        const response = await apiClient.get(`/friendships/${this.currentUser.id}/friends`);
        // Get the new friends list
        const newFriends = response.data.data || [];

        // Instead of replacing the entire array, update entries individually to preserve reactivity
        // This approach minimizes UI updates and prevents flickering

        // Remove friends that no longer exist
        this.friends = this.friends.filter(friend =>
          newFriends.some((newFriend: any) => newFriend.id === friend.id)
        );

        // Update or add new friends
        newFriends.forEach((newFriend: any) => {
          const existingFriend = this.friends.find(f => f.id === newFriend.id);
          if (existingFriend) {
            // Update existing friend properties if needed
            if (existingFriend.username !== newFriend.username ||
                existingFriend.picture !== newFriend.picture ||
                existingFriend.elo !== newFriend.elo) {
              Object.assign(existingFriend, newFriend);
            }
          } else {
            // Add new friend
            this.friends.push(newFriend);
          }
        });
      } catch (error) {
        console.error('Error loading friends quietly:', error);
      }
    },
    async loadFriendRequests() {
      try {
        const response = await apiClient.get(`/friendships/${this.currentUser.id}/requests`);
        this.friendRequests = response.data.data || [];
      } catch (error) {
        console.error('Error loading friend requests:', error);
      }
    },
    isOnline(userId: number) {
      return this.onlinePlayers.some(player => Number(player.userId) === userId);
    },
    challengeFriend(friend: any) {
      this.$emit('challenge-friend', friend);
    },
    handleImageError(event: Event) {
      const target = event.target as HTMLImageElement;
      target.src = this.defaultAvatar;
    },
    refreshData() {
      // Manual refresh - load friends without setting loading state if already loaded
      if (this.friends.length > 0 && !this.loading) {
        this.loadFriendsQuietly();
      } else {
        this.loadFriends();
      }
      this.loadFriendRequests();
    },
    async searchUsers() {
      if (!this.searchUsername.trim()) return;

      try {
        const response = await apiClient.get('/users/search', {
          params: { query: this.searchUsername }
        });

        // Filter out current user and existing friends
        this.searchResults = (response.data.data || [])
          .filter((user: any) => user.id !== this.currentUser.id)
          .filter((user: any) => !this.friends.some((friend: any) => friend.id === user.id))
          .map((user: any) => ({ ...user, requestSent: false }));
      } catch (error) {
        console.error('Error searching users:', error);
      }
    },
    async sendFriendRequest(friendId: number) {
      try {
        await apiClient.post('/friendships/request', {
          userId: this.currentUser.id,
          friendId
        });

        // Mark as sent in UI
        const userIndex = this.searchResults.findIndex(user => user.id === friendId);
        if (userIndex !== -1) {
          this.searchResults[userIndex].requestSent = true;
        }

        this.$emit('notification', {
          type: 'info',
          message: 'Friend request sent'
        });
      } catch (error: any) {
        console.error('Error sending friend request:', error);
        this.$emit('notification', {
          type: 'error',
          message: error.response?.data?.message || 'Failed to send friend request'
        });
      }
    },
    async acceptFriendRequest(request: any) {
      try {
        await apiClient.post(`/friendships/${request.id}/accept`, {
          userId: this.currentUser.id
        });

        // Remove from requests
        this.friendRequests = this.friendRequests.filter(r => r.id !== request.id);

        // Add the friend directly to the list if we have all needed data
        if (request.user && request.user.id && request.user.username) {
          const newFriend = {
            id: request.user.id,
            username: request.user.username,
            picture: request.user.picture || this.defaultAvatar,
            elo: request.user.elo || 0
          };

          // Check if this user is already in the friends list
          const existingFriendIndex = this.friends.findIndex(f => f.id === newFriend.id);

          if (existingFriendIndex === -1) {
            // Add as a new friend
            this.friends.push(newFriend);
          }
        } else {
          // If we don't have all the data, fall back to the API
          this.loadFriendsQuietly();
        }

        this.$emit('notification', {
          type: 'info',
          message: `You are now friends with <strong>${request.user.username}</strong>`
        });
      } catch (error) {
        console.error('Error accepting friend request:', error);
      }
    },
    async declineFriendRequest(request: any) {
      try {
        await apiClient.post(`/friendships/${request.id}/decline`, {
          userId: this.currentUser.id
        });

        // Remove from requests
        this.friendRequests = this.friendRequests.filter(r => r.id !== request.id);

        this.$emit('notification', {
          type: 'info',
          message: `Friend request from ${request.user.username} declined`
        });
      } catch (error) {
        console.error('Error declining friend request:', error);
      }
    },
    removeFriend(friend: any) {
      this.friendToRemove = friend;
      this.showRemoveFriendModal = true;
    },

    async confirmRemoveFriend() {
      if (!this.friendToRemove) return;

      try {
        await apiClient.delete(`/friendships/${this.currentUser.id}/remove/${this.friendToRemove.id}`);

        // Remove from friends list
        this.friends = this.friends.filter(f => f.id !== this.friendToRemove.id);

        this.$emit('notification', {
          type: 'info',
          message: `Removed ${this.friendToRemove.username} from your friends`
        });

        // Reset and hide modal
        this.friendToRemove = null;
        this.showRemoveFriendModal = false;
      } catch (error) {
        console.error('Error removing friend:', error);
      }
    },

    cancelRemoveFriend() {
      this.friendToRemove = null;
      this.showRemoveFriendModal = false;
    }
  }
});
</script>
