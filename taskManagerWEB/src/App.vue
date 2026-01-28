<script>
//база
import SideBar from './components/Side-bar.vue'
import TaskManager from './components/Task-manager.vue'

//регистрация
import LoginModal from './components/auth/Login-modal.vue'
import RegistrationModal from './components/auth/Registration-modal.vue'

//логика
import PushPanel from './components/logic/Push-panel.vue'
import ContactList from './components/logic/Contact-list.vue'
import NotificationList from './components/logic/Notification-list.vue'
import SettingsModal from './components/logic/Settings-modal.vue'

//почта
import EmailVerification from './components/auth/Email-verification.vue'

export default {
  name: 'App',
  components: {
    TaskManager,
    SideBar,
    LoginModal,
    PushPanel,
    ContactList,
    RegistrationModal,
    NotificationList,
    EmailVerification,
    SettingsModal,
  },

  provide() {
    return {
      getCurrentUser: () => {
        console.log('Provide currentUser:', this.currentUser)
        return this.currentUser
      },
    }
  },

  data() {
    return {
      isAuthenticated: false,
      globalData: {
        userId: '',
        userName: '',
        firstName: '',
        lastName: '',
        roles: new Set(),
      },

      //пуш-панель:
      showPushPanel: false,
      pushPanelData: {
        message: '',
        buttonText: '',
        seconds: 0,
      },

      savedCredentials: null,

      showLoginModal: false,
      showRegistrationModal: false,
      showSideBar: false,
      showContactlist: false,
      showNotificationlist: false,
      webSocketConnected: false,

      showSettingList: false,
      showEmailVerification: false,
      verificationCode: '',
    }
  },

  computed: {
    currentUser() {
      return {
        id: this.globalData.userId,
        username: this.globalData.userName,
        firstName: this.globalData.firstName,
        lastName: this.globalData.lastName,
        roles: this.globalData.roles,
      }
    },

    requiresAuthBadge() {
      return !(this.isAuthenticated || this.showSideBar)
    },

    mainContentClass() {
      return {
        blurred:
          this.showSideBar ||
          (!this.isAuthenticated && !this.showLoginModal) ||
          this.showEmailVerification,
      }
    },

    showMainContent() {
      return this.isAuthenticated && !this.showEmailVerification
    },
  },

  methods: {
    async logIn(credentials = null) {
      try {
        const creds = credentials || this.savedCredentials
        if (!creds) {
          throw new Error('Не удалось залогиниться: отсутствуют учетные данные')
        }

        let authToken
        const preLoginResponse = await this.fetchWithTimeout('/auth/pre-login', {
          method: 'GET',
          credentials: 'include',
        })

        if (preLoginResponse.ok) {
          authToken = await preLoginResponse.text()
        } else {
          const loginResponse = await this.fetchWithTimeout('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(creds),
            credentials: 'include',
          })

          if (!loginResponse.ok) {
            throw new Error(`Ошибка авторизации: ${loginResponse.status}`)
          }

          authToken = await loginResponse.text()
        }

        if (authToken) {
          localStorage.setItem('authToken', authToken)
        }

        const sessionResponse = await this.fetchWithTimeout('/api/create-session', {
          method: 'POST',
          headers: {
            'X-Auth-Token': authToken,
          },
          credentials: 'include',
        })

        console.log('Session response status:', sessionResponse.status)

        if (!sessionResponse.ok) {
          throw new Error(`Ошибка создания сессии: ${sessionResponse.status}`)
        }

        await this.loadUserData()

        if (credentials) {
          this.saveCredentials(creds)
        }

        this.isAuthenticated = true
        this.showLoginModal = false
        this.showRegistrationModal = false

        console.log(`Выполнен вход: ${this.globalData.userName}`)
        this.showAuthWelcome()
      } catch (error) {
        console.error('Ошибка авторизации:', error)
        this.handleAuthError(error)
      }
    },

    async register(userData) {
      try {
        console.log('Регистрация пользователя:', userData)

        const newUser = {
          firstName: userData.firstName,
          lastName: userData.lastName,
          username: userData.login,
          password: userData.password,
          email: userData.email,
        }

        const registrationResponse = await this.fetchWithTimeout('/auth/signup', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(newUser),
          credentials: 'include',
        })

        if (!registrationResponse.ok) {
          const errorText = await registrationResponse.text()
          throw new Error(`Ошибка регистрации: ${registrationResponse.status} - ${errorText}`)
        }

        const authToken = await registrationResponse.text()
        console.log('Получен authToken:', authToken)

        if (authToken) {
          localStorage.setItem('authToken', authToken)
        }

        this.showPushMessage('Не забудьте подтвердить свою почту', 'ок', null)
        this.showRegistrationModal = false
        this.showLoginModal = true

      } catch (error) {
        console.error('Ошибка регистрации:', error)
        this.showPushMessage('Ошибка регистрации: ' + error.message, 'OK', 3000)
      }
    },

    async loadUserData() {
      try {
        const userResponse = await this.fetchWithTimeout('/api/users/current', {
          method: 'GET',
          credentials: 'include',
        })

        if (userResponse.ok) {
          const userData = await userResponse.json()
          this.updateUserData(userData)
        }
      } catch (error) {
        console.warn('Не удалось загрузить данные пользователя:', error)
      }
    },

    updateUserData(userData) {
      this.globalData.userId = userData.id
      this.globalData.userName = userData.username
      this.globalData.firstName = userData.firstName
      this.globalData.lastName = userData.lastName
      this.globalData.roles = userData.roles
    },

    async logOut() {
      try {
        if (this.globalData.userId) {
          await this.fetchWithTimeout('/api/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain' },
            body: this.globalData.userId,
            credentials: 'include',
          })
        }
        console.log('Выход выполнен успешно')
      } catch (error) {
        console.warn('Не удалось отправить log-out запрос:', error)
      } finally {
        this.resetAuthState()
        console.log('Вы успешно разлогинились!')
      }
    },

    resetAuthState() {
      this.globalData = {
        userId: '',
        userName: '',
        firstName: '',
        lastName: '',
      }
      this.isAuthenticated = false
      this.savedCredentials = null
      localStorage.removeItem('savedCredentials')
      localStorage.removeItem('authToken')
    },

    async checkExistingSession() {
      try {
        const testResponse = await this.fetchWithTimeout('/api/users/current', {
          method: 'GET',
          credentials: 'include',
        })

        if (testResponse.ok) {
          await this.loadUserData()
          this.isAuthenticated = true
          return true
        }
        return false
      } catch (error) {
        console.error('Ошибка проверки сессии:', error)
        return false
      }
    },

    checkUrlForVerification() {
      const urlParams = new URLSearchParams(window.location.search);
      const code = urlParams.get('code');

      if (code) {
        this.showEmailVerification = true;
        this.verificationCode = code;
        console.log('Обнаружен code подтверждения:', code);

        const newUrl = window.location.pathname;
        window.history.replaceState({}, '', newUrl);
      }
    },

    finishVerification(authToken = null) {
      this.showEmailVerification = false
      this.verificationCode = ''

      if (authToken) {
        localStorage.setItem('authToken', authToken)
      }

      this.showPushMessage('Email успешно подтвержден!', 'OK', 5000)

      this.attemptAutoLoginAfterVerification()
    },

    async attemptAutoLoginAfterVerification() {
      try {
        const hasActiveSession = await this.checkExistingSession()
        if (hasActiveSession) {
          this.isAuthenticated = true
          await this.loadUserData()
        } else if (this.loadSavedCredentials()) {
          await this.logIn()
        } else {
          this.showLoginModal = true
        }
      } catch (error) {
        console.error('Автоматический вход после верификации не удался:', error)
        this.showLoginModal = true
      }
    },

    toggleSideBar() {
      this.showSideBar = !this.showSideBar
    },

    closeContactlist() {
      this.showContactlist = false
    },

    switchToRegistration() {
      this.showLoginModal = false
      this.showRegistrationModal = true
    },

    switchToLogin() {
      this.showRegistrationModal = false
      this.showLoginModal = true
    },

    showAuthWelcome() {
      this.showPushMessage(`Добро пожаловать, ${this.globalData.userName}!`, 'Вперёд!', 5000)
    },

    showAuthError() {
      this.showPushMessage('Не удалось авторизоваться', 'OK', 3000)
    },

    showSettings() {
      this.showSettingList = true
    },

    closeSettings() {
      this.showSettingList = false
    },

    handleAuthError(error) {
      this.isAuthenticated = false
      this.showAuthError()

      console.log(error)
      if (this.savedCredentials?.login) {
        this.showLoginModal = true
      }
    },

    showPushMessage(message, buttonText, seconds) {
      this.hidePushPanel()
      this.pushPanelData = { message, buttonText, seconds }
      this.showPushPanel = true
    },

    hidePushPanel() {
      this.showPushPanel = false
    },

    openLoginModal() {
      this.showLoginModal = true
      this.showRegistrationModal = false
    },

    closeLoginModal() {
      this.showLoginModal = false
    },

    closeRegistrationModal() {
      this.showRegistrationModal = false
    },

    showFullContactlist() {
      this.showContactlist = true
    },

    showNotifications() {
      this.showNotificationlist = true
    },

    closeNotifications() {
      this.showNotificationlist = false
    },

    loadSavedCredentials() {
      try {
        const saved = localStorage.getItem('savedCredentials')
        if (saved) {
          this.savedCredentials = JSON.parse(saved)
          return true
        }
      } catch (error) {
        console.error('Не удалось загрузить credentials:', error)
      }
      return false
    },

    saveCredentials(credentials) {
      this.savedCredentials = credentials
      localStorage.setItem('savedCredentials', JSON.stringify(credentials))
    },

    async fetchWithTimeout(url, options, timeout = 10000) {
      const controller = new AbortController()
      const id = setTimeout(() => controller.abort(), timeout)

      try {
        const response = await fetch(url, {
          ...options,
          signal: controller.signal,
        })
        clearTimeout(id)
        return response
      } catch (error) {
        clearTimeout(id)
        throw error
      }
    },
  },

  async mounted() {
    console.log("Добро пожаловать в TM");

    this.checkUrlForVerification();

    if (!this.showEmailVerification) {
      const hasActiveSession = await this.checkExistingSession();
      if (!hasActiveSession) {
        const hasCredentials = this.loadSavedCredentials();
        if (hasCredentials) {
          try {
            await this.logIn();
          } catch (error) {
            console.error('Автоматический вход не удался:', error);
            this.showLoginModal = true;
          }
        } else {
          this.showLoginModal = true;
        }
      }
    }
  },
}
</script>

