<script>
export default {
  name: 'Email-verification',
  props: {
    code: {
      type: String,
      required: true
    }
  },
  emits: ['verification-complete', 'verification-failed'],
  data() {
    return {
      loading: true,
      success: false,
      errorMessage: ''
    }
  },
  async mounted() {
    await this.verifyEmail()
  },
  methods: {
    async verifyEmail() {
      try {
        console.log('Отправка запроса подтверждения с кодом:', this.code)

        const response = await this.fetchWithTimeout(`/auth/verify?code=${encodeURIComponent(this.code)}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include'
        })

        if (response.ok) {
          const authToken = await response.text()
          this.success = true
          console.log('Email успешно подтвержден, токен:', authToken)

          if (authToken) {
            localStorage.setItem('authToken', authToken)
          }

          this.$emit('verification-complete', authToken)
        } else {
          const errorText = await response.text()
          let errorMessage = 'Неизвестная ошибка'

          try {
            const errorData = JSON.parse(errorText)
            errorMessage = errorData.message || errorData
          } catch {
            errorMessage = errorText || `Ошибка сервера: ${response.status}`
          }

          throw new Error(errorMessage)
        }
      } catch (error) {
        console.error('Ошибка подтверждения email:', error)
        this.errorMessage = error.message || 'Произошла ошибка при подтверждении email'
        this.success = false
        this.$emit('verification-failed', this.errorMessage)
      } finally {
        this.loading = false
      }
    },

    handleComplete() {
    },

    handleFailed() {
      this.$emit('verification-failed', this.errorMessage)
    },

    async fetchWithTimeout(url, options, timeout = 10000) {
      const controller = new AbortController()
      const id = setTimeout(() => controller.abort(), timeout)

      try {
        const response = await fetch(url, {
          ...options,
          signal: controller.signal
        })
        clearTimeout(id)
        return response
      } catch (error) {
        clearTimeout(id)
        if (error.name === 'AbortError') {
          throw new Error('Таймаут запроса: сервер не отвечает')
        }
        throw error
      }
    }
  }
}
</script>

<template>
  <div class="email-verification-overlay">
    <div class="verification-container">
      <div class="verification-card">
        <div v-if="loading" class="verification-status loading">
          <div class="spinner"></div>
          <h2>Подтверждение email</h2>
          <p>Пожалуйста, подождите...</p>
        </div>

        <div v-else-if="success" class="verification-status success">
          <div class="status-icon">✅</div>
          <h2>Email успешно подтвержден!</h2>
          <p>Теперь вы можете пользоваться всеми функциями приложения.</p>
          <button @click="handleComplete" class="btn-primary">
            Продолжить
          </button>
        </div>

        <div v-else class="verification-status error">
          <div class="status-icon">❌</div>
          <h2>Ошибка подтверждения</h2>
          <p>{{ errorMessage }}</p>
          <button @click="handleFailed" class="btn-secondary">
            Вернуться
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.email-verification-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 20px;
}

.verification-container {
  max-width: 500px;
  width: 100%;
}

.verification-card {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  text-align: center;
  border: 1px solid #e1e5e9;
}

.verification-status {
  padding: 1rem;
}

.status-icon {
  font-size: 64px;
  margin-bottom: 1rem;
}

.verification-status h2 {
  color: #2c3e50;
  margin-bottom: 1rem;
  font-size: 1.5rem;
}

.verification-status p {
  color: #7f8c8d;
  margin-bottom: 2rem;
  line-height: 1.5;
}

.btn-primary {
  background: linear-gradient(135deg, #42b883, #369b70);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(66, 184, 131, 0.3);
}

.btn-secondary {
  background: #6c757d;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: #5a6268;
  transform: translateY(-2px);
}

.loading .spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #42b883;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.success .status-icon {
  color: #28a745;
}

.error .status-icon {
  color: #dc3545;
}

@media (max-width: 768px) {
  .verification-card {
    padding: 1.5rem;
  }

  .status-icon {
    font-size: 48px;
  }
}
</style>
