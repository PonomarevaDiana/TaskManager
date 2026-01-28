<script>
export default {
  name: 'Register-modal',
  data() {
    return {
      firstName: '',
      lastName: '',
      login: '',
      password: '',
      confirmPassword: '',
      email: '',

      loading: false,
      error: ''
    }
  },
  methods: {
    signup() {
      console.log("регистрация типа")
    },

    async handleSubmit() {
      if (!this.login.trim() || !this.password.trim() || !this.confirmPassword.trim() || !this.email.trim()) {
        this.error = 'Пожалуйста, заполните все поля'
        return
      }

      if (this.password !== this.confirmPassword) {
        this.error = 'Пароли не совпадают'
        return
      }

      this.loading = true
      this.error = ''

      try {
        this.$emit('register', {
          firstName: this.firstName.trim(),
          lastName: this.lastName.trim(),
          login: this.login.trim(),
          password: this.password.trim(),
          email: this.email.trim()
        })

        this.resetForm()

      } catch (error) {
        this.error = 'Ошибка регистрации. Попробуйте еще раз.'
        console.error('Ошибка в компоненте RegisterModal:', error)
      } finally {
        this.loading = false
      }
    },

    resetForm() {
      this.firstName = ''
      this.lastName = ''
      this.login = ''
      this.password = ''
      this.confirmPassword = ''
      this.email = ''
      this.error = ''
    },

    close() {
      this.$emit('close')
      this.resetForm()
      this.error = ''
      this.loading = false
    },

    switchToLogin() {
      this.$emit('switch-to-login')
    },

    handleKeydown(event) {
      if (event.key === 'Escape') {
        this.close()
      }
    }
  },

  mounted() {
    document.addEventListener('keydown', this.handleKeydown)

    this.$nextTick(() => {
      const firstNameInput = this.$el.querySelector('#firstName')
      if (firstNameInput) firstNameInput.focus()
    })
  },

  beforeUnmount() {
    document.removeEventListener('keydown', this.handleKeydown)
  }
}
</script>

<template>
  <div class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <button class="close-button" @click="close" aria-label="Закрыть окно">×</button>

      <div class="modal-header">
        <h2>Регистрация</h2>
        <p>Создайте новую учетную запись</p>
      </div>

      <form @submit.prevent="handleSubmit" class="register-form">
        <div class="form-group">
          <label for="firstName">Имя:</label>
          <input
            id="firstName"
            v-model="firstName"
            type="text"
            required
            placeholder="Введите Ваше имя"
            :disabled="loading"
            autocomplete="given-name"
          >
        </div>

        <div class="form-group">
          <label for="lastName">Фамилия:</label>
          <input
            id="lastName"
            v-model="lastName"
            type="text"
            required
            placeholder="Введите Вашу фамилию"
            :disabled="loading"
            autocomplete="family-name"
          >
        </div>

        <div class="form-group">
          <label for="login">Логин:</label>
          <input
            id="login"
            v-model="login"
            type="text"
            required
            placeholder="Придумайте логин"
            :disabled="loading"
            autocomplete="username"
          >
        </div>

        <div class="form-group">
          <label for="email">Email:</label>
          <input
            id="email"
            v-model="email"
            type="email"
            required
            placeholder="Введите ваш email"
            :disabled="loading"
            autocomplete="email"
          >
        </div>

        <div class="form-group">
          <label for="password">Пароль:</label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            placeholder="Придумайте пароль"
            :disabled="loading"
            autocomplete="new-password"
          >
        </div>

        <div class="form-group">
          <label for="confirmPassword">Подтвердите пароль:</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            required
            placeholder="Повторите пароль"
            :disabled="loading"
            autocomplete="new-password"
          >
        </div>

        <button
          type="submit"
          class="submit-button"
          :disabled="loading || !firstName.trim() || !lastName.trim() || !login.trim() || !password.trim() || !confirmPassword.trim() || !email.trim()"
        >
          <span v-if="loading">Регистрация...</span>
          <span v-else>Зарегистрироваться</span>
        </button>
      </form>

      <div v-if="error" class="error-message">
        <span class="error-icon">⚠</span>
        {{ error }}
      </div>

      <div class="modal-footer">
        <p class="help-text">
          Уже есть аккаунт?
          <button type="button" class="switch-button" @click="switchToLogin">
            Войти
          </button>
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 30px;
  max-width: 400px;
  width: 100%;
  position: relative;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  animation: slideIn 0.3s ease;
  max-height: 90vh;
  overflow-y: auto;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-50px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.close-button {
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #7f8c8d;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.close-button:hover {
  background: #f5f5f5;
  color: #2c3e50;
  transform: rotate(90deg);
}

.modal-header {
  text-align: center;
  margin-bottom: 30px;
}

.modal-header h2 {
  color: #2c3e50;
  margin-bottom: 8px;
  font-size: 24px;
  font-weight: 600;
}

.modal-header p {
  color: #7f8c8d;
  font-size: 14px;
  margin: 0;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 8px;
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
}

.form-group input {
  padding: 12px;
  border: 2px solid #e9ecef;
  border-radius: 6px;
  font-size: 16px;
  transition: all 0.3s ease;
  font-family: inherit;
}

.form-group input:focus {
  outline: none;
  border-color: #42b883;
  box-shadow: 0 0 0 3px rgba(66, 184, 131, 0.1);
}

.form-group input:disabled {
  background-color: #f8f9fa;
  cursor: not-allowed;
  opacity: 0.7;
}

.submit-button {
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 10px;
  font-family: inherit;
  position: relative;
  min-height: 48px;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  background: #ccc;
}

.switch-button {
  background: none;
  border: none;
  color: #369b70;
  cursor: pointer;
  text-decoration: underline;
  font-size: inherit;
  padding: 0;
  margin-left: 4px;
  font-family: inherit;
}

.switch-button:hover {
  color: #267355;
}

.error-message {
  background: #ff6b6b;
  color: white;
  padding: 12px;
  border-radius: 6px;
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  animation: shake 0.5s ease;
}

.error-icon {
  font-size: 16px;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.modal-footer {
  margin-top: 20px;
  text-align: center;
}

.help-text {
  color: #7f8c8d;
  font-size: 12px;
  margin: 0;
  line-height: 1.4;
}

@media (max-width: 480px) {
  .modal-content {
    padding: 20px;
    margin: 10px;
  }

  .modal-header h2 {
    font-size: 20px;
  }

  .modal-overlay {
    padding: 10px;
  }
}

@media (max-height: 600px) {
  .modal-content {
    max-height: 95vh;
    overflow-y: auto;
  }
}
</style>
