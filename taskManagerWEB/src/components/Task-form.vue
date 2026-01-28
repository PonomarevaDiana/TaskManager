<script>
import UserService from '@/services/User-service.js'
import TaskService from '@/services/Task-service.js'
import ProjectService from '@/services/Project-service.js'

export default {
  name: 'TaskForm',
  inject: ['getCurrentUser'],
  emits: ['task-created', 'task-updated', 'close'],
  props: {
    isVisible: {
      type: Boolean,
      default: false,
    },
    task: {
      type: Object,
      default: null,
    },
    users: {
      type: Array,
      default: () => [],
    },
    priorities: {
      type: Array,
      default: () => [
        { id: 1, name: 'высокий' },
        { id: 2, name: 'средний' },
        { id: 3, name: 'низкий' },
      ],
    },
    statuses: {
      type: Array,
      default: () => [
        { id: 1, name: 'активная' },
        { id: 2, name: 'завершена' },
        { id: 3, name: 'в работе' },
        { id: 4, name: 'на проверке' },
      ],
    },
    taskSource: {
      type: String,
      default: 'personal',
      validator: (value) => ['personal', 'project', 'work'].includes(value),
    },
    sourceId: {
      type: String,
      default: null,
    },
    projectMembers: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      formData: {
        title: '',
        assignees: [],
        startDate: '',
        deadlineDate: '',
        priorityId: null,
        statusId: null,
      },
      loading: false,
      error: null,
      localUsers: [],
      usersLoading: false,
      usersError: null,
      searchUserQuery: '',
    }
  },
  computed: {
    isEditMode() {
      return !!this.task
    },
    currentUser() {
      const user = this.getCurrentUser ? this.getCurrentUser() : {}
      return user.id ? user : { id: 'user1', username: 'system' }
    },
    isCreator() {
      return this.task && this.task.creator && this.task.creator.id === this.currentUser.id
    },
    isExecutor() {
      if (!this.task || !this.task.assignees) return false
      const currentUserId = this.currentUser.id
      return this.task.assignees.some((assignee) => {
        const assigneeId = typeof assignee === 'object' ? assignee.id : assignee
        return assigneeId === currentUserId
      })
    },
    canEditTask() {
      if (!this.isEditMode) return 'full'
      if (this.isCreator) return 'full'
      if (this.isExecutor) return 'status-only'
      return 'none'
    },
    minStartDate() {
      return !this.isEditMode ? new Date().toISOString().split('T')[0] : null
    },
    minDeadlineDate() {
      return (
        this.formData.startDate ||
        (!this.isEditMode ? new Date().toISOString().split('T')[0] : null)
      )
    },
    displayUsers() {
      if (this.taskSource === 'project') {
        if (this.projectMembers && this.projectMembers.length > 0) {
          return this.projectMembers.map((member) => ({
            id: member.userId || member.id,
            username: member.username,
            name: member.username,
            role: member.role,
          }))
        }
        return []
      }

      if (this.taskSource === 'work') {
        return this.localUsers
      }

      return this.localUsers
    },
    filteredUsers() {
      if (!this.searchUserQuery) {
        return this.displayUsers
      }
      const query = this.searchUserQuery.toLowerCase()
      return this.displayUsers.filter((user) => {
        const name = (user.name || user.username || '').toLowerCase()
        const id = user.id.toString()
        return name.includes(query) || id.includes(query)
      })
    },
    usersWithRoles() {
      return this.displayUsers.map((user) => ({
        ...user,
        displayName:
          this.taskSource === 'project'
            ? `${user.username} (${this.getRoleDisplayName(user.role)})`
            : user.username,
      }))
    },
  },
  watch: {
    isVisible: {
      handler(newVal) {
        if (newVal) {
          if (this.taskSource !== 'project' || this.projectMembers.length === 0) {
            this.loadUsers()
          }
          this.initializeForm()
        }
      },
      immediate: true,
    },
    task: {
      handler(newTask) {
        if (newTask) {
          this.initializeForm()
        }
      },
      immediate: true,
    },
  },
  methods: {
    initializeForm() {
      if (this.isEditMode) {
        let assigneeIds = []

        if (this.task.assignees && Array.isArray(this.task.assignees)) {
          assigneeIds = this.extractAssigneeIds(this.task.assignees)
        } else if (this.task.executors && Array.isArray(this.task.executors)) {
          assigneeIds = this.extractAssigneeIds(this.task.executors)
        } else if (this.task.responsible && Array.isArray(this.task.responsible)) {
          assigneeIds = this.extractAssigneeIds(this.task.responsible)
        }

        this.formData = {
          title: this.task.title || '',
          assignees: assigneeIds,
          startDate: this.task.startDate ? this.formatDateForInput(this.task.startDate) : '',
          deadlineDate: this.task.deadlineDate
            ? this.formatDateForInput(this.task.deadlineDate)
            : '',
          priorityId: this.task.priority?.id || null,
          statusId: this.task.status?.id || null,
        }

        if (this.taskSource === 'personal' && this.task.creator?.id) {
          this.formData.assignees = [this.task.creator.id]
        }
      } else {
        this.formData = {
          title: '',
          assignees: [],
          startDate: '',
          deadlineDate: '',
          priorityId: null,
          statusId: null,
        }
        this.autoAssignUser()
      }
    },

    extractAssigneeIds(assignees) {
      if (!Array.isArray(assignees)) return []

      return assignees
        .map((assignee) => {
          if (typeof assignee === 'object' && assignee.id) {
            return assignee.id
          } else if (typeof assignee === 'string') {
            return assignee
          } else if (typeof assignee === 'number') {
            return assignee.toString()
          }
          return null
        })
        .filter((id) => id !== null)
    },

    async submitTask() {
      this.loading = true
      this.error = null

      try {
        if (this.isEditMode) {
          await this.updateTask()
        } else {
          await this.createTask()
        }
      } catch (error) {
        console.error('Ошибка:', error)
        this.error = error.response?.data?.message || error.message || 'Произошла ошибка'
      } finally {
        this.loading = false
      }
    },

    async createTask() {
      if (
        this.formData.startDate &&
        new Date(this.formData.startDate) < new Date().setHours(0, 0, 0, 0)
      ) {
        this.error = 'Дата начала не может быть в прошлом'
        this.loading = false
        return
      }

      if (
        this.formData.deadlineDate &&
        this.formData.startDate &&
        new Date(this.formData.deadlineDate) < new Date(this.formData.startDate)
      ) {
        this.error = 'Дедлайн не может быть раньше даты начала'
        this.loading = false
        return
      }

      const taskData = {
        title: this.formData.title,
        startDate: this.formData.startDate || null,
        deadlineDate: this.formData.deadlineDate || null,
        createDate: new Date().toISOString().split('T')[0],
      }

      if (this.taskSource === 'project' && this.sourceId) {
        taskData.project = { id: this.sourceId }
      }

      if (this.formData.priorityId) {
        taskData.priority = { id: this.formData.priorityId }
      }

      if (this.formData.statusId) {
        taskData.status = { id: this.formData.statusId }
      }

      if (this.formData.assignees && this.formData.assignees.length > 0) {
        taskData.assignees = this.formData.assignees.map((id) => ({ id: id }))
      } else {
        if (this.currentUser && this.currentUser.id) {
          taskData.assignees = [{ id: this.currentUser.id }]
        }
      }

      if (this.currentUser && this.currentUser.id) {
        taskData.creator = { id: this.currentUser.id }
      }

      Object.keys(taskData).forEach((key) => {
        if (
          taskData[key] === null ||
          taskData[key] === '' ||
          (Array.isArray(taskData[key]) && taskData[key].length === 0)
        ) {
          delete taskData[key]
        }
      })

      const response = await TaskService.createTask(taskData)
      const createdTask = response.data

      this.resetForm()
      this.$emit('task-created', createdTask)
      this.$emit('close')
    },

    async updateTask() {
      this.loading = true
      this.error = null

      try {
        let taskData = {}

        if (this.canEditTask === 'full') {
          taskData = {
            title: this.formData.title || this.task.title,
            startDate: this.formData.startDate || this.task.startDate || null,
            deadlineDate: this.formData.deadlineDate || this.task.deadlineDate || null,
          }

          if (this.taskSource === 'project' && this.sourceId) {
            taskData.project = { id: this.sourceId }
          } else if (this.task.project?.id) {
            taskData.project = { id: this.task.project.id }
          }

          if (this.formData.priorityId) {
            taskData.priority = { id: this.formData.priorityId }
          } else if (this.task.priority?.id) {
            taskData.priority = { id: this.task.priority.id }
          }

          if (this.formData.statusId) {
            taskData.status = { id: this.formData.statusId }
          } else if (this.task.status?.id) {
            taskData.status = { id: this.task.status.id }
          }

          if (this.formData.assignees && this.formData.assignees.length > 0) {
            taskData.assignees = this.formData.assignees.map((id) => ({ id: id }))
          } else {
            taskData.assignees = this.task.assignees || []
          }

          if (this.task.creator?.id) {
            taskData.creator = { id: this.task.creator.id }
          }

          if (this.task.createDate) {
            taskData.createDate = this.task.createDate
          }
        } else if (this.canEditTask === 'status-only') {
          taskData = {
            title: this.task.title,
            startDate: this.task.startDate,
            deadlineDate: this.task.deadlineDate,
          }

          if (this.task.project?.id) {
            taskData.project = { id: this.task.project.id }
          }

          if (this.task.priority?.id) {
            taskData.priority = { id: this.task.priority.id }
          }

          if (this.formData.statusId) {
            taskData.status = { id: this.formData.statusId }
          } else if (this.task.status?.id) {
            taskData.status = { id: this.task.status.id }
          }

          if (this.task.assignees) {
            taskData.assignees = this.task.assignees
          }

          if (this.task.creator?.id) {
            taskData.creator = { id: this.task.creator.id }
          }

          if (this.task.createDate) {
            taskData.createDate = this.task.createDate
          }
        }

        Object.keys(taskData).forEach((key) => {
          if (
            taskData[key] === null ||
            taskData[key] === '' ||
            (Array.isArray(taskData[key]) && taskData[key].length === 0)
          ) {
            delete taskData[key]
          }
        })

        console.log('🔄 Sending task update:', taskData)
        console.log('🔐 Access level:', this.canEditTask)

        const response = await TaskService.updateTask(this.task.id, taskData)

        if (response.status === 200 || response.status === 204) {
          const updatedTask = response.data || {
            ...this.task,
            ...taskData,
          }
          this.$emit('task-updated', updatedTask)
          this.closeForm()
        } else {
          throw new Error('Ошибка при обновлении задачи')
        }
      } catch (error) {
        console.error('Ошибка:', error)
        this.error = error.response?.data?.message || error.message || 'Произошла ошибка'
      } finally {
        this.loading = false
      }
    },

    generateTempFriendshipKey() {
      return Math.random().toString(36).substring(2, 10)
    },

    getRoleDisplayName(role) {
      const roleNames = {
        ROLE_OWNER: 'Владелец',
        ROLE_ADMIN: 'Админ',
        ROLE_USER: 'Участник',
      }
      return roleNames[role] || role
    },

    autoAssignUser() {
      if (this.taskSource === 'personal' && this.sourceId && this.localUsers.length > 0) {
        const sourceUser = this.localUsers.find((user) => user.id === this.sourceId)
        if (sourceUser && !this.formData.assignees.includes(this.sourceId)) {
          this.formData.assignees.push(this.sourceId)
        }
      }

      if (this.taskSource === 'project') {
        this.formData.assignees = []
      }
    },

    async loadUsers() {
      if (this.taskSource === 'work' && this.task && this.task.project) {
        try {
          const projectId = this.task.project.id
          console.log('🔄 Загружаем участников проекта через ProjectService, ID:', projectId)

          const response = await ProjectService.getProjectMembers(projectId)
          this.localUsers = response.data.map((member) => ({
            id: member.userId || member.id,
            username: member.username,
            name: member.username,
            role: member.role || 'ROLE_USER',
          }))

          console.log(
            '✅ Участники проекта загружены через ProjectService:',
            this.localUsers.length,
          )
        } catch (error) {
          console.error('❌ Ошибка загрузки участников проекта через ProjectService:', error)
          this.localUsers = []
        }
      } else if (this.taskSource === 'project' && this.sourceId) {
        try {
          const response = await ProjectService.getProjectMembers(this.sourceId)
          this.localUsers = response.data.map((member) => ({
            id: member.userId || member.id,
            username: member.username,
            name: member.username,
            role: member.role || 'ROLE_USER',
          }))
        } catch (error) {
          console.error('❌ Ошибка загрузки участников проекта:', error)
          this.localUsers = []
        }
      } else {
        try {
          const response = await UserService.getAllUsers()
          this.localUsers = response.data.map((user) => ({
            id: user.id,
            username: user.username,
            name: user.username,
            role: 'ROLE_USER',
          }))
        } catch (error) {
          console.error('Ошибка загрузки пользователей:', error)
          this.localUsers = []
        }
      }
    },

    handleAssigneeChange(userId) {
      if (this.taskSource === 'personal' && userId === this.sourceId) {
        return
      }

      const index = this.formData.assignees.indexOf(userId)
      if (index > -1) {
        this.formData.assignees.splice(index, 1)
      } else {
        this.formData.assignees.push(userId)
      }
    },

    isAssignee(userId) {
      if (this.taskSource === 'personal' && this.isEditMode && this.task.creator?.id === userId) {
        return true
      }
      const isAssigned = this.formData.assignees.includes(userId)
      return isAssigned
    },

    getUserName(userId) {
      const user = this.displayUsers.find((u) => u.id === userId)
      return user ? user.name || user.username : `ID: ${userId}`
    },

    formatDateForInput(dateString) {
      if (!dateString) return ''
      try {
        const date = new Date(dateString)
        return date.toISOString().split('T')[0]
      } catch (e) {
        console.log(e)
        return dateString
      }
    },

    formatDate(dateString) {
      if (!dateString) return ''
      try {
        const date = new Date(dateString)
        return date.toLocaleDateString('ru-RU')
      } catch (e) {
        console.log(e)
        return dateString
      }
    },

    resetForm() {
      this.formData = {
        title: '',
        assignees: [],
        startDate: '',
        deadlineDate: '',
        priorityId: null,
        statusId: null,
      }
      this.searchUserQuery = ''
      this.error = null
    },

    closeForm() {
      this.localUsers = []
      this.resetForm()
      this.$emit('close')
    },

    clearSearch() {
      this.searchUserQuery = ''
    },

    getProjectName(project) {
      if (!project) return 'Без проекта'
      if (typeof project === 'string') return project
      return project.name || 'Без названия'
    },

    getProjectDescription(project) {
      if (!project) return ''
      if (typeof project === 'string') return ''
      return project.description || ''
    },

    getProjectOwner(project) {
      if (!project) return null
      if (typeof project === 'string') return null
      return project.owner || project.creator || null
    },

    hasProject(task) {
      return task && (task.project || task.projectId || task.project_id)
    },

    getPersonName(person) {
      if (!person) return 'Не указан'
      if (typeof person === 'string') return person
      return person.name || person.username || 'Не указан'
    },
  },
}
</script>

