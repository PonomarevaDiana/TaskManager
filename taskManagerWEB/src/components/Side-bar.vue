<script>
import AccountCard from './Account-card.vue'

export default {
  components: { AccountCard },
  data() {
    return {
      showLanguageMenu: false,
      languageMenuX: 0,
      languageMenuY: 0,
      phrases: {},
      showOptions: false,
      showAuthModal: false,
    }
  },
  props: {
    globalData: Object,
    isVisible: Boolean,
    isAuthenticated: Boolean,
  },
  emits: ['logIn', 'logOut', 'showFullContactlist', 'showNotifications', 'showSettings'],
  computed: {
    isAdmin() {
      return this.globalData.roles.includes('ADMIN') || false
    },
  },
  watch: {
    isVisible(newVal) {
      if (newVal) {
        setTimeout(() => {
          this.showOptions = true
        }, 50)
      } else {
        this.showOptions = false
      }
    },
  },
  methods: {
    handleShowAuthModal() {
      this.showAuthModal = true
    },
    handleLogin(loginData) {
      console.log('Login attempt:', loginData)
      this.showAuthModal = false
    },
    handleRegister(registerData) {
      console.log('Register attempt:', registerData)
      this.showAuthModal = false
    },
    switchAuth() {
      if (this.isAuthenticated) {
        this.$emit('logOut')
      } else {
        this.$emit('logIn')
      }
    },
    onShowFullContactlist() {
      this.$emit('showFullContactlist')
    },
    showNotification() {
      this.$emit('showNotifications')
    },
    showSettings() {
      this.$emit('showSettings')
    },
    showMetrics() {
      window.open('http://localhost:9090', '_blank')
    },
  },
}
</script>

<template>
  <div class="sidebar-container" :class="{ 'sidebar-visible': isVisible }">
    <div class="container" :class="{ 'options-visible': showOptions }">
      <AccountCard
        v-if="showOptions"
        :isAuthenticated="isAuthenticated"
        :globalData="globalData"
        @switchAuth="switchAuth"
      />

      <button
        v-if="isAuthenticated"
        type="submit"
        class="sideBar-btn"
        @click="onShowFullContactlist"
      >
        <span class="btn-icon">👥</span>
        Контакты
        <span class="btn-arrow">→</span>
      </button>
      <button v-if="isAuthenticated" type="submit" class="sideBar-btn" @click="showNotification">
        <span class="btn-icon">📢</span>
        Уведомления
        <span class="btn-arrow">→</span>
      </button>
      <button v-if="isAuthenticated" type="submit" class="sideBar-btn" @click="showSettings">
        <span class="btn-icon">⚙️</span>
        Настройки
        <span class="btn-arrow">→</span>
      </button>
      <button
        v-if="isAuthenticated && isAdmin"
        type="submit"
        class="sideBar-btn"
        @click="showMetrics"
      >
        <span class="btn-icon">📶</span>
        Метрики
        <span class="btn-arrow">→</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.sidebar-container {
  position: fixed;
  top: 0;
  left: -340px;
  width: 350px;
  height: 100%;
  background-color: #369b70;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  z-index: 999;
  transition: left 0.3s ease-in-out;
  overflow-y: hidden;
}

.sidebar-visible {
  left: 0;
}

.container {
  opacity: 0;
  transition: opacity 0.2s ease-in-out;
  padding: 20px;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.options-visible {
  opacity: 1;
}

.sideBar-btn {
  width: 100%;
  padding: 16px 20px;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.sideBar-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.sideBar-btn:hover::before {
  left: 100%;
}

.sideBar-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.15);
}

.sideBar-btn:active {
  transform: translateY(0);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.btn-icon {
  font-size: 20px;
  transition: transform 0.3s ease;
  filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.2));
}

.btn-arrow {
  font-size: 18px;
  transition: transform 0.3s ease;
  opacity: 0.8;
}

.sideBar-btn:hover .btn-icon {
  transform: scale(1.1);
}

.sideBar-btn:hover .btn-arrow {
  transform: translateX(3px);
  opacity: 1;
}

@media (max-width: 480px) {
  .sideBar-btn {
    padding: 14px 18px;
    font-size: 15px;
    border-radius: 14px;
  }

  .btn-icon {
    font-size: 18px;
  }

  .btn-arrow {
    font-size: 16px;
  }
}
</style>
