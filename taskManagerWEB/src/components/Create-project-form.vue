<script>
import ProjectService from '@/services/Project-service.js'
import ContactService from '@/services/Contact-service.js'

export default {
  name: 'CreateProjectForm',
  emits: ['project-created', 'project-updated', 'close'],
  props: {
    isVisible: {
      type: Boolean,
      default: false,
    },
    editingProject: {
      type: Object,
      default: null,
    },
    currentUser: Object,
  },
  data() {
    return {
      loading: false,
      loadingUsers: false,
      error: null,
      usersError: null,
      searchQuery: '',
      availableUsers: [],
      projectDto: {
        name: '',
        description: '',
        assignedUsers: [],
      },
      currentProjectMembers: [],
      removedMembers: [],
    }
  },
  computed: {
    filteredUsers() {
      if (!this.searchQuery) {
        return this.availableUsers
      }

      const query = this.searchQuery.toLowerCase()
      return this.availableUsers.filter(
        (user) =>
          user.name.toLowerCase().includes(query) ||
          (user.email && user.email.toLowerCase().includes(query)),
      )
    },

    selectedUsers() {
      return this.availableUsers.filter((user) => this.projectDto.assignedUsers.includes(user.id))
    },

    displayMembers() {
      return this.currentProjectMembers.map((member) => {
        const userInfo = this.availableUsers.find((u) => u.id === member.userId)
        return {
          ...member,
          displayName: userInfo?.name || member.username || 'Неизвестный участник',
          isOwner: member.role === 'ROLE_OWNER',
          isCurrentUser: member.userId === this.currentUser?.id,
          isRemoved: this.removedMembers.includes(member.userId),
        }
      })
    },

    keptMembers() {
      return this.displayMembers.filter(
        (member) => member.isOwner || !this.removedMembers.includes(member.userId),
      )
    },

    isEditMode() {
      return !!this.editingProject
    },
  },
  watch: {
    isVisible: {
      async handler(newVal) {
        if (newVal) {
          await this.loadContacts()
          if (this.editingProject) {
            await this.initializeForm()
          } else {
            this.resetForm()
          }
        }
      },
      immediate: true,
    },
  },
  methods: {
    async initializeForm() {
      if (!this.editingProject) return

      try {
        await this.loadProjectMembers()

        this.projectDto = {
          name: this.editingProject.name || '',
          description: this.editingProject.description || '',
          assignedUsers: [],
        }
      } catch (error) {
        console.error('Error initializing form:', error)
        this.error = 'Ошибка загрузки данных проекта'
      }
    },

    async loadProjectMembers() {
      if (!this.editingProject?.id) return

      try {
        const response = await ProjectService.getProjectMembers(this.editingProject.id)
        this.currentProjectMembers = response.data || []
      } catch (error) {
        console.error('Error loading project members:', error)
        this.currentProjectMembers = []
      }
    },

    transformToBackendFormat(frontendData) {
      if (!this.currentUser?.id) {
        throw new Error('Текущий пользователь не определен')
      }

      const baseData = {
        name: (frontendData.name || '').trim(),
        description: (frontendData.description || '').trim(),
      }

      if (!this.isEditMode) {
        const memberIds = [
          this.currentUser.id,
          ...(frontendData.assignedUsers || []).filter((id) => id !== this.currentUser.id),
        ]

        return {
          ...baseData,
          memberIds: memberIds,
        }
      }

      const keptMemberIds = this.currentProjectMembers
        .filter((member) => !this.removedMembers.includes(member.userId))
        .map((member) => member.userId)

      const newMemberIds = (frontendData.assignedUsers || []).filter(
        (userId) => !this.currentProjectMembers.some((m) => m.userId === userId),
      )

      const allMemberIds = [...new Set([...keptMemberIds, ...newMemberIds])]

      if (!allMemberIds.includes(this.currentUser.id)) {
        allMemberIds.push(this.currentUser.id)
      }

      return {
        ...baseData,
        memberIds: allMemberIds,
      }
    },

    async saveProject() {
      if (this.loading) return

      this.loading = true
      this.error = null

      try {
        if (!this.projectDto?.name?.trim()) {
          throw new Error('Название проекта не может быть пустым')
        }

        const backendData = this.transformToBackendFormat(this.projectDto)

        if (!backendData.memberIds || backendData.memberIds.length === 0) {
          throw new Error('Должен быть хотя бы один участник проекта')
        }

        console.log('Sending to backend:', JSON.stringify(backendData, null, 2))

        let response
        if (this.isEditMode) {
          response = await ProjectService.updateProject(this.editingProject.id, backendData)

          const [updatedProjectResponse, membersResponse] = await Promise.all([
            ProjectService.getProject(this.editingProject.id),
            ProjectService.getProjectMembers(this.editingProject.id),
          ])

          const projectWithMembers = {
            ...updatedProjectResponse.data,
            members: membersResponse.data || [],
          }

          this.$emit('project-updated', projectWithMembers)
        } else {
          response = await ProjectService.createProject(backendData)
          this.$emit('project-created', response.data)
        }

        this.closeForm()
      } catch (error) {
        console.error('Save project error:', error)
        this.error = this.getErrorMessage(error)

        if (error.response) {
          console.error('Response data:', error.response.data)
        }
      } finally {
        this.loading = false
      }
    },

    getErrorMessage(error) {
      if (error.response) {
        const data = error.response.data
        if (data && typeof data === 'object') {
          return data.message || data.error || `Ошибка сервера: ${error.response.status}`
        }
        return `Ошибка сервера: ${error.response.status}`
      } else if (error.request) {
        return 'Не удалось соединиться с сервером. Проверьте подключение к интернету.'
      } else {
        return error.message || 'Ошибка при сохранении проекта'
      }
    },

    async loadContacts() {
      if (this.loadingUsers) return

      this.loadingUsers = true
      this.usersError = null

      try {
        if (!this.currentUser?.id) {
          throw new Error('Текущий пользователь не определен')
        }

        const response = await ContactService.getContacts(this.currentUser.id)

        if (response.data && Array.isArray(response.data)) {
          this.availableUsers = response.data
            .map((contact) => ({
              id: contact.contactUserId,
              name: contact.contactUserName,
              contactId: contact.contactId,
              isPinned: contact.isPinned || false,
            }))
            .filter((contact) => contact.id)
        } else {
          this.availableUsers = []
        }
      } catch (error) {
        console.error('Ошибка загрузки контактов:', error)
        this.usersError = 'Не удалось загрузить список контактов'
        this.availableUsers = []
      } finally {
        this.loadingUsers = false
      }
    },

    clearSearch() {
      this.searchQuery = ''
    },

    removeUser(userId) {
      this.projectDto.assignedUsers = this.projectDto.assignedUsers.filter((id) => id !== userId)
    },

    removeMember(userId) {
      if (!this.removedMembers.includes(userId)) {
        this.removedMembers.push(userId)
      }
      this.removeUser(userId)
    },

    restoreMember(userId) {
      this.removedMembers = this.removedMembers.filter((id) => id !== userId)
      if (!this.projectDto.assignedUsers.includes(userId)) {
        this.projectDto.assignedUsers.push(userId)
      }
    },

    closeForm() {
      this.resetForm()
      this.$emit('close')
    },

    resetForm() {
      this.projectDto = {
        name: '',
        description: '',
        assignedUsers: [],
      }
      this.searchQuery = ''
      this.error = null
      this.usersError = null
      this.currentProjectMembers = []
      this.removedMembers = []
    },

    getRoleDisplayName(role) {
      const roleNames = {
        ROLE_OWNER: 'Владелец',
        ROLE_ADMIN: 'Администратор',
        ROLE_USER: 'Участник',
      }
      return roleNames[role] || role
    },
  },
}
</script>

