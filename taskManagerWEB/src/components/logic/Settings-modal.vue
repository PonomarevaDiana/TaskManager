<script>
import SettingsService from '@/services/Settings-service.js'

export default {
  emits: ['close', 'showPushMessage'],
  name: 'Settings-modal',
  props: {
    globalData: {
      type: Object
    }
  },
  data() {
    return {
      isLoading: false,
      settings: {
        profile: {
          firstName: this.globalData.firstName,
          lastName: this.globalData.lastName,
          username: ''
        },
        security: {
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        }
      },
      activeSection: 'profile'
    }
  },
  computed: {
    userInitials() {
      const { firstName, lastName } = this.settings.profile;
      if (firstName && lastName) {
        return (firstName[0] + lastName[0]).toUpperCase();
      }
      return this.globalData.userName ? this.globalData.userName[0].toUpperCase() : 'U';
    }
  },
  methods: {
    closeSettings() {
      this.$emit('close');
    },

    setActiveSection(section) {
      this.activeSection = section;
    },

    async saveProfile() {
      this.isLoading = true;

      try {
        await SettingsService.updateProfile({
          userId: this.globalData.userId,
          firstName: this.settings.profile.firstName,
          lastName: this.settings.profile.lastName,
          username: this.settings.profile.username
        });
        this.$emit('showPushMessage', 'Профиль успешно обновлен', 'OK', 3000);
      } catch (error) {
        console.log('error: ' + error)
        this.$emit('showPushMessage', error.message || 'Ошибка при сохранении профиля', 'OK', 3000);
      } finally {
        this.isLoading = false;
      }
    },

    async changePassword() {
      if (this.settings.security.newPassword !== this.settings.security.confirmPassword) {
        this.$emit('showPushMessage', 'Пароли не совпадают', 'OK', 3000);
        return;
      }

      if (this.settings.security.newPassword.length < 1) {
        this.$emit('showPushMessage', 'Пароль должен содержать минимум один символ', 'OK', 3000);
        return;
      }

      this.isLoading = true;

      try {
        await SettingsService.changePassword({
          userId: this.globalData.userId,
          oldPassword: this.settings.security.currentPassword,
          newPassword: this.settings.security.newPassword
        });

        this.settings.security.currentPassword = '';
        this.settings.security.newPassword = '';
        this.settings.security.confirmPassword = '';

        this.$emit('showPushMessage', 'Пароль успешно изменен', 'OK', 3000);
      } catch (error) {
        console.log('error: ' + error)
        this.$emit('showPushMessage', error.message || 'Ошибка при изменении пароля', 'OK', 3000);
      } finally {
        this.isLoading = false;
      }
    },

    async savePreferences() {
      this.isLoading = true;

      try {
        await SettingsService.updatePreferences(this.globalData.userId, this.settings.preferences);
        this.$emit('showPushMessage', 'Настройки сохранены', 'OK', 3000);
      } catch (error) {
        console.log('error: ' + error)
        this.$emit('showPushMessage', error.message || 'Ошибка при сохранении настроек', 'OK', 3000);
      } finally {
        this.isLoading = false;
      }
    },

    loadUserData() {
      this.settings.profile = {
        firstName: this.globalData.firstName || '',
        lastName: this.globalData.lastName || '',
        username: this.globalData.userName || ''
      };
    }
  },

  mounted() {
    document.body.style.overflow = 'hidden';
    this.loadUserData();
  },

  beforeUnmount() {
    document.body.style.overflow = '';
  }
}
</script>

