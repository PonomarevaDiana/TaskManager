<script>
import NotificationService from '@/services/Notification-service.js'
import ContactService from '@/services/Contact-service.js'
import ProjectInvitationsService from "@/services/Project-invitations-service.js";

export default {
  name: 'NotificationList',
  props: {
    globalData: {
      type: Object
    }
  },
  data() {
    return {
      searchQuery: '',
      serverNotifications: [],
      notifications: [],
      selectedFilter: 'all',
      websocket: null,
      isConnected: false,
      reconnectAttempts: 0,
      maxReconnectAttempts: 5,
      processingActions: new Set()
    }
  },
  computed: {
    filteredNotifications() {
      let notificationsCopy = JSON.parse(JSON.stringify(this.notifications || []));

      if (this.selectedFilter === 'unread') {
        notificationsCopy = notificationsCopy.filter(notification => !notification.isRead);
      } else if (this.selectedFilter === 'read') {
        notificationsCopy = notificationsCopy.filter(notification => notification.isRead);
      }

      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase();
        notificationsCopy = notificationsCopy.filter(notification =>
          notification.title.toLowerCase().includes(query) ||
          notification.message.toLowerCase().includes(query) ||
          notification.senderName.toLowerCase().includes(query)
        );
      }

      return notificationsCopy.sort((a, b) => new Date(b.date) - new Date(a.date));
    },

    unreadCount() {
      return this.notifications.filter(n => !n.isRead).length;
    }
  },
  methods: {
    closeNotificationList() {
      this.$emit('close');
    },

    getInitials(name) {
      if (!name) return 'S';
      return name.split(' ').map(word => word[0]).join('').toUpperCase();
    },

    markAsRead(notification) {
      if (!notification.isRead) {
        notification.isRead = true;
        this.sendWebSocketMessage({
          type: 'MARK_AS_READ',
          notificationId: notification.id,
          userId: this.globalData.userId
        });
        console.log('Уведомление помечено как прочитанное:', notification.id);
      }
    },

    markAllAsRead() {
      this.notifications.forEach(notification => {
        if (!notification.isRead) {
          notification.isRead = true;
        }
      });

      this.sendWebSocketMessage({
        type: 'MARK_ALL_AS_READ',
        userId: this.globalData.userId
      });
      console.log('Все уведомления помечены как прочитанные');
    },

    async deleteNotification(notification) {
      try {
        this.notifications = this.notifications.filter(n => n.id !== notification.id);

        this.sendWebSocketMessage({
          type: 'DELETE_NOTIFICATION',
          notificationId: notification.id,
          userId: this.globalData.userId
        });
        console.log('Уведомление удалено:', notification.id);

        this.showActionFeedback('Удалено', 'Уведомление удалено');
      } catch (error) {
        console.error('Ошибка при удалении уведомления:', error);
        this.showActionFeedback('Ошибка', 'Не удалось удалить уведомление');
      }
    },

    async clearAllNotifications() {
      try {
        this.notifications = [];

        this.sendWebSocketMessage({
          type: 'CLEAR_ALL_NOTIFICATIONS',
          userId: this.globalData.userId
        });
        console.log('Все уведомления удалены');

        this.showActionFeedback('Очищено', 'Все уведомления удалены');
      } catch (error) {
        console.error('Ошибка при очистке уведомлений:', error);
        this.showActionFeedback('Ошибка', 'Не удалось очистить уведомления');
      }
    },

    async handleNotificationAction(notification, action) {
      const actionKey = `${notification.id}-${action}`;
      if (this.processingActions.has(actionKey)) {
        return;
      }

      this.processingActions.add(actionKey);

      try {
        console.log('Действие с уведомлением:', notification.id, action);

        if (notification.type === 'CONTACT_REQUEST') {
          await this.handleContactRequestAction(notification, action);
        } else if (notification.type === 'PROJECT_INVITATION') {
          await this.handleProjectInvitationAction(notification, action);
        } else {
          /*
          this.sendWebSocketMessage({
            type: 'NOTIFICATION_ACTION',
            notificationId: notification.id,
            action: action,
            userId: this.globalData.userId
          }); */

          this.handleGenericNotificationAction(notification, action);
        }

      } catch (error) {
        console.error('Ошибка при обработке действия:', error);
        this.showActionFeedback('Ошибка', 'Не удалось выполнить действие');
      } finally {
        this.processingActions.delete(actionKey);
      }
    },

    async handleContactRequestAction(notification, action) {
      const currentUserId = this.globalData.userId;
      const targetUserId = notification.senderId;

      if (!targetUserId) {
        throw new Error('Не указан ID отправителя запроса');
      }

      switch (action) {
        case 'accept':
          await ContactService.acceptRequest(targetUserId);
          this.showActionFeedback('Принято', 'Запрос в контакты принят');
          this.removeNotificationFromList(notification.id);
          this.$emit('contact-accepted', targetUserId);
          break;

        case 'decline':
          await ContactService.declineRequest(targetUserId);
          this.showActionFeedback('Отклонено', 'Запрос в контакты отклонен');
          this.removeNotificationFromList(notification.id);
          break;

        default:
          console.warn('Неизвестное действие для CONTACT_REQUEST:', action);
      }
    },

    async handleProjectInvitationAction(notification, action) {
      const invitationId = notification.invitationId;

      if (!invitationId) {
        throw new Error('Не указан ID приглашения');
      }

      switch (action) {
        case 'accept':
          await ProjectInvitationsService.acceptInvitation(invitationId);
          this.showActionFeedback('Принято', 'Приглашение в проект принято');
          this.removeNotificationFromList(notification.id);
          this.$emit('project-invitation-accepted', invitationId);
          break;

        case 'decline':
          await ProjectInvitationsService.declineInvitation(invitationId);
          this.showActionFeedback('Отклонено', 'Приглашение в проект отклонено');
          this.removeNotificationFromList(notification.id);
          break;

        default:
          console.warn('Неизвестное действие для PROJECT_NOTIFICATION:', action);
      }
    },

    handleGenericNotificationAction(notification, action) {
      switch (action) {
        case 'accept':
          this.showActionFeedback('Принято', notification.title);
          this.removeNotificationFromList(notification.id);
          break;
        case 'decline':
          this.showActionFeedback('Отклонено', notification.title);
          this.removeNotificationFromList(notification.id);
          break;
        case 'confirm':
          this.showActionFeedback('Подтверждено', notification.title);
          this.markAsRead(notification);
          break;
        case 'cancel':
          this.showActionFeedback('Отменено', notification.title);
          this.removeNotificationFromList(notification.id);
          break;
        case 'view':
          this.showActionFeedback('Просмотрено', notification.title);
          this.markAsRead(notification);
          break;
        default:
          console.log('Неизвестное действие:', action);
      }
    },

    removeNotificationFromList(notificationId) {
      this.notifications = this.notifications.filter(n => n.id !== notificationId);
    },

    showActionFeedback(action, title) {
      console.log(`${action}: ${title}`);
    },

    connectWebSocket() {
      try {
        let wsUrl;

        if (process.env.NODE_ENV === 'development') {
          const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
          wsUrl = `${protocol}//localhost:8080/ws/notifications?userId=${this.globalData.userId}`;
        } else {
          const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
          const host = window.location.host;
          wsUrl = `${protocol}//${host}/ws/notifications?userId=${this.globalData.userId}`;
        }

        this.websocket = new WebSocket(wsUrl)
        this.websocket.onopen = this.handleWebSocketOpen.bind(this)
        this.websocket.onmessage = this.handleWebSocketMessage.bind(this)
        this.websocket.onclose = this.handleWebSocketClose.bind(this)
        this.websocket.onerror = this.handleWebSocketError.bind(this)

        console.log('WebSocket подключение инициировано')
      } catch (error) {
        console.error('Ошибка подключения WebSocket:', error);
        this.scheduleReconnect();
      }
    },

    getWebSocketUrl() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.host;
      const path = '/ws/notifications';
      return `${protocol}//${host}${path}?userId=${this.globalData.userId}&token=${this.getAuthToken()}`;
    },

    getAuthToken() {
      return localStorage.getItem('authToken') || this.globalData.token || '';
    },

    handleWebSocketOpen(event) {
      console.log('WebSocket подключен ', event);
      this.isConnected = true;
      this.reconnectAttempts = 0;

      this.sendWebSocketMessage({
        type: 'GET_NOTIFICATIONS',
        userId: this.globalData.userId
      });
    },

    handleWebSocketMessage(event) {
      try {
        const data = JSON.parse(event.data);
        console.log('Получено сообщение WebSocket:', data);

        this.processWebSocketMessage(data);
      } catch (error) {
        console.error('Ошибка обработки WebSocket сообщения:', error);
      }
    },

    processWebSocketMessage(data) {
      switch (data.type) {
        case 'NEW_NOTIFICATION':
          this.addNewNotification(data.notification);
          break;
        case 'NOTIFICATIONS_LIST':
          const notifs = data.notifications || data.data || [];
          this.updateNotificationsList(notifs);
          break;
        case 'NOTIFICATION_UPDATED':
          this.updateNotification(data.notification);
          break;
        case 'NOTIFICATION_DELETED':
          this.removeNotification(data.notificationId);
          break;
        case 'NOTIFICATION_READ':
          this.markAsRead(data.notification)
          break;
        case 'PONG':
          console.log('Pong received');
          break;
        default:
          console.warn('Неизвестный тип сообщения:', data.type);
      }
    },

    handleWebSocketClose(event) {
      console.log('WebSocket отключен:', event.code, event.reason);
      this.isConnected = false;

      if (event.code !== 1000) {
        this.scheduleReconnect();
      }
    },

    handleWebSocketError(error) {
      console.error('WebSocket ошибка:', error);
      this.isConnected = false;
    },

    scheduleReconnect() {
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++;
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);

        console.log(`Попытка переподключения ${this.reconnectAttempts} через ${delay}ms`);

        setTimeout(() => {
          this.connectWebSocket();
        }, delay);
      } else {
        console.error('Превышено максимальное количество попыток переподключения');
      }
    },

    sendWebSocketMessage(message) {
      if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
        this.websocket.send(JSON.stringify(message));
      } else {
        console.warn('WebSocket не подключен, сообщение не отправлено:', message);
      }
    },

    addNewNotification(notificationData) {
      const newNotification = this.transformNotifications([notificationData])[0];

      newNotification.isNew = true;

      this.notifications.unshift(newNotification);

      if (!this.$el || this.$el.offsetParent === null) {
        this.showDesktopNotification(newNotification);
      }

      setTimeout(() => {
        const notification = this.notifications.find(n => n.id === newNotification.id);
        if (notification) {
          notification.isNew = false;
        }
      }, 3000);

      console.log('Добавлено новое уведомление:', newNotification);
    },

    showDesktopNotification(notification) {
      if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(notification.title, {
          body: notification.message,
          icon: '/favicon.ico'
        });
      }
    },

    updateNotificationsList(notifications) {
      if (!notifications || !Array.isArray(notifications)) {
        console.warn('Получены некорректные уведомления:', notifications);
        this.notifications = [];
        return;
      }

      this.serverNotifications = notifications;
      this.notifications = this.transformNotifications(notifications);
      console.log('Список уведомлений обновлен через WebSocket');
    },

    updateNotification(updatedNotification) {
      const index = this.notifications.findIndex(n => n.id === updatedNotification.id);
      if (index !== -1) {
        this.notifications.splice(index, 1, this.transformNotifications([updatedNotification])[0]);
        console.log('Уведомление обновлено:', updatedNotification.id);
      }
    },

    removeNotification(notificationId) {
      this.notifications = this.notifications.filter(n => n.id !== notificationId);
      console.log('Уведомление удалено через WebSocket:', notificationId);
    },

    transformNotifications(serverNotifications) {
      if (!serverNotifications || !Array.isArray(serverNotifications)) {
        return [];
      }

      return serverNotifications.map(notification => ({
        id: notification.id || Date.now(),
        title: notification.title || 'Новое уведомление',
        message: notification.message || 'У вас новое уведомление',
        senderName: this.getSenderName(notification.senderId, notification.type),
        senderId: notification.senderId,
        type: notification.type || 'info',
        timestamp: notification.date || new Date().toISOString(),
        isRead: notification.isRead || false,
        hasActions: this.hasActions(notification.type),
        actions: this.getActionsForType(notification.type),
        priority: this.getPriorityForType(notification.type),
        isNew: notification.isNew || false,
        invitationId: notification.invitationId || null
      }));
    },

    getSenderName(senderId, type) {
      console.log(type)
      if (!senderId) return 'Система';

      const userNames = {
        'user123': 'Алексей Петров',
        'user456': 'Мария Иванова',
        'user789': 'Дмитрий Сидоров',
        'user001': 'Анна Козлова',
        'user002': 'Сергей Волков'
      };

      return userNames[senderId] || 'Пользователь';
    },

    hasActions(type) {
      const typesWithActions = ['CONTACT_REQUEST', 'friend_request', 'invitation', 'confirmation', 'PROJECT_INVITATION'];
      return typesWithActions.includes(type);
    },

    getActionsForType(type) {
      const actionsMap = {
        CONTACT_REQUEST: ['accept', 'decline'],
        CONTACT_REQUEST_ACCEPTED:['view'],
        friend_request: ['accept', 'decline'],
        invitation: ['accept', 'decline'],
        confirmation: ['confirm', 'cancel'],
        message: ['view'],
        system: ['view'],
        PROJECT_INVITATION: ['accept', 'decline'],
        TASK_ASSIGMENT: ['view']
      };
      return actionsMap[type] || [];
    },

    getPriorityForType(type) {
      const priorityMap = {
        CONTACT_REQUEST: 'high',
        CONTACT_REQUEST_ACCEPTED: 'high',
        PROJECT_INVITATION: 'high',
        TASK_ASSIGMENT: 'high',
        error: 'high',
        warning: 'medium',
        friend_request: 'medium',
        invitation: 'medium',
        confirmation: 'medium',
        info: 'low',
        system: 'low',
        message: 'low'
      };
      return priorityMap[type] || 'normal';
    },

    getNotificationIcon(type) {
      const icons = {
        CONTACT_REQUEST: '👤',
        CONTACT_REQUEST_ACCEPTED: '👤',
        info: '💡',
        warning: '⚠️',
        success: '✅',
        error: '❌',
        friend_request: '👤',
        invitation: '📨',
        confirmation: '✅',
        message: '💬',
        system: '⚙️',
        PROJECT_INVITATION: '📁',
        TASK_ASSIGMENT: '✅'
      };
      return icons[type] || '🔔';
    },

    getNotificationClass(type) {
      const classes = {
        CONTACT_REQUEST: 'notification-contact-request',
        CONTACT_REQUEST_ACCEPTED: 'notification-contact-request',
        info: 'notification-info',
        warning: 'notification-warning',
        success: 'notification-success',
        error: 'notification-error',
        friend_request: 'notification-friend-request',
        invitation: 'notification-invitation',
        confirmation: 'notification-confirmation',
        message: 'notification-message',
        system: 'notification-system',
        PROJECT_INVITATION: 'notification-project-invitation'
      };
      return classes[type] || 'notification-info';
    },

    formatTime(timestamp) {
      if (!timestamp) return 'только что';

      const now = new Date();
      const notificationTime = new Date(timestamp);
      const diffMs = now - notificationTime;
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMs / 3600000);
      const diffDays = Math.floor(diffMs / 86400000);

      if (diffMins < 1) return 'только что';
      if (diffMins < 60) return `${diffMins} мин назад`;
      if (diffHours < 24) return `${diffHours} ч назад`;
      if (diffDays < 7) return `${diffDays} дн назад`;

      return notificationTime.toLocaleDateString('ru-RU');
    },

    async loadNotifications() {
      try {
        const response = await NotificationService.getNotificationsById(this.globalData.userId);
        if (response.status === 200) {
          this.serverNotifications = response.data;
          this.notifications = this.transformNotifications(this.serverNotifications);
          console.log("Уведомления загружены через HTTP:", this.notifications);
        }
      } catch (error) {
        console.error('Ошибка при загрузке уведомлений через HTTP:', error);
        this.generateDemoNotifications();
      }
    },

    generateDemoNotifications() {
      const demoNotifications = [
        {
          id: 1,
          owner: this.globalData.userId,
          type: 'CONTACT_REQUEST',
          title: 'Новый запрос в контакты',
          date: new Date(Date.now() - 5 * 60000).toISOString(),
          isRead: false,
          message: 'Алексей Петров хочет добавить вас в контакты',
          senderId: 'user123'
        },
        {
          id: 2,
          owner: this.globalData.userId,
          type: 'message',
          title: 'Новое сообщение',
          date: new Date(Date.now() - 30 * 60000).toISOString(),
          isRead: true,
          message: 'У вас новое сообщение в чате',
          senderId: 'user456'
        },
        {
          id: 3,
          owner: this.globalData.userId,
          type: 'system',
          title: 'Обновление системы',
          date: new Date(Date.now() - 2 * 3600000).toISOString(),
          isRead: false,
          message: 'Система была обновлена до версии 2.0',
          senderId: null
        }
      ];

      this.notifications = this.transformNotifications(demoNotifications);
    },

    requestNotificationPermission() {
      if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
      }
    },

    startPing() {
      setInterval(() => {
        if (this.isConnected) {
          this.sendWebSocketMessage({ type: 'PING' });
        }
      }, 30000);
    }
  },

  async mounted() {
    document.body.style.overflow = 'hidden';

    this.requestNotificationPermission();

    this.connectWebSocket();

    this.startPing();

    setTimeout(() => {
      if (!this.isConnected) {
        console.log('WebSocket не подключен, используем HTTP fallback');
        this.loadNotifications();
      }
    }, 2000);
  },

  beforeUnmount() {
    document.body.style.overflow = '';

    if (this.websocket) {
      this.websocket.close(1000, 'Component unmounted');
    }
  }
}
</script>

