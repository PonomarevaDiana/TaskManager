<script>
import ContactService from '@/services/Contact-service.js'

export default {
  emits: ['showPushMessage', 'close'],
  name: 'ContactList',
  props: {
    globalData: {
      type: Object
    }
  },
  data() {
    return {
      searchQuery: '',
      newContactCode: '',
      friendship_key: '',
      showSecretCode: false,
      contacts: [],
      isLoading: false
    }
  },
  computed: {
    filteredContacts() {
      if (!this.searchQuery) {
        return this.contacts;
      }

      const query = this.searchQuery.toLowerCase();
      return this.contacts.filter(contact =>
        contact.name.toLowerCase().includes(query) ||
        (contact.email && contact.email.toLowerCase().includes(query))
      );
    },

    secretCode() {
      return this.friendship_key + '@' + (this.globalData.userName);
    },
  },
  methods: {
    closeContactList() {
      this.$emit('close');
    },

    getInitials(name) {
      return name.split(' ').map(word => word[0]).join('').toUpperCase();
    },

    async addContactByCode() {
      if (!this.newContactCode.trim()) {
        this.$emit('showPushMessage', 'Введите код контакта', 'OK', 3000);
        return;
      }

      this.isLoading = true;

      try {
        const response = await ContactService.sendRequest(this.newContactCode);

        if (response.status === 200) {
          this.$emit('showPushMessage', 'Запрос на добавление отправлен', 'OK', 5000);
          await this.loadContacts();
        } else {
          this.$emit('showPushMessage', 'Ошибка при отправке запроса', 'OK', 5000);
        }
      } catch (error) {
        console.error('Ошибка при отправке запроса:', error);

        if (error.response) {
          const errorMessage = error.response.data || 'Произошла ошибка';
          this.$emit('showPushMessage', errorMessage, 'OK', 5000);
        } else {
          this.$emit('showPushMessage', 'Ошибка подключения', 'OK', 5000);
        }
      } finally {
        this.isLoading = false;
        this.newContactCode = '';
      }
    },

    async removeContact(contact) {
      try {
        // Отправляем запрос на удаление контакта
        const response = await ContactService.deleteContact(contact.id);

        if (response.status === 204) {
          this.contacts = this.contacts.filter(c => c.id !== contact.id);
          this.$emit('showPushMessage', `Контакт ${contact.name} удален`, 'OK', 3000);
        } else {
          this.$emit('showPushMessage', 'Ошибка при удалении контакта', 'OK', 5000);
        }
      } catch (error) {
        console.error('Ошибка при удалении контакта:', error);
        this.$emit('showPushMessage', 'Ошибка при удалении контакта', 'OK', 5000);
      }
    },

    toggleSecretCode() {
      this.showSecretCode = !this.showSecretCode;
    },

    copyCode() {
      navigator.clipboard.writeText(this.secretCode)
        .then(() => {
          this.$emit('showPushMessage', 'Код скопирован в буфер обмена', 'OK', 3000);
        })
        .catch(err => {
          console.error('Ошибка копирования: ', err);
          const textArea = document.createElement('textarea');
          textArea.value = this.secretCode;
          document.body.appendChild(textArea);
          textArea.select();
          document.execCommand('copy');
          document.body.removeChild(textArea);
          this.$emit('showPushMessage', 'Код скопирован в буфер обмена', 'OK', 3000);
        });
    },

    transformContacts(serverContacts) {
      return serverContacts.map(contact => ({
        id: contact.contactId,
        name: contact.contactUserName || 'Неизвестный контакт',
        userId: contact.contactUserId,
        isPinned: contact.isPinned || false
      }));
    },

    async loadContacts() {
      this.isLoading = true;
      try {
        const response = await ContactService.getContacts();
        if (response.status === 200) {
          this.contacts = this.transformContacts(response.data);
          console.log("Контакты загружены:", this.contacts);
        } else {
          throw new Error('Ошибка загрузки контактов');
        }
      } catch (error) {
        console.error('Ошибка при загрузке контактов:', error);
        this.$emit('showPushMessage', 'Ошибка загрузки контактов', 'OK', 5000);
      } finally {
        this.isLoading = false;
      }
    },

    async refreshContacts() {
      await this.loadContacts();
      this.$emit('showPushMessage', 'Контакты обновлены', 'OK', 2000);
    }
  },

  async mounted() {
    document.body.style.overflow = 'hidden';

    try {
      const response = await ContactService.getKey(this.globalData.userId);
      if (response.status === 200) {
        this.friendship_key = response.data;
        console.log("Код дружбы получен:", this.friendship_key);
      }

      await this.loadContacts();
    } catch (error) {
      console.error('Ошибка инициализации:', error);
      this.$emit('showPushMessage', 'Ошибка загрузки данных', 'OK', 5000);
    }
  },

  beforeUnmount() {
    document.body.style.overflow = '';
  }
}
</script>