<template>
  <div v-if="isVisible" class="modal-overlay" @click.self="closeForm">
    <div class="modal-content">
      <div class="modal-header">
        <h2>{{ isEditMode ? 'Редактировать задачу' : 'Создать новую задачу' }}</h2>
        <button class="close-btn" @click="closeForm" :disabled="loading">×</button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="submitTask" class="task-form">
          <div v-if="error" class="error-message">
            {{ error }}
          </div>

          <div v-if="canEditTask === 'full'" class="settings-section">
            <div class="form-group">
              <label for="title" class="form-label">Название задачи:</label>
              <input
                type="text"
                id="title"
                v-model="formData.title"
                required
                class="form-input"
                :placeholder="isEditMode ? 'Введите название задачи' : 'Введите название задачи'"
                :disabled="loading"
              />
            </div>

            <div class="form-group">
              <label class="form-label">Исполнители:</label>

              <div
                v-if="!isEditMode && taskSource === 'personal' && sourceId"
                class="auto-assigned-info"
              >
                {{ getUserName(sourceId) }}
              </div>

              <div v-else-if="isEditMode && taskSource === 'personal'" class="auto-assigned-info">
                {{ task.creator?.name || task.creator?.username || 'Не указан' }}
                <span class="creator-badge">(создатель)</span>
              </div>

              <div v-else class="assignees-section">
                <div v-if="taskSource === 'project'" class="info-badge">
                  👥 Участники проекта
                </div>

                <div v-if="usersLoading" class="loading-users">Загрузка пользователей...</div>
                <div v-else-if="usersError" class="error-users">
                  {{ usersError }}
                </div>
                <div v-else class="assignees-container">
                  <div class="search-container">
                    <input
                      type="text"
                      v-model="searchUserQuery"
                      :placeholder="
                        taskSource === 'project'
                          ? 'Поиск среди участников проекта...'
                          : 'Поиск пользователей по имени или ID...'
                      "
                      class="form-input search-input"
                      :disabled="loading"
                    />
                    <button
                      v-if="searchUserQuery"
                      type="button"
                      @click="clearSearch"
                      class="clear-search-btn"
                      :disabled="loading"
                    >
                      ×
                    </button>
                  </div>

                  <div class="assignees-list">
                    <div v-for="user in usersWithRoles" :key="user.id" class="assignee-item">
                      <label class="checkbox-label">
                        <input
                          type="checkbox"
                          :value="user.id"
                          :checked="isAssignee(user.id)"
                          @change="handleAssigneeChange(user.id)"
                          :disabled="loading"
                        />
                        <span class="checkmark"></span>
                        {{ user.displayName || user.username }}
                      </label>
                    </div>
                  </div>

                  <div v-if="filteredUsers.length > 0" class="users-count">
                    Найдено: {{ filteredUsers.length }} из {{ displayUsers.length }}
                    <span v-if="searchUserQuery" class="search-hint"
                      >(по запросу: "{{ searchUserQuery }}")</span
                    >
                  </div>
                </div>
              </div>
            </div>

            <div class="form-grid">
              <div class="form-group">
                <label for="startDate" class="form-label">Дата начала:</label>
                <input
                  type="date"
                  id="startDate"
                  v-model="formData.startDate"
                  :min="minStartDate"
                  class="form-input"
                  :disabled="loading"
                />
              </div>

              <div class="form-group">
                <label for="deadlineDate" class="form-label">Дедлайн:</label>
                <input
                  type="date"
                  id="deadlineDate"
                  v-model="formData.deadlineDate"
                  :min="minDeadlineDate"
                  class="form-input"
                  :disabled="loading"
                />
              </div>
            </div>

            <div class="form-group">
              <label for="priority" class="form-label">Приоритет:</label>
              <select
                id="priority"
                v-model="formData.priorityId"
                class="form-input"
                :disabled="loading"
              >
                <option value="">Выберите приоритет</option>
                <option v-for="priority in priorities" :key="priority.id" :value="priority.id">
                  {{ priority.name }}
                </option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label for="status" class="form-label">Статус:</label>
            <select
              id="status"
              v-model="formData.statusId"
              class="form-input"
              :disabled="loading || canEditTask === 'none'"
            >
              <option value="">Выберите статус</option>
              <option v-for="status in statuses" :key="status.id" :value="status.id">
                {{ status.name }}
              </option>
            </select>
          </div>

          <div v-if="isEditMode" class="task-info-section">
            <h3 class="section-title">Информация о задаче</h3>

            <div class="info-grid">
              <div class="info-item">
                <div class="info-label">
                  <span class="info-icon">👤</span>
                  Создатель
                </div>
                <div class="info-value">
                  {{ task.creator?.name || task.creator?.username || 'Не указан' }}
                </div>
              </div>

              <div class="info-item">
                <div class="info-label">
                  <span class="info-icon">📅</span>
                  Дата создания
                </div>
                <div class="info-value">
                  {{ formatDate(task.createDate) }}
                </div>
              </div>

              <div class="info-item" v-if="taskSource === 'personal'">
                <div class="info-label">
                  <span class="info-icon">🎯</span>
                  Тип задачи
                </div>
                <div class="info-value">
                  <span class="type-badge">Личная</span>
                </div>
              </div>

              <div class="info-item" v-if="taskSource === 'work' && task.project">
                <div class="info-label">
                  <span class="info-icon">🏢</span>
                  Проект
                </div>
                <div class="info-value">
                  <span class="type-badge">{{ getProjectName(task.project) }}</span>
                </div>
              </div>
            </div>

            <div v-if="taskSource === 'work' && task.project" class="project-info">
              <h4 class="project-title">Информация о проекте</h4>
              <div class="project-details">
                <div class="project-name">{{ getProjectName(task.project) }}</div>
                <div v-if="getProjectDescription(task.project)" class="project-description">
                  "{{ getProjectDescription(task.project) }}"
                </div>
                <div v-if="getProjectOwner(task.project)" class="project-owner">
                  Владелец: {{ getPersonName(getProjectOwner(task.project)) }}
                </div>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button
              v-if="canEditTask !== 'none' || !isEditMode"
              type="submit"
              class="save-btn"
              :disabled="loading || !formData.title || !formData.statusId"
            >
              {{ loading ? (isEditMode ? 'Сохранение...' : 'Создание...') : (isEditMode ? 'Сохранить изменения' : 'Создать задачу') }}
            </button>

            <button
              type="button"
              @click="closeForm"
              class="cancel-btn"
              :disabled="loading"
            >
              {{ canEditTask === 'none' && isEditMode ? 'Закрыть' : 'Отмена' }}
            </button>
          </div>
        </form>
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
  z-index: 1001;
  animation: fadeIn 0.3s ease;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
  overflow: hidden;
}