<template>
  <div class="settings-overlay" @click.self="closeSettings">
    <div class="settings-modal">
      <div class="settings-header">
        <h2>Настройки</h2>
        <button class="close-btn" @click="closeSettings" :disabled="isLoading">×</button>
      </div>

      <div class="settings-content">
        <div class="settings-layout">
          <div class="settings-sidebar">
            <button
              class="sidebar-item"
              :class="{ active: activeSection === 'profile' }"
              @click="setActiveSection('profile')"
            >
              <span class="sidebar-icon">👤</span>
              <span class="sidebar-text">Профиль</span>
            </button>

            <button
              class="sidebar-item"
              :class="{ active: activeSection === 'security' }"
              @click="setActiveSection('security')"
            >
              <span class="sidebar-icon">🔒</span>
              <span class="sidebar-text">Смена пароля</span>
            </button>
            <div class="sidebar-divider"></div>
          </div>

          <div class="settings-main">
            <div class="settings-main-content">
              <div v-if="activeSection === 'profile'" class="settings-section">
                <h3 class="section-title">Профиль</h3>

                <div class="avatar-section">
                  <div class="avatar-container">
                    <div class="avatar-preview">
                      <img v-if="avatarPreview" :src="avatarPreview" alt="Avatar" class="avatar-image" />
                      <div v-else class="avatar-placeholder">
                        {{ userInitials }}
                      </div>
                    </div>
                  </div>
                </div>

                <div class="form-grid">
                  <div class="form-group">
                    <label class="form-label">Имя</label>
                    <input
                      v-model="settings.profile.firstName"
                      type="text"
                      class="form-input"
                      placeholder="Введите ваше имя"
                    />
                  </div>

                  <div class="form-group">
                    <label class="form-label">Фамилия</label>
                    <input
                      v-model="settings.profile.lastName"
                      type="text"
                      class="form-input"
                      placeholder="Введите вашу фамилию"
                    />
                  </div>

                  <div class="form-group">
                    <label class="form-label">Логин</label>
                    <input
                      v-model="settings.profile.username"
                      type="text"
                      class="form-input"
                      placeholder="Введите логин"
                    />
                  </div>
                </div>

                <div class="form-actions">
                  <button
                    class="save-btn"
                    @click="saveProfile"
                    :disabled="isLoading"
                  >
                    {{ isLoading ? 'Сохранение...' : 'Сохранить изменения' }}
                  </button>
                </div>
              </div>

              <!-- Секция смены пароля -->
              <div v-if="activeSection === 'security'" class="settings-section">
                <h3 class="section-title">Смена пароля</h3>

                <div class="form-grid">
                  <div class="form-group">
                    <label class="form-label">Текущий пароль</label>
                    <input
                      v-model="settings.security.currentPassword"
                      type="password"
                      class="form-input"
                      placeholder="Введите текущий пароль"
                    />
                  </div>

                  <div class="form-group">
                    <label class="form-label">Новый пароль</label>
                    <input
                      v-model="settings.security.newPassword"
                      type="password"
                      class="form-input"
                      placeholder="Введите новый пароль"
                    />
                  </div>

                  <div class="form-group">
                    <label class="form-label">Подтвердите пароль</label>
                    <input
                      v-model="settings.security.confirmPassword"
                      type="password"
                      class="form-input"
                      placeholder="Повторите новый пароль"
                    />
                  </div>
                </div>

                <div class="form-actions">
                  <button
                    class="save-btn"
                    @click="changePassword"
                    :disabled="isLoading"
                  >
                    {{ isLoading ? 'Смена пароля...' : 'Сменить пароль' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  animation: fadeIn 0.3s ease;
}

.settings-modal {
  background: white;
  border-radius: 20px;
  width: 900px;
  height: 600px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
  overflow: hidden;
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #e0e0e0;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border-radius: 20px 20px 0 0;
  flex-shrink: 0;
}

.settings-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 24px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s ease;
}

.close-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
}

.close-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.settings-content {
  flex: 1;
  overflow: hidden;
  display: flex;
}

.settings-layout {
  display: flex;
  width: 100%;
  height: 100%;
}

.settings-sidebar {
  width: 250px;
  background: #f8f9fa;
  border-right: 1px solid #e0e0e0;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #2c3e50;
  font-size: 14px;
}

.sidebar-item:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.1);
}

.sidebar-item.active {
  background: rgba(102, 126, 234, 0.15);
  border-right: 3px solid #667eea;
  color: #667eea;
  font-weight: 600;
}

.sidebar-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.sidebar-icon {
  font-size: 18px;
  margin-right: 12px;
  width: 20px;
  text-align: center;
}

.sidebar-text {
  flex: 1;
  text-align: left;
}

.sidebar-divider {
  height: 1px;
  background: #e0e0e0;
  margin: 10px 20px;
}

.settings-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.settings-main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.settings-section {
  max-width: 500px;
}

.section-title {
  margin: 0 0 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.avatar-section {
  margin-bottom: 24px;
}

.avatar-container {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-preview {
  position: relative;
}

.avatar-image {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #667eea;
}

.avatar-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 20px;
  border: 3px solid #667eea;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.avatar-upload-btn {
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s ease;
}

.avatar-upload-btn:hover {
  background: #5a6fd8;
}

.avatar-remove-btn {
  padding: 8px 16px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s ease;
}

.avatar-remove-btn:hover {
  background: #c0392b;
}

.form-grid {
  display: grid;
  gap: 16px;
  margin-bottom: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-weight: 600;
  margin-bottom: 6px;
  color: #2c3e50;
  font-size: 14px;
}

.form-input {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.preferences-grid {
  display: grid;
  gap: 20px;
  margin-bottom: 24px;
}

.preference-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.preference-label {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.preference-select {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  background: white;
}

.toggle-item {
  border-bottom: none;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}

.toggle-input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

.toggle-input:checked + .toggle-slider {
  background-color: #667eea;
}

.toggle-input:checked + .toggle-slider:before {
  transform: translateX(20px);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.save-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .settings-modal {
    width: 95%;
    height: 90vh;
    border-radius: 16px;
  }

  .settings-layout {
    flex-direction: column;
  }

  .settings-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e0e0e0;
    padding: 0;
    max-height: 200px;
  }

  .sidebar-item {
    padding: 16px 20px;
  }

  .settings-main-content {
    padding: 20px;
  }

  .avatar-container {
    flex-direction: column;
    text-align: center;
  }

  .form-actions {
    justify-content: center;
  }

  .save-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .settings-header {
    padding: 20px;
  }

  .settings-main-content {
    padding: 16px;
  }

  .preference-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