<template>
  <div class="contact-list-overlay" @click.self="closeContactList">
    <div class="contact-list-modal">
      <div class="contact-list-header">
        <h2>Список контактов</h2>
        <button class="close-btn" @click="closeContactList" :disabled="isLoading">×</button>
      </div>

      <div class="contact-list-content">
        <div class="add-contact-section">
          <div class="add-contact-input-wrapper">
            <input
              v-model="newContactCode"
              type="text"
              placeholder="Введите код контакта..."
              class="add-contact-input"
              :disabled="isLoading"
            />
            <button
              class="add-contact-btn"
              @click="addContactByCode"
              :disabled="isLoading || !newContactCode.trim()"
            >
              {{ isLoading ? 'Отправка...' : 'Добавить' }}
            </button>
          </div>
        </div>

        <div class="secret-code-section">
          <div class="secret-code-header" @click="toggleSecretCode">
            <span class="secret-code-title">Мой секретный код</span>
            <span class="toggle-icon">{{ showSecretCode ? '▼' : '▶' }}</span>
          </div>

          <div v-if="showSecretCode" class="secret-code-content">
            <div class="code-display">
              <code>{{ secretCode }}</code>
              <button class="copy-btn" @click="copyCode">Копировать</button>
            </div>
            <p class="code-description">Поделитесь этим кодом с другими пользователями, чтобы они могли добавить вас в контакты</p>
          </div>
        </div>

        <div class="search-section">
          <div class="search-input-wrapper">
            <span class="search-icon">🔍</span>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Поиск контактов..."
              class="search-input"
            />
          </div>
        </div>

        <div class="contacts-section">
          <div class="section-header">
            <span class="section-title">Контакты</span>
            <div class="header-actions">
              <span class="contacts-count">{{ filteredContacts.length }}</span>
              <button
                class="refresh-btn"
                @click="refreshContacts"
                :disabled="isLoading"
                title="Обновить контакты"
              >
                🔄
              </button>
            </div>
          </div>

          <div class="contacts-list">
            <div
              v-for="contact in filteredContacts"
              :key="contact.id"
              class="contact-item"
            >
              <div class="contact-avatar">
                <div class="avatar-placeholder">
                  {{ getInitials(contact.name) }}
                </div>
              </div>

              <div class="contact-info">
                <div class="contact-name">{{ contact.name }}</div>
                <div class="contact-username">{{ contact.name }}</div>
              </div>

              <div class="contact-actions">
                <button
                  v-if="contact.isPinned"
                  class="action-btn pin-btn"
                  title="Открепить"
                >
                  📌
                </button>
                <button
                  v-else
                  class="action-btn pin-btn"
                  title="Закрепить"
                >
                  📌
                </button>

                <button
                  class="action-btn delete-btn"
                  title="Удалить контакт"
                  @click="removeContact(contact)"
                >
                  ❌
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="no-contacts" v-if="isLoading">
          <div class="no-contacts-icon">⏳</div>
          <p>Загрузка контактов...</p>
        </div>

        <div class="no-contacts" v-else-if="contacts.length === 0">
          <div class="no-contacts-icon">👥</div>
          <p>Контактов пока нет</p>
          <p class="no-contacts-hint">Добавьте контакты с помощью кода выше</p>
        </div>

        <div class="no-contacts" v-else-if="filteredContacts.length === 0">
          <div class="no-contacts-icon">🔍</div>
          <p>Контакты не найдены</p>
          <p class="no-contacts-hint">Попробуйте изменить поисковый запрос</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contact-username {
  font-size: 11px;
  color: #7f8c8d;
  margin-top: 2px;
}