.modal-header {
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

.modal-header h2 {
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

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.task-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.auto-assigned-info {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  color: #2c3e50;
}

.creator-badge {
  color: #667eea;
  font-size: 12px;
  margin-left: 8px;
}

.info-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
  margin-bottom: 12px;
}

.assignees-section {
  margin-top: 8px;
}

.assignees-container {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.search-container {
  position: relative;
  padding: 12px;
  border-bottom: 1px solid #e0e0e0;
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

.assignees-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 12px;
}

.assignee-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.assignee-item:last-child {
  border-bottom: none;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 10px;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  border: 2px solid #667eea;
  border-radius: 4px;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"]:checked {
  background-color: #667eea;
}

.users-count {
  padding: 12px;
  background: #f8f9fa;
  font-size: 12px;
  color: #666;
  text-align: center;
}

.search-hint {
  color: #999;
  font-style: italic;
}

.loading-users,
.error-users {
  text-align: center;
  padding: 16px;
  border-radius: 8px;
  margin: 12px 0;
}

.loading-users {
  background: #e3f2fd;
  color: #1976d2;
}

.error-users {
  background: #ffebee;
  color: #d32f2f;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
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

.task-info-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.info-icon {
  font-size: 14px;
}

.info-value {
  font-weight: 500;
  color: #2c3e50;
}

.type-badge {
  background: #667eea;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  display: inline-block;
}

.project-info {
  background: white;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.project-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.project-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.project-name {
  font-weight: 600;
  color: #2c3e50;
}

.project-description {
  font-size: 13px;
  color: #666;
  font-style: italic;
}

.project-owner {
  font-size: 12px;
  color: #888;
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
  .modal-content {
    width: 95%;
    border-radius: 16px;
  }

  .modal-header {
    padding: 20px;
  }

  .modal-body {
    padding: 20px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .form-actions {
    flex-direction: column;
  }

  .save-btn,
  .cancel-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .modal-header {
    padding: 16px;
  }

  .modal-header h2 {
    font-size: 18px;
  }

  .modal-body {
    padding: 16px;
  }

  .task-form {
    gap: 16px;
  }
}
</style>