<template>
  <div class="app">
    <div class="content-wrapper">
      <button
        class="toggle-button"
        :class="{ 'button-pushed': showSideBar }"
        @click="toggleSideBar"
      >
        {{ showSideBar ? 'X' : '☰' }}
        <span class="auth-required-badge" v-if="requiresAuthBadge">!</span>
      </button>
      <div v-if="!showEmailVerification">
        <div
          v-if="!isAuthenticated && !showLoginModal && !showRegistrationModal"
          class="auth-overlay"
        >
          <div class="auth-lock-icon">🔒</div>
          <h2>Авторизуйтесь для доступа</h2>
          <p>Войдите в систему, чтобы использовать все возможности приложения</p>
          <button class="auth-prompt-button" @click="openLoginModal">Войти</button>
        </div>
        <main class="main-content" :class="mainContentClass" id="main-content">
          <TaskManager
            v-if="showMainContent"
            :globalData="globalData"
            :isAuthenticated="isAuthenticated"
            :currentUser="currentUser"
          />
        </main>
      </div>

      <!-- всякие всплывающие объекты -->
      <SideBar
        :globalData="globalData"
        :isVisible="showSideBar"
        :isAuthenticated="isAuthenticated"
        @logIn="openLoginModal"
        @logOut="logOut"
        @showFullContactlist="showFullContactlist"
        @showNotifications="showNotifications"
        @showSettings="showSettings"
      />

      <LoginModal
        v-if="showLoginModal"
        @login="logIn"
        @close="closeLoginModal"
        @switch-to-register="switchToRegistration"
      />

      <RegistrationModal
        v-if="showRegistrationModal"
        @register="register"
        @close="closeRegistrationModal"
        @switch-to-login="switchToLogin"
      />

      <EmailVerification
        v-if="showEmailVerification"
        :code="verificationCode"
        @verification-complete="finishVerification"
        @verification-failed="showEmailVerification = false"
      />

      <ContactList
        v-if="showContactlist"
        :globalData="globalData"
        @close="closeContactlist"
        @showPushMessage="showPushMessage"
      />
      <NotificationList
        v-if="showNotificationlist"
        :globalData="globalData"
        @close="closeNotifications"
      />
      <SettingsModal
        v-if="showSettingList"
        :globalData="globalData"
        @showPushMessage="showPushMessage"
        @close="closeSettings"
      />
    </div>
  </div>
  <PushPanel
    v-if="showPushPanel"
    :message="pushPanelData.message"
    :button-text="pushPanelData.buttonText"
    :button-action="hidePushPanel"
    :auto-hide="pushPanelData.seconds"
    :show="showPushPanel"
    @hidden="hidePushPanel"
  />
