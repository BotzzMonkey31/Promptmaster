<template>
  <div class="bg-gray-100 min-h-screen">
    <main class="container mx-auto py-8">
      <h2 class="text-2xl font-semibold text-center mb-6">Game Lobby</h2>
      <p class="text-center text-gray-600 mb-4">Find an opponent or invite a friend to a match!</p>

      <div v-if="gameNotification" class="bg-blue-100 border border-blue-300 text-blue-800 px-4 py-3 rounded relative mb-6">
        <span v-html="gameNotification.message"></span>
        <div v-if="gameNotification.type === 'challenge'" class="flex justify-center gap-4 mt-3">
          <button @click="acceptChallenge" class="bg-green-500 text-white px-4 py-2 rounded">Accept Challenge</button>
          <button @click="rejectChallenge" class="bg-red-500 text-white px-4 py-2 rounded">Decline</button>
        </div>
      </div>

      <div class="flex justify-center gap-4 mb-6">
        <button
          @click="findRandomOpponent"
          class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 transition"
          :disabled="findingOpponent"
        >
          {{ findingOpponent ? 'Searching...' : 'Find Random Opponent' }}
        </button>
        <button
          @click="toggleInviteModal"
          class="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600 transition"
        >
          Invite a Friend
        </button>
      </div>

      <div v-if="showInviteModal" class="fixed inset-0 flex items-center justify-center z-50">
        <div class="absolute inset-0 bg-black bg-opacity-50" @click="toggleInviteModal"></div>
        <div class="bg-white p-6 rounded-lg shadow-lg z-10 w-full max-w-md">
          <h3 class="text-lg font-semibold mb-4">Invite a Friend</h3>
          <p class="mb-4">Share this link with your friend:</p>
          <div class="flex">
            <input
              type="text"
              readonly
              :value="inviteLink"
              class="flex-1 border rounded-l px-3 py-2"
            />
            <button
              @click="copyInviteLink"
              class="bg-blue-500 text-white px-4 py-2 rounded-r"
            >
              Copy
            </button>
          </div>
          <p v-if="linkCopied" class="text-green-600 mt-2">Link copied to clipboard!</p>
          <button @click="toggleInviteModal" class="mt-4 w-full bg-gray-300 text-gray-800 px-4 py-2 rounded">Close</button>
        </div>
      </div>

      <div class="bg-white p-6 shadow rounded-lg mb-6">
        <h3 class="text-lg font-semibold mb-4">Available Players</h3>
        <div v-if="availablePlayers.length === 0" class="text-gray-500 text-center py-4">
          No players are currently available.
        </div>
        <ul v-else>
          <li
            v-for="player in availablePlayers"
            :key="player.userId"
            class="flex justify-between items-center py-3 border-b"
          >
            <div class="flex items-center">
              <img :src="player.picture || defaultAvatar" class="w-8 h-8 rounded-full mr-3" alt="Avatar" @error="handleImageError" />
              <span>{{ player.username }}</span>
              <span class="ml-2 text-sm text-gray-500">ELO: {{ player.elo }}</span>
            </div>
            <button
              @click="challengePlayer(player)"
              class="bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600 transition"
              :disabled="challengingSomeone"
            >
              Challenge
            </button>
          </li>
        </ul>
      </div>

      <div class="bg-white p-6 shadow rounded-lg">
        <h3 class="text-lg font-semibold mb-4">Your Rank</h3>
        <div v-if="error" class="text-red-500 mb-4">
          {{ error }}
        </div>
        <div v-else-if="loading" class="text-gray-500 mb-4">Loading user data...</div>
        <div v-else>
          <div v-if="user" class="flex items-center">
            <img :src="user.picture" class="w-10 h-10 rounded-full mr-3" alt="Avatar" @error="handleImageError" />
            <div>
              <p class="font-semibold">{{ user.username }}</p>
              <p>
                Rank: <span class="text-blue-500">{{ currentRank }}</span>
              </p>
              <p class="text-sm text-gray-600">ELO: {{ user.elo }}</p>
              <p class="text-sm text-gray-600">Country: {{ user.country || 'Unknown' }}</p>
            </div>
          </div>
          <div class="mt-4">
            <p class="text-sm text-gray-600">Progress to next rank: {{ progressToNextRank }}%</p>
            <div class="w-full bg-gray-300 h-2 rounded overflow-hidden">
              <div class="bg-blue-500 h-full" :style="{ width: progressToNextRank + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <ChatBox />
    </main>
  </div>
