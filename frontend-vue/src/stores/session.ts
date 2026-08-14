import { defineStore } from 'pinia'

import { fetchCurrentUser, fetchNotificationUnreadCount, loginUser, registerUser } from '../api/forum'
import type { AuthResponse, AuthUser } from '../api/types'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('threadmind.token') || '',
    username: localStorage.getItem('threadmind.username') || '',
    nickname: localStorage.getItem('threadmind.nickname') || '演示用户',
    roles: readRoles(),
    unreadCount: 0
  }),
  getters: {
    isSignedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.roles.includes('ADMIN') || state.roles.includes('MODERATOR')
  },
  actions: {
    async login(username: string, password: string) {
      this.applyAuth(await loginUser({ username, password }))
    },
    async register(payload: { username: string; email: string; password: string; nickname: string }) {
      this.applyAuth(await registerUser(payload))
    },
    async refreshCurrentUser() {
      if (!this.token) {
        return
      }
      const user = await fetchCurrentUser()
      this.applyUser(user)
    },
    async refreshUnreadCount() {
      if (!this.token) {
        this.unreadCount = 0
        return
      }
      try {
        this.unreadCount = (await fetchNotificationUnreadCount()).unreadCount
      } catch {
        this.unreadCount = 0
      }
    },
    applyAuth(response: AuthResponse) {
      this.token = response.token
      localStorage.setItem('threadmind.token', response.token)
      this.applyUser(response.user)
      void this.refreshUnreadCount()
    },
    applyUser(user: AuthUser) {
      this.username = user.username
      this.nickname = user.nickname || user.username || '演示用户'
      this.roles = user.roles || []
      localStorage.setItem('threadmind.username', this.username)
      localStorage.setItem('threadmind.nickname', this.nickname)
      localStorage.setItem('threadmind.roles', JSON.stringify(this.roles))
    },
    logout() {
      this.token = ''
      this.username = ''
      this.unreadCount = 0
      localStorage.removeItem('threadmind.token')
      localStorage.removeItem('threadmind.username')
      localStorage.removeItem('threadmind.nickname')
      localStorage.removeItem('threadmind.roles')
    }
  }
})

function readRoles(): string[] {
  const raw = localStorage.getItem('threadmind.roles')
  if (!raw) {
    return []
  }
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return []
  }
}
