import { defineStore } from 'pinia'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('threadmind.token') || '',
    nickname: localStorage.getItem('threadmind.nickname') || '演示用户'
  }),
  getters: {
    isSignedIn: (state) => Boolean(state.token)
  },
  actions: {
    login(username: string) {
      this.token = `demo.${Date.now()}`
      this.nickname = username || '演示用户'
      localStorage.setItem('threadmind.token', this.token)
      localStorage.setItem('threadmind.nickname', this.nickname)
    },
    logout() {
      this.token = ''
      localStorage.removeItem('threadmind.token')
      localStorage.removeItem('threadmind.nickname')
    }
  }
})