<template>
  <div v-if="isVisible" class="settings-overlay" @click.self="closeForm">
    <div class="settings-modal">
      <div class="settings-header">
        <h2>{{ editingProject ? 'Редактировать проект' : 'Создать новый проект' }}</h2>
        <button class="close-btn" @click="closeForm" :disabled="loading">×</button>
      </div>

      <div class="settings-content">
        <form @submit.prevent="saveProject" class="project-form">
          <div v-if="error" class="error-message">
            {{ error }}
          </div>

          <div class="settings-section">
            <h3 class="section-title">Основная информация</h3>

            <div class="form-group">
              <label for="projectName" class="form-label">Название проекта:</label>
              <input
                type="text"
                id="projectName"
                v-model="projectDto.name"
                required
                class="form-input"
                placeholder="Введите название проекта"
                :disabled="loading"
              />
            </div>

            <div class="form-group">
              <label for="projectDescription" class="form-label">Описание:</label>
              <textarea
                id="projectDescription"
                v-model="projectDto.description"
                class="form-input"
                placeholder="Опишите ваш проект..."
                :disabled="loading"
                rows="4"
              ></textarea>
            </div>
          </div>

          <div v-if="editingProject && displayMembers.length > 0" class="settings-section">
            <h3 class="section-title">Текущие участники проекта</h3>

            <div class="current-members-section">
              <div class="members-list">
                <div
                  v-for="member in displayMembers"
                  :key="member.userId"
                  class="member-item"
                  :class="{
                    owner: member.isOwner,
                    'current-user': member.isCurrentUser,
                    removed: member.isRemoved,
                  }"
                >
                  <div class="member-avatar">
                    {{ member.isOwner ? '👑' : '👤' }}
                  </div>
                  <div class="member-info">
                    <span class="member-name">{{ member.displayName }}</span>
                    <span class="member-role" :class="member.role.toLowerCase()">
                      {{ getRoleDisplayName(member.role) }}
                    </span>
                  </div>
                  <div class="member-actions" v-if="!member.isOwner">
                    <button
                      v-if="!member.isRemoved"
                      @click="removeMember(member.userId)"
                      class="remove-member-btn"
                      :disabled="loading"
                      title="Удалить из проекта"
                    >
                      ×
                    </button>
                    <button
                      v-else
                      @click="restoreMember(member.userId)"
                      class="restore-member-btn"
                      :disabled="loading"
                      title="Восстановить в проекте"
                    >
                      ↶
                    </button>
                  </div>
                </div>
              </div>

              <div class="members-stats">
                <span class="total-members">
                  Останутся в проекте: {{ keptMembers.length }} из {{ displayMembers.length }}
                </span>
                <span class="selection-hint" v-if="removedMembers.length > 0">
                  Удалено участников: {{ removedMembers.length }}. Нажмите ↶ чтобы восстановить.
                </span>
                <span class="selection-hint" v-else>
                  Нажмите × чтобы удалить участника из проекта
                </span>
              </div>
            </div>
          </div>

          <div class="settings-section">
            <h3 class="section-title">{{ editingProject ? 'Добавить новых участников' : 'Участники проекта' }}</h3>

            <div class="assignees-section">
              <div class="search-container">
                <input
                  type="text"
                  v-model="searchQuery"
                  placeholder="Поиск участников..."
                  class="form-input search-input"
                  :disabled="loadingUsers"
                />
                <button
                  v-if="searchQuery"
                  @click="clearSearch"
                  class="clear-search-btn"
                  type="button"
                  :disabled="loadingUsers"
                >
                  ×
                </button>
              </div>

              <div v-if="loadingUsers" class="loading-users">Загрузка контактов...</div>

              <div v-else-if="usersError" class="error-users">
                {{ usersError }}
              </div>

              <div v-else class="assignees-list">
                <div
                  v-for="user in filteredUsers"
                  :key="user.id"
                  class="assignee-item"
                  :class="{
                    'pinned-contact': user.isPinned,
                    'already-member':
                      editingProject &&
                      displayMembers.some((m) => m.userId === user.id && !m.isRemoved),
                  }"
                >
                  <label class="checkbox-label">
                    <input
                      type="checkbox"
                      :value="user.id"
                      v-model="projectDto.assignedUsers"
                      :disabled="
                        loading ||
                        (editingProject &&
                          displayMembers.some((m) => m.userId === user.id && !m.isRemoved))
                      "
                    />
                    <div class="user-info">
                      <span class="user-name">{{ user.name }}</span>
                      <span v-if="user.isPinned" class="pinned-badge">📍 Закреплен</span>
                      <span
                        v-if="
                          editingProject &&
                          displayMembers.some((m) => m.userId === user.id && !m.isRemoved)
                        "
                        class="already-member-badge"
                      >
                        ✓ Уже в проекте
                      </span>
                    </div>
                  </label>
                </div>

                <div v-if="filteredUsers.length === 0" class="no-users">
                  {{ searchQuery ? 'Контакты не найдены' : 'У вас пока нет контактов' }}
                </div>
              </div>

              <div class="users-count">
                {{ editingProject ? 'Новых участников:' : 'Участников:' }}
                {{ projectDto.assignedUsers.length }}
                <span v-if="searchQuery" class="search-hint">(фильтр: "{{ searchQuery }}")</span>
              </div>
            </div>

            <div v-if="selectedUsers.length > 0" class="selected-users-section">
              <label class="form-label">Выбранные участники:</label>
              <div class="selected-users">
                <div v-for="user in selectedUsers" :key="user.id" class="selected-user-tag">
                  <span class="selected-user-name">{{ user.name }}</span>
                  <button @click="removeUser(user.id)" type="button" class="remove-user-btn" :disabled="loading">
                    ×
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button
              type="submit"
              class="save-btn"
              :disabled="loading || !projectDto.name"
            >
              {{ loading ? 'Сохранение...' : (editingProject ? 'Сохранить изменения' : 'Создать проект') }}
            </button>
            <button
              type="button"
              @click="closeForm"
              class="cancel-btn"
              :disabled="loading"
            >
              Отмена
            </button>
          </div>
        </form>
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
  padding: 20px;
}