<template>
  <div class="notification-list-overlay" @click.self="closeNotificationList">
    <div class="notification-list-modal">
      <div class="notification-list-header">
        <h2>Уведомления</h2>
        <div class="header-actions">
          <span class="unread-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
          <button class="close-btn" @click="closeNotificationList">×</button>
        </div>
      </div>

      <div class="notification-list-content">
        <div class="notification-controls">
          <div class="filter-buttons">
            <button
              class="filter-btn"
              :class="{ active: selectedFilter === 'all' }"
              @click="selectedFilter = 'all'"
            >
              Все
            </button>
            <button
              class="filter-btn"
              :class="{ active: selectedFilter === 'unread' }"
              @click="selectedFilter = 'unread'"
            >
              Непрочитанные
            </button>
            <button
              class="filter-btn"
              :class="{ active: selectedFilter === 'read' }"
              @click="selectedFilter = 'read'"
            >
              Прочитанные
            </button>
          </div>

          <div class="action-buttons">
            <button
              class="action-btn mark-all-read"
              @click="markAllAsRead"
              :disabled="unreadCount === 0"
            >
              Прочитать все
            </button>
            <button
              class="action-btn clear-all"
              @click="clearAllNotifications"
              :disabled="notifications.length === 0"
            >
              Очистить все
            </button>
          </div>
        </div>

        <!-- Поиск -->
        <div class="search-section">
          <div class="search-input-wrapper">
            <span class="search-icon">🔍</span>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Поиск уведомлений..."
              class="search-input"
            />
          </div>
        </div>

        <!-- Список уведомлений -->
        <div class="notifications-section">
          <div class="section-header">
            <span class="section-title">Уведомления</span>
            <span class="notifications-count">Всего: {{ filteredNotifications.length }}</span>
          </div>

          <div class="notifications-list">
            <div
              v-for="notification in filteredNotifications"
              :key="notification.id"
              class="notification-item"
              :class="[getNotificationClass(notification.type), {
                unread: !notification.isRead,
                'new-notification': notification.isNew
              }]"
              @click="markAsRead(notification)"
            >
              <div class="notification-icon">
                <span class="icon">{{ getNotificationIcon(notification.type) }}</span>
                <div class="unread-indicator" v-if="!notification.isRead"></div>
              </div>

              <div class="notification-content">
                <div class="notification-header">
                  <div class="notification-title">
                    {{ notification.title }}
                    <span class="new-badge" v-if="notification.isNew">NEW</span>
                  </div>
                  <div class="notification-time">{{ formatTime(notification.timestamp) }}</div>
                </div>

                <div class="notification-message">{{ notification.message }}</div>

                <div class="notification-sender" v-if="notification.senderName && notification.senderName !== 'Система'">
                  От: {{ notification.senderName }}
                </div>

                <!-- Кнопки действий -->
                <div class="notification-actions" v-if="notification.hasActions && notification.actions.length > 0">
                  <button
                    v-for="action in notification.actions"
                    :key="action"
                    class="notification-action-btn"
                    :class="action"
                    @click.stop="handleNotificationAction(notification, action)"
                    :disabled="processingActions.has(`${notification.id}-${action}`)"
                  >
                    <span v-if="processingActions.has(`${notification.id}-${action}`)">...</span>
                    <span v-else>
                      {{ action === 'accept' ? 'Принять' :
                         action === 'decline' ? 'Отклонить' :
                         action === 'confirm' ? 'Подтвердить' :
                         action === 'cancel' ? 'Отмена' :
                         action === 'view' ? 'Посмотреть' : action }}
                    </span>
                  </button>
                </div>
              </div>

              <div class="notification-actions-menu">
                <button
                  class="action-btn delete-btn"
                  title="Удалить уведомление"
                  @click.stop="deleteNotification(notification)"
                >
                  ❌
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Состояние пустого списка -->
        <div class="no-notifications" v-if="filteredNotifications.length === 0 && notifications.length === 0">
          <div class="no-notifications-icon">🔔</div>
          <p>Загрузка уведомлений...</p>
        </div>

        <div class="no-notifications" v-else-if="filteredNotifications.length === 0">
          <div class="no-notifications-icon">📭</div>
          <p>Уведомлений не найдено</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.notification-action-btn:disabled:hover {
  opacity: 0.6;
  transform: none;
}

