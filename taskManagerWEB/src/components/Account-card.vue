<script>
export default {
  props: {
    isAuthenticated: Boolean,
    globalData: Object
  },
  emits: ['switchAuth'],
  computed: {
    accountDisplayName() {
      if (this.isAuthenticated && this.globalData) {
        return `${this.globalData.firstName} ${this.globalData.lastName}`;
      }
      return 'Требуется вход';
    },
    userInitial() {
      if (this.isAuthenticated && this.globalData && this.globalData.firstName) {
        return this.globalData.firstName.charAt(0);
      }
      return '?';
    }
  }
}
</script>

<template>
  <div class="account-card">
    <div class="card-header">
      <div class="user-avatar">
        <div class="avatar-icon">
          {{ userInitial }}
        </div>
        <span
          class="status-badge"
          :class="{ online: isAuthenticated, offline: !isAuthenticated }"
          :title="isAuthenticated ? 'Online' : 'Offline'"
        ></span>
      </div>

      <div class="user-info">
        <div class="user-name">{{ accountDisplayName }}</div>
        <div class="user-status">
          <span class="status-text">{{ isAuthenticated ? 'Online' : 'Offline' }}</span>
        </div>
      </div>
    </div>

    <button
      class="auth-toggle-btn"
      :class="{ 'logout-btn': isAuthenticated, 'login-btn': !isAuthenticated }"
      @click="$emit('switchAuth')"
    >
      <span class="btn-icon">{{ isAuthenticated ? '→' : '⇨' }}</span>
      {{ isAuthenticated ? 'Выйти' : 'Войти' }}
    </button>
  </div>
</template>

<style scoped>
.account-card {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  position: relative;
  overflow: hidden;
}

.account-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.1);
  transform: rotate(25deg);
  pointer-events: none;
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.user-avatar {
  position: relative;
  margin-right: 16px;
}

.avatar-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  color: white;
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.status-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid white;
}

.status-badge.online {
  background-color: #4cd964;
  box-shadow: 0 0 0 2px rgba(76, 217, 100, 0.4);
}

.status-badge.offline {
  background-color: #ff3b30;
  box-shadow: 0 0 0 2px rgba(255, 59, 48, 0.4);
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.user-status {
  display: flex;
  align-items: center;
}

.status-text {
  font-size: 14px;
  opacity: 0.9;
  font-weight: 500;
}

.auth-toggle-btn {
  width: 100%;
  padding: 12px 16px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
  overflow: hidden;
}

.auth-toggle-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.auth-toggle-btn:hover::before {
  left: 100%;
}

.login-btn {
  background: rgba(255, 255, 255, 0.9);
  color: #667eea;
}

.login-btn:hover {
  background: white;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.logout-btn {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.btn-icon {
  font-size: 18px;
  transition: transform 0.3s ease;
}

.auth-toggle-btn:hover .btn-icon {
  transform: translateX(3px);
}

@media (max-width: 480px) {
  .account-card {
    padding: 20px;
  }

  .avatar-icon {
    width: 50px;
    height: 50px;
    font-size: 20px;
  }

  .user-name {
    font-size: 16px;
  }
}
</style>