.settings-modal {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
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
  overflow-y: auto;
  padding: 24px;
}

.project-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.error-message {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #feb2b2;
  font-weight: 500;
  text-align: center;
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

.form-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  opacity: 0.7;
}

textarea.form-input {
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
}

.current-members-section {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e0e0e0;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  transition: all 0.3s ease;
}

.member-item.owner {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
  border-color: #f59e0b;
}

.member-item.current-user {
  border-left: 4px solid #667eea;
}

.member-item.removed {
  opacity: 0.5;
  background: #f5f5f5;
}

.member-item:hover:not(.removed) {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.member-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 50%;
  font-size: 18px;
  flex-shrink: 0;
}

.member-item.owner .member-avatar {
  background: rgba(245, 158, 11, 0.2);
}

.member-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.member-name {
  font-weight: 500;
  color: #2c3e50;
  font-size: 14px;
}

.member-role {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  align-self: flex-start;
}

.member-role.role_owner {
  background: #f59e0b;
  color: white;
}

.member-role.role_admin {
  background: #667eea;
  color: white;
}

.member-role.role_user {
  background: #48bb78;
  color: white;
}

.member-actions {
  display: flex;
  gap: 8px;
}

.remove-member-btn,
.restore-member-btn {
  background: #e74c3c;
  border: none;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: background 0.3s ease;
}