.new-notification {
  animation: pulse 2s ease-in-out;
  background: linear-gradient(90deg, #f8fbff 0%, #e3f2fd 50%, #f8fbff 100%) !important;
}

@keyframes pulse {
  0% { background-color: #f8fbff; }
  50% { background-color: #e3f2fd; }
  100% { background-color: #f8fbff; }
}

.new-badge {
  background: #ff4757;
  color: white;
  padding: 2px 6px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  margin-left: 8px;
}

.notification-list-overlay {
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

.notification-list-modal {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

.notification-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #e0e0e0;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border-radius: 20px 20px 0 0;
}

.notification-list-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.unread-badge {
  background: #ff4757;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
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

.notification-list-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Элементы управления */
.notification-controls {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-buttons {
  display: flex;
  gap: 8px;
}

.filter-btn {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.filter-btn.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.filter-btn:hover:not(.active) {
  background: #f8f9fa;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.mark-all-read {
  background: #4cd964;
  color: white;
}

.mark-all-read:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.clear-all {
  background: #ff4757;
  color: white;
}

.clear-all:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* Поиск */
.search-section {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  font-size: 16px;
  opacity: 0.6;
}

.search-input {
  width: 100%;
  padding: 12px 12px 12px 40px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
}

/* Секция уведомлений */
.notifications-section {
  flex: 1;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8f9fa;
}

.section-title {
  font-weight: 600;
  color: #2c3e50;
}

.notifications-count {
  background: #667eea;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.notifications-list {
  overflow-y: auto;
  max-height: 400px;
}

.notification-item {
  display: flex;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.notification-item:hover {
  background: #f8f9fa;
}

.notification-item.unread {
  background: #f8fbff;
  border-left: 3px solid #667eea;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-contact-request {
  border-left-color: #3742fa;
}

.notification-info {
  border-left-color: #667eea;
}

.notification-warning {
  border-left-color: #ffa502;
}

.notification-success {
  border-left-color: #2ed573;
}

.notification-error {
  border-left-color: #ff4757;
}

.notification-friend-request {
  border-left-color: #3742fa;
}

.notification-invitation {
  border-left-color: #a55eea;
}

.notification-confirmation {
  border-left-color: #2ed573;
}

.notification-message {
  border-left-color: #1e90ff;
}

.notification-system {
  border-left-color: #747d8c;
}

.notification-icon {
  position: relative;
  margin-right: 16px;
  display: flex;
  align-items: center;
}

.icon {
  font-size: 24px;
}

.unread-indicator {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4757;
}

.notification-content {
  flex: 1;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.notification-title {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.notification-time {
  font-size: 11px;
  color: #7f8c8d;
  margin-left: 10px;
}

.notification-message {
  font-size: 13px;
  color: #555;
  margin-bottom: 6px;
  line-height: 1.4;
}

.notification-sender {
  font-size: 11px;
  color: #7f8c8d;
  font-style: italic;
}

.notification-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.notification-action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.notification-action-btn.accept,
.notification-action-btn.confirm {
  background: #2ed573;
  color: white;
}

.notification-action-btn.decline,
.notification-action-btn.cancel {
  background: #ff4757;
  color: white;
}

.notification-action-btn.view {
  background: #667eea;
  color: white;
}

.notification-action-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.notification-actions-menu {
  display: flex;
  align-items: flex-start;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.notification-item:hover .notification-actions-menu {
  opacity: 1;
}

.delete-btn {
  background: rgba(255, 71, 87, 0.1);
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  background: rgba(255, 71, 87, 0.2);
  transform: scale(1.1);
}

.no-notifications {
  text-align: center;
  padding: 40px 20px;
  color: #7f8c8d;
}

.no-notifications-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
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
  .notification-list-modal {
    width: 95%;
    max-height: 90vh;
    border-radius: 16px;
  }

  .notification-list-header {
    padding: 20px;
  }

  .notification-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-buttons {
    justify-content: center;
  }

  .action-buttons {
    justify-content: center;
  }

  .notification-item {
    padding: 12px 16px;
  }

  .notification-header {
    flex-direction: column;
    gap: 4px;
  }

  .notification-time {
    margin-left: 0;
  }

  .notification-actions {
    flex-wrap: wrap;
  }
}
</style>