</template>

<style scoped>
.app {
  position: relative;
  min-height: 100vh;
  font-family: Arial, sans-serif;
}

.content-wrapper {
  position: relative;
  min-height: 100vh;
}

.toggle-button {
  position: fixed;
  top: 20px;
  left: 30px;
  padding: 10px 15px;
  background-color: #42b883;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  z-index: 1000;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 8px;
}

.button-pushed {
  left: 360px;
  background-color: #cd2424;
}

.toggle-button:hover {
  background-color: #369b70;
  transform: translateY(-1px);
}

.toggle-button:active {
  transform: translateY(0);
}

.auth-required-badge {
  background: #ff6b6b;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.main-content {
  padding: 20px 20px 20px;
  max-width: 90vw;
  margin: 0 auto;
  position: relative;
  transition: filter 0.3s ease;
}

.main-content.blurred {
  filter: blur(5px);
  pointer-events: none;
  user-select: none;
}

.auth-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 998;
  text-align: center;
  padding: 20px;
}

.auth-lock-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.1);
  }

  100% {
    transform: scale(1);
  }
}

.auth-overlay h2 {
  color: #2c3e50;
  margin-bottom: 10px;
  font-size: 24px;
}

.auth-overlay p {
  color: #7f8c8d;
  margin-bottom: 30px;
  max-width: 400px;
  line-height: 1.5;
}

.auth-prompt-button {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.auth-prompt-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

@media (max-width: 768px) {
  .main-content {
    padding: 70px 15px 15px;
  }

  .auth-overlay h2 {
    font-size: 20px;
  }

  .auth-lock-icon {
    font-size: 48px;
  }
}
</style>