.remove-member-btn:hover:not(:disabled) {
  background: #c0392b;
}

.restore-member-btn {
  background: #2ecc71;
}

.restore-member-btn:hover:not(:disabled) {
  background: #27ae60;
}

.remove-member-btn:disabled,
.restore-member-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.members-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e0e0e0;
}

.total-members {
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
}

.selection-hint {
  color: #667eea;
  font-size: 12px;
  font-weight: 500;
}

.assignees-section {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.search-container {
  position: relative;
  padding: 12px;
  border-bottom: 1px solid #e0e0e0;
  background: #f8f9fa;
}

.search-input {
  padding-right: 40px;
}

.clear-search-btn {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  background: #a0aec0;
  border: none;
  color: white;
  font-size: 18px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-search-btn:hover:not(:disabled) {
  background: #718096;
}

.clear-search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-users,
.error-users {
  padding: 20px;
  text-align: center;
  font-weight: 500;
}

.loading-users {
  color: #667eea;
}

.error-users {
  color: #e74c3c;
}

.assignees-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 12px;
  background: white;
}

.assignee-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.assignee-item:last-child {
  border-bottom: none;
}

.assignee-item:hover {
  background: #f8f9fa;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 10px;
  padding: 4px;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  border: 2px solid #667eea;
  border-radius: 4px;
  cursor: pointer;
  flex-shrink: 0;
}

.checkbox-label input[type="checkbox"]:checked {
  background-color: #667eea;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 500;
  color: #2c3e50;
  font-size: 14px;
}

.pinned-badge,
.already-member-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
  align-self: flex-start;
}

.pinned-badge {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.already-member-badge {
  background: rgba(72, 187, 120, 0.1);
  color: #48bb78;
}

.assignee-item.already-member {
  opacity: 0.7;
}

.assignee-item.already-member .checkbox-label {
  cursor: not-allowed;
}

.no-users {
  text-align: center;
  padding: 20px;
  color: #a0aec0;
  font-style: italic;
}

.users-count {
  padding: 12px;
  background: #f8f9fa;
  font-size: 12px;
  color: #666;
  text-align: center;
  border-top: 1px solid #e0e0e0;
}

.search-hint {
  color: #999;
  font-style: italic;
}

.selected-users-section {
  margin-top: 8px;
}

.selected-users {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.selected-user-tag {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
}

.selected-user-name {
  margin-right: 6px;
}

.remove-user-btn {
  background: rgba(255, 255, 255, 0.3);
  border: none;
  color: white;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: background 0.3s ease;
}

.remove-user-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.5);
}

.remove-user-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
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
  flex: 1;
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

.cancel-btn {
  padding: 12px 24px;
  background: white;
  color: #666;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
}

.cancel-btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.cancel-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
    border-radius: 16px;
  }

  .settings-header {
    padding: 20px;
  }

  .settings-content {
    padding: 20px;
  }

  .form-actions {
    flex-direction: column;
  }

  .save-btn,
  .cancel-btn {
    width: 100%;
  }

  .members-list {
    max-height: 200px;
    overflow-y: auto;
  }

  .assignees-list {
    max-height: 150px;
  }
}

@media (max-width: 480px) {
  .settings-header {
    padding: 16px;
  }

  .settings-header h2 {
    font-size: 18px;
  }

  .settings-content {
    padding: 16px;
  }

  .member-item {
    flex-wrap: wrap;
  }

  .selected-users {
    justify-content: center;
  }
}
</style>