</template>

<script lang="ts">
import Cookies from 'js-cookie'
import ChatBox from '../components/ChatBox.vue'
import apiClient from '../services/api'
import { useRouter } from 'vue-router'
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Client } from '@stomp/stompjs'

interface RankThreshold {
  rank: string
  min: number
  max: number
}

const rankThresholds: RankThreshold[] = [
  { rank: 'UNRANKED', min: 0, max: 49 },
  { rank: 'BRONZE', min: 50, max: 999 },
  { rank: 'SILVER', min: 1000, max: 1399 },
  { rank: 'GOLD', min: 1400, max: 1799 },
  { rank: 'DIAMOND', min: 1800, max: 2199 },
  { rank: 'EMERALD', min: 2200, max: 2599 },
  { rank: 'PLATINUM', min: 2600, max: 2999 },
  { rank: 'PROMPTMASTER', min: 3000, max: 9999 },
]

interface User {
  id?: number
  email: string
  name?: string
  username: string
  picture: string
  country: string
  rank: string
  elo: number
  createdAt?: string
  updatedAt?: string
}

interface Player {
  userId: number
  username: string
  picture: string
  elo: number
}

interface GameNotification {
  type: string
  message: string
  data?: any
}

export default {
  name: 'GameLobby',
  components: { ChatBox },
  setup() {
    const router = useRouter()
    return { router }
  },
  data() {
    return {
      players: [] as Player[],
      user: null as User | null,
      loading: true,
      error: null as string | null,
      stompClient: null as Client | null,
      gameNotification: null as GameNotification | null,
      challengingSomeone: false,
      findingOpponent: false,
      showInviteModal: false,
      inviteLink: '',
      linkCopied: false,
      challengerId: null as number | null,
      defaultAvatar: 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y',
      preserveSocket: false,
      matchmakingTimer: null as number | null,
      transitioningToGame: false
    }
  },
  computed: {
    currentRank() {
      if (!this.user) return 'UNRANKED';
      const elo = this.user.elo;
      return rankThresholds.find(t => elo >= t.min && elo <= t.max)?.rank || 'UNRANKED';
    },
    progressToNextRank() {
      if (!this.user) return 0;
      const elo = this.user.elo;
      const currentThreshold = rankThresholds.find(t => elo >= t.min && elo <= t.max);
      if (!currentThreshold) return 0;

      const nextThreshold = rankThresholds[rankThresholds.indexOf(currentThreshold) + 1];
      if (!nextThreshold) return 100;

      const progress = ((elo - currentThreshold.min) / (nextThreshold.min - currentThreshold.min)) * 100;
      return Math.min(Math.max(progress, 0), 100);
    },
    availablePlayers() {
      if (!this.user?.id || !this.players) return [];
      return this.players.filter(player => Number(player.userId) !== this.user!.id);
    }
  },
  methods: {
    handleImageError(event: Event) {
      const target = event.target as HTMLImageElement;
      target.src = this.defaultAvatar;
    },
    async loadUserData() {
      try {
        console.log('Starting to fetch user data');

        const savedUser = Cookies.get('user');
        if (!savedUser) {
          console.log('No user cookie found');
          this.error = 'No user is logged in';
          this.loading = false;
          return;
        }

        try {
          const userData = JSON.parse(savedUser);
          console.log('Parsed user cookie data:', userData);

          if (!userData.email) {
            console.error('No email found in user cookie');
            this.error = 'User data is incomplete';
            this.loading = false;
            return;
          }

          const response = await apiClient.get('/users/email', {
            params: { email: userData.email },
            timeout: 10000
          });

          if (!response || response.status !== 200) {
            throw new Error(`Server responded with status ${response?.status || 'unknown'}`);
          }

          if (!response.data) {
            console.error('Response exists but no data received');
            throw new Error('No data received from server');
          }

          this.user = response.data;
          this.loading = false;

          this.initGameWebSocket();
        } catch (parseError) {
          console.error('Error parsing user cookie:', parseError);
          this.error = 'Invalid user data format';
          this.loading = false;
        }
      } catch (error: any) {
        console.error('Error fetching user data:', error);
        this.error = `Failed to load user data: ${error.message || 'Unknown error'}`;
        this.loading = false;
      }
    },
    initGameWebSocket() {
      if (!this.user || !this.user.id) {
        console.error('Cannot initialize game WebSocket - user ID is missing');
        return;
      }

      if (this.stompClient) {
        this.stompClient.deactivate();
      }

      const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
      const wsUrl = baseUrl.replace(/^http/, 'ws');

      this.stompClient = new Client({
        brokerURL: `${wsUrl}/game`,
        debug: function(str) {
          console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('Game WebSocket connection established');

          // Join the lobby
          this.stompClient?.publish({
            destination: '/app/game/join-lobby',
            body: JSON.stringify({
              userId: this.user?.id
            })
          });

          // Subscribe to lobby updates
          this.stompClient?.subscribe('/topic/lobby', (message) => {
            try {
              this.players = JSON.parse(message.body);
            } catch (e) {
              console.error('Error processing lobby update:', e);
            }
          });

          // Subscribe to personal game messages
          this.stompClient?.subscribe(`/user/${this.user?.id}/queue/game`, (message) => {
            try {
              const gameMessage = JSON.parse(message.body);
              this.handleGameMessage(gameMessage);
            } catch (e) {
              console.error('Error processing game message:', e);
            }
          });
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
        }
      });

      this.stompClient.activate();
    },
    findRandomOpponent() {
      if (!this.stompClient?.connected || !this.user?.id) return;

      this.findingOpponent = true;
      this.showNotification('info', 'Searching for an opponent...');

      const matchPreferences = {
        eloRange: 200,
        strictMatching: false
      };

      this.stompClient.publish({
        destination: '/app/game/find-opponent',
        body: JSON.stringify({
          userId: this.user.id,
          preferences: matchPreferences
        })
      });

      // Set a timeout to stop searching after 15 seconds
      if (this.matchmakingTimer) {
        clearTimeout(this.matchmakingTimer);
      }
      this.matchmakingTimer = setTimeout(() => {
        if (this.findingOpponent) {
          this.findingOpponent = false;
          this.showNotification('info', 'No opponent found. Please try again.');
          this.stompClient?.publish({
            destination: '/app/game/stop-searching',
            body: JSON.stringify({
              userId: this.user?.id
            })
          });
        }
      }, 15000);
    },
    challengePlayer(player: Player) {
      if (!this.stompClient?.connected || !this.user?.id) return;

      this.challengingSomeone = true;

      this.stompClient.publish({
        destination: '/app/game/challenge-player',
        body: JSON.stringify({
          userId: this.user.id,
          targetId: player.userId
        })
      });

      this.showNotification('info', `Challenge sent to ${player.username}. Waiting for response...`);
    },
    async acceptChallenge() {
      if (!this.stompClient?.connected || !this.user?.id) {
        this.error = 'WebSocket connection is not available';
        return;
      }

      console.log('Accepting challenge...');
      this.transitioningToGame = true;
      this.preserveSocket = true;

      try {
        this.stompClient.publish({
          destination: '/app/game/accept-challenge',
          body: JSON.stringify({
            userId: this.user.id
          })
        });
        console.log('Challenge acceptance sent');
      } catch (error) {
        console.error('Error accepting challenge:', error);
        this.showNotification('error', 'Failed to accept challenge. Please try again.');
        this.transitioningToGame = false;
        this.preserveSocket = false;
      }
    },
    rejectChallenge() {
      if (!this.stompClient?.connected || !this.user?.id) return;

      this.stompClient.publish({
        destination: '/app/game/reject-challenge',
        body: JSON.stringify({
          userId: this.user.id
        })
      });

      this.gameNotification = null;
      this.challengerId = null;
    },
    handleGameMessage(message: any) {
      switch (message.type) {
        case 'CHALLENGE_RECEIVED':
          this.handleChallengeReceived(message);
          break;
        case 'CHALLENGE_REJECTED':
          this.handleChallengeRejected(message);
          break;
        case 'NO_OPPONENT':
          this.handleNoOpponent(message);
          break;
        case 'GAME_STARTED':
          this.handleGameStarted(message);
          break;
        case 'ERROR':
          this.handleError(message);
          break;
      }
    },
    handleChallengeReceived(message: any) {
      this.challengerId = message.challengerId;

      const challengeMessage = `
        <div class="flex items-center">
          <img src="${message.challengerPicture || this.defaultAvatar}" class="w-8 h-8 rounded-full mr-2" alt="Challenger" />
          <div>
            <strong>${message.challengerName}</strong> (ELO: ${message.challengerElo})
            <p>has challenged you to a game!</p>
          </div>
        </div>
      `;

      this.showNotification('challenge', challengeMessage);
    },
    handleChallengeRejected(message: any) {
      this.challengingSomeone = false;
      this.showNotification('info', message.content);
    },
    handleNoOpponent(message: any) {
      this.findingOpponent = false;
      this.showNotification('info', message.content);
    },
    handleGameStarted(message: any) {
      console.log('Game started event received:', message);
      this.preserveSocket = true;

      if (!this.user?.id || !this.user?.username) {
        console.error('User data is missing');
        this.showNotification('error', 'Failed to start game: User data is missing');
        return;
      }

      try {
        this.router.push({
          name: 'game',
          params: { gameId: message.gameId },
          query: {
            playerId: this.user.id.toString(),
            username: this.user.username,
            picture: this.user.picture || '',
            opponentId: message.opponentId,
            opponentName: message.opponentName,
            opponentPicture: message.opponentPicture,
            opponentElo: message.opponentElo,
            rounds: message.rounds,
            currentRound: message.currentRound,
            currentPuzzleId: message.currentPuzzleId,
            timePerRound: message.timePerRound,
          }
        });
      } catch (error) {
        console.error('Error navigating to game:', error);
        this.showNotification('error', 'Failed to start game. Please try again.');
      }
    },
    handleError(message: any) {
      this.showNotification('error', message.content);
    },
    showNotification(type: string, message: string) {
      this.gameNotification = { type, message };

      if (type !== 'challenge') {
        setTimeout(() => {
          if (this.gameNotification && this.gameNotification.message === message) {
            this.gameNotification = null;
          }
        }, 5000);
      }
    },
    toggleInviteModal() {
      this.showInviteModal = !this.showInviteModal;
      this.linkCopied = false;

      if (this.showInviteModal) {
        const baseUrl = window.location.origin;
        this.inviteLink = `${baseUrl}/invite?user=${this.user?.username || 'friend'}`;
      }
    },
    copyInviteLink() {
      navigator.clipboard.writeText(this.inviteLink)
        .then(() => {
          this.linkCopied = true;
        })
        .catch(err => {
          console.error('Failed to copy link:', err);
        });
    }
  },
  created() {
    this.loadUserData();
    this.inviteLink = `${window.location.origin}/invite`;
  },
  beforeUnmount() {
    if (this.stompClient && !this.preserveSocket) {
      if (this.user?.id) {
        this.stompClient.publish({
          destination: '/app/game/leave-lobby',
          body: JSON.stringify({
            userId: this.user.id
          })
        });
      }
      this.stompClient.deactivate();
    }

    if (this.matchmakingTimer) {
      clearTimeout(this.matchmakingTimer);
    }
  },
}
</script>

<style>
body {
  font-family: Arial, sans-serif;
}
</style>