.pin-btn {
  background: rgba(255, 193, 7, 0.1);
}

.pin-btn:hover {
  background: rgba(255, 193, 7, 0.2);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.refresh-btn {
  background: rgba(102, 126, 234, 0.1);
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.2);
  transform: scale(1.1);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.no-contacts-hint {
  font-size: 12px;
  color: #7f8c8d;
  margin-top: 8px;
}

.add-contact-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.close-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.contact-list-overlay {
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

.contact-list-modal {
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

.contact-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #e0e0e0;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border-radius: 20px 20px 0 0;
}

.contact-list-header h2 {
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

.contact-list-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.add-contact-section {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.add-contact-input-wrapper {
  display: flex;
  gap: 10px;
}

.add-contact-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
}

.add-contact-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.add-contact-btn {
  padding: 12px 20px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.add-contact-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.secret-code-section {
  padding: 0 20px;
  border-bottom: 1px solid #f0f0f0;
}

.secret-code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  cursor: pointer;
  transition: background 0.3s ease;
  border-radius: 8px;
  margin: 0 -10px;
  padding-left: 10px;
  padding-right: 10px;
}

.secret-code-header:hover {
  background: #f8f9fa;
}

.secret-code-title {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.toggle-icon {
  color: #7f8c8d;
  font-size: 12px;
}

.secret-code-content {
  padding: 0 0 16px 0;
  animation: slideDown 0.3s ease;
}

.code-display {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.code-display code {
  flex: 1;
  background: #f8f9fa;
  padding: 10px 12px;
  border-radius: 8px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  border: 1px solid #e0e0e0;
  word-break: break-all;
}

.copy-btn {
  padding: 8px 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.copy-btn:hover {
  background: #5a6fd8;
}

.code-description {
  font-size: 12px;
  color: #7f8c8d;
  margin: 0;
  line-height: 1.4;
}

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

.contacts-section {
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

.contacts-count {
  background: #667eea;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.contacts-list {
  overflow-y: auto;
  max-height: 300px;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.3s ease;
}

.contact-item:hover {
  background: #f8f9fa;
}

.contact-item:last-child {
  border-bottom: none;
}

.contact-avatar {
  position: relative;
  margin-right: 16px;
}

.avatar-placeholder {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.contact-info {
  flex: 1;
}

.contact-name {
  font-weight: 600;
  margin-bottom: 4px;
  color: #2c3e50;
}

.contact-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.contact-item:hover .contact-actions {
  opacity: 1;
}

.action-btn {
  background: rgba(102, 126, 234, 0.1);
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: rgba(102, 126, 234, 0.2);
  transform: scale(1.1);
}

.delete-btn:hover {
  background: rgba(255, 59, 48, 0.2);
}

.no-contacts {
  text-align: center;
  padding: 40px 20px;
  color: #7f8c8d;
}

.no-contacts-icon {
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

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 200px;
  }
}

@media (max-width: 768px) {
  .contact-list-modal {
    width: 95%;
    max-height: 90vh;
    border-radius: 16px;
  }

  .contact-list-header {
    padding: 20px;
  }

  .contact-item {
    padding: 12px 16px;
  }

  .avatar-placeholder {
    width: 44px;
    height: 44px;
    font-size: 14px;
  }

  .add-contact-input-wrapper {
    flex-direction: column;
  }

  .code-display {
    flex-direction: column;
    align-items: stretch;
  }

  .copy-btn {
    align-self: flex-end;
  }

  .header-actions {
    flex-direction: column;
    gap: 5px;
  }
}
</style>
