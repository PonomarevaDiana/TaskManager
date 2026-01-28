<script>
import Canban from './Canban.vue'
import Calendar from './Calendar.vue'
import TaskForm from './Task-form.vue'
import List from './List.vue'
import SimpleFilter from './Simple-filter.vue'
import CreateProjectForm from './Create-project-form.vue'
import ProjectService from '@/services/Project-service.js'
import NotesList from '@/components/notes/Notes-list.vue'

export default {
  name: 'ProjectPage',
  components: { Canban, TaskForm, List, Calendar, SimpleFilter, CreateProjectForm, NotesList },
  emits: ['open-discussions'],
  props: {
    currentUser: Object,
    projectId: String,
    onBack: Function,
    refresh: Boolean,
  },
  data() {
    return {
      showForm: false,
      showProjectForm: false,
      refreshTaskList: false,
      viewMode: 'kanban',
      currentFilters: {},
      showFilterWarning: false,
      project: null,
      loading: true,
      error: null,
      showSettingsModal: false,
      autoDeleteDays: 30,
      savingSettings: false,
      showNotesList: false,
    }
  },
  async created() {
    await this.loadProjectData()
  },
  watch: {
    projectId: {
      handler: 'loadProjectData',
      immediate: false,
    },
    refresh(newVal) {
      if (newVal) {
        this.refreshTaskList = true
      }
    },
  },
  computed: {
    hasActiveFilters() {
      return Object.values(this.currentFilters).some((value) => value !== '')
    },
    isProjectOwner() {
      if (!this.project || !this.project.members) return false
      const owner = this.project.members.find((member) => member.role === 'ROLE_OWNER')
      return owner && owner.userId === this.currentUser?.id
    },
    membersCount() {
      return this.project?.members?.length || 0
    },
    formattedCreateDate() {
      if (!this.project?.createdAt) return ''
      return new Date(this.project.createdAt).toLocaleDateString('ru-RU')
    },
  },
  methods: {
    openDiscussions(taskId) {
      this.$emit('open-discussions', taskId)
    },
    setupAutoRefresh() {
      document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
          this.forceRefreshTasks()
        }
      })

      window.addEventListener('focus', () => {
        this.forceRefreshTasks()
      })
    },

    forceRefreshTasks() {
      this.refreshTaskList = true
      setTimeout(() => {
        this.refreshTaskList = false
      }, 100)
    },

    async loadProjectData() {
      if (!this.projectId) {
        this.error = 'ID проекта не указан'
        this.loading = false
        return
      }

      this.loading = true
      this.error = null
      try {
        const projectResponse = await ProjectService.getProject(this.projectId)

        const membersResponse = await ProjectService.getProjectMembers(this.projectId)

        this.project = {
          ...projectResponse.data,
          members: membersResponse.data || [],
        }

        console.log('Project data loaded:', this.project)
      } catch (error) {
        console.error('Error loading project:', error)
        this.error = this.getErrorMessage(error)
      } finally {
        this.loading = false
      }
    },

    getErrorMessage(error) {
      if (error.response?.status === 404) {
        return 'Проект не найден'
      } else if (error.response?.status === 403) {
        return 'У вас нет доступа к этому проекту'
      } else if (error.response?.data?.message) {
        return error.response.data.message
      } else {
        return 'Ошибка при загрузке проекта'
      }
    },

    openForm() {
      this.showForm = true
    },

    closeForm() {
      this.showForm = false
    },

    openProjectForm() {
      this.showProjectForm = true
    },

    closeProjectForm() {
      this.showProjectForm = false
    },

    showNotes() {
      this.showNotesList = true
    },

    closeNotes() {
      this.showNotesList = false
    },

    handleTaskCreated(newTask) {
      console.log('🎯 Задача создана в ProjectPage:', newTask)
      const taskProjectId = newTask.project?.id || newTask.projectId
      if (taskProjectId !== this.projectId) {
        console.log('⚠️ Задача не относится к текущему проекту:', {
          taskProjectId: taskProjectId,
          currentProjectId: this.projectId,
        })
        return
      }

      console.log('✅ Задача относится к текущему проекту, обновляем канбан')
      this.refreshTaskList = true
      this.closeForm()
    },

    async handleProjectUpdated(updatedProject) {
      console.log('Проект обновлен:', updatedProject)
      await this.loadProjectData()
      this.closeProjectForm()
    },

    setViewMode(mode) {
      this.viewMode = mode
    },

    applyFilter(filters) {
      this.currentFilters = filters
      this.showFilterWarning = false
    },

    clearFilters() {
      this.currentFilters = {}
      this.showFilterWarning = false
    },

    goBack() {
      if (this.onBack) {
        this.onBack()
      } else {
        console.warn('onBack function not provided')
      }
    },

    async refreshProject() {
      await this.loadProjectData()
    },

    getMemberRoleDisplayName(role) {
      const roleNames = {
        ROLE_OWNER: 'Владелец',
        ROLE_ADMIN: 'Администратор',
        ROLE_USER: 'Участник',
      }
      return roleNames[role] || role
    },

    handleDeleteTask(taskId) {
      this.$emit('delete-task', taskId)
    },

    async openSettingsModal() {
      this.showSettingsModal = true
      await this.loadAutoDeleteDays()
    },

    closeSettingsModal() {
      this.showSettingsModal = false
    },

    async loadAutoDeleteDays() {
      try {
        const response = await ProjectService.getProjectSettings(this.projectId)
        this.autoDeleteDays = response.data.autoDeleteDays
      } catch (error) {
        console.error('Ошибка загрузки настроек: ', error)
      }
    },

    async saveAutoDeleteSettings() {
      this.savingSettings = true
      try {
        await ProjectService.updateAutoDeleteDays(this.projectId, this.autoDeleteDays)
        alert('Настройки сохранены')
        this.closeSettingsModal()
      } catch (error) {
        console.error('Ошибка сохранения настроек: ', error)
        alert('Не удалось сохранить изменения')
      } finally {
        this.savingSettings = false
      }
    },

    async disableAutoDelete() {
      this.savingSettings = true
      try {
        await ProjectService.updateAutoDeleteDays(this.projectId, null)
        alert('Автоудаление отключено')
        this.autoDeleteDays = null
        this.closeSettingsModal()
      } catch (error) {
        console.error('Ошибка отключения автоудаления: ', error)
        alert('Не удалось отключить автоудаление')
      } finally {
        this.savingSettings = false
      }
    },
  },
  mounted() {
    this.setupAutoRefresh()
  },

  emits: ['refreshed', 'delete-task'],
}
</script>

<template>
  <div class="project-container">
    <!-- Загрузка -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p class="loading-text">Загрузка проекта...</p>
    </div>

    <!-- Ошибка -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">⚠️</div>
      <h3 class="error-title">Ошибка загрузки</h3>
      <p class="error-message">{{ error }}</p>
      <button @click="goBack" class="save-btn">Вернуться к проектам</button>
    </div>

    <!-- Контент проекта -->
    <div v-else-if="project" class="project-content">
      <!-- Шапка проекта -->
      <div class="project-header">
        <div class="project-header-content">
          <div class="project-header-left">
            <button @click="goBack" class="back-btn">
              <span class="back-icon">←</span>
              Назад
            </button>
          </div>

          <div class="project-header-center">
            <h1 class="project-title">{{ project.name }}</h1>
            <p class="project-description" v-if="project.description">{{ project.description }}</p>
            <div class="project-meta">
              <span class="meta-item">
                <span class="meta-icon">👥</span>
                {{ membersCount }}
              </span>
              <span class="meta-item">
                <span class="meta-icon">📅</span>
                {{ formattedCreateDate }}
              </span>
              <span v-if="isProjectOwner" class="meta-item owner-badge">Владелец</span>
            </div>
          </div>

          <div class="project-header-right">
            <div class="header-actions">
              <button @click="showNotes" class="notes-btn" title="Обсуждения проекта">
                <span class="notes-icon">📝</span>
              </button>
              <button @click="openForm" class="action-btn create-task-btn" title="Создать задачу">
                ＋
              </button>
              <button
                v-if="isProjectOwner"
                @click="openProjectForm"
                class="action-btn edit-project-btn"
                title="Редактировать проект"
              >
                ✏️
              </button>
              <button
                v-if="isProjectOwner"
                @click="openSettingsModal"
                class="action-btn settings-btn"
                title="Настройки проекта"
              >
                ⚙️
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Участники проекта -->
      <div v-if="project.members && project.members.length > 0" class="members-section">
        <div class="section-header">
          <h3 class="section-title">Участники проекта</h3>
        </div>
        <div class="members-list">
          <div
            v-for="member in project.members"
            :key="member.userId"
            class="member-item"
            :class="{ owner: member.role === 'ROLE_OWNER' }"
          >
            <div class="member-avatar">
              {{ member.role === 'ROLE_OWNER' ? '👑' : '👤' }}
            </div>
            <div class="member-info">
              <span class="member-name">{{ member.username }}</span>
              <span class="member-role">{{ getMemberRoleDisplayName(member.role) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Фильтр -->
      <SimpleFilter
        :current-filters="currentFilters"
        @filter="applyFilter"
        task-source="project"
        :source-id="projectId"
      />

      <!-- Управление видом -->
      <div class="view-controls">
        <div class="view-buttons">
          <button
            @click="setViewMode('kanban')"
            :class="['view-btn', { 'active': viewMode === 'kanban' }]"
          >
            <span class="view-icon">📊</span>
            Канбан
          </button>
          <button
            @click="setViewMode('list')"
            :class="['view-btn', { 'active': viewMode === 'list' }]"
          >
            <span class="view-icon">📋</span>
            Список
          </button>
          <button
            @click="setViewMode('calendar')"
            :class="['view-btn', { 'active': viewMode === 'calendar' }]"
          >
            <span class="view-icon">📅</span>
            Календарь
          </button>
        </div>
        <button @click="refreshProject" class="refresh-btn" title="Обновить данные">🔄</button>
      </div>

      <!-- Контент задач -->
      <div class="tasks-content">
        <div v-if="viewMode === 'list'" class="view-section">
          <List
            :refresh="refreshTaskList"
            @refreshed="refreshTaskList = false"
            :external-filters="currentFilters"
            task-source="project"
            :source-id="projectId"
            :project-members="project.members"
            @delete-task="handleDeleteTask"
            @open-discussions="openDiscussions"
            :current-user="currentUser"
          />
        </div>

        <div v-else-if="viewMode === 'kanban'" class="view-section">
          <Canban
            :refresh="refreshTaskList"
            @refreshed="refreshTaskList = false"
            :external-filters="currentFilters"
            task-source="project"
            :source-id="projectId"
            :project-members="project.members"
            @delete-task="handleDeleteTask"
            @open-discussions="openDiscussions"
            :current-user="currentUser"
          />
        </div>

        <div v-else-if="viewMode === 'calendar'" class="view-section">
          <Calendar
            :refresh="refreshTaskList"
            @refreshed="refreshTaskList = false"
            :external-filters="currentFilters"
            task-source="project"
            :source-id="projectId"
            :project-members="project.members"
            @delete-task="handleDeleteTask"
            @open-discussions="openDiscussions"
            :current-user="currentUser"
          />
        </div>
      </div>
    </div>

    <!-- Модальные окна -->
    <TaskForm
      :is-visible="showForm"
      :currentUserId="currentUser?.id"
      @task-created="handleTaskCreated"
      @close="closeForm"
      task-source="project"
      :source-id="projectId"
      :project-members="project?.members || []"
    />

    <CreateProjectForm
      :isVisible="showProjectForm"
      :editingProject="project"
      :currentUser="currentUser"
      @project-updated="handleProjectUpdated"
      @close="closeProjectForm"
    />

    <NotesList
      v-if="showNotesList"
      :objectId="projectId"
      :isProject="true"
      :userId="currentUser.id"
      @close="closeNotes"
      @showPushMessage="$emit('showPushMessage', $event)"
    />

    <!-- Модальное окно настроек -->
    <div v-if="showSettingsModal" class="settings-overlay" @click.self="closeSettingsModal">
      <div class="settings-modal">
        <div class="settings-header">
          <h2>Параметры проекта</h2>
          <button class="close-btn" @click="closeSettingsModal" :disabled="savingSettings">×</button>
        </div>

        <div class="settings-content">
          <div class="settings-section">
            <h3 class="section-title">Автоудаление задач</h3>

            <div class="form-group">
              <label class="form-label">Удалять завершённые задачи через (дней):</label>
              <div class="input-with-btn">
                <input
                  v-model.number="autoDeleteDays"
                  type="number"
                  min="1"
                  max="365"
                  class="form-input"
                  placeholder="Количество дней"
                  :disabled="savingSettings"
                />
                <button
                  @click="disableAutoDelete"
                  class="cancel-btn"
                  :disabled="savingSettings"
                  style="min-width: 180px;"
                >
                  Отключить автоудаление
                </button>
              </div>
              <p v-if="autoDeleteDays != null" class="form-hint">
                Завершённые задачи будут автоматически удаляться через
                <strong>{{ autoDeleteDays }}</strong> дней после завершения.
              </p>
              <p v-else class="form-hint">
                Автоудаление завершённых задач <strong>отключено</strong>
              </p>
            </div>
          </div>

          <div class="form-actions">
            <button
              @click="saveAutoDeleteSettings"
              class="save-btn"
              :disabled="savingSettings || !autoDeleteDays"
            >
              {{ savingSettings ? 'Сохранение...' : 'Сохранить изменения' }}
            </button>
            <button
              @click="closeSettingsModal"
              class="cancel-btn"
              :disabled="savingSettings"
            >
              Отмена
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.project-container {
  min-height: 100vh;
  padding: 0;
}

/* Загрузка */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.loading-spinner {
  border: 4px solid #e0e0e0;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

.loading-text {
  color: #2c3e50;
  font-size: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Ошибка */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  padding: 40px 20px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.error-title {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
}

.error-message {
  margin: 0 0 24px 0;
  color: #666;
  max-width: 400px;
  line-height: 1.5;
}

/* Шапка проекта - УМЕНЬШЕНА */
.project-header {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  padding: 16px 24px;
  border-radius: 0 0 16px 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.project-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.project-header-left {
  flex-shrink: 0;
}

.project-header-center {
  flex: 1;
  text-align: center;
  min-width: 0;
}

.project-header-right {
  flex-shrink: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.3s ease;
  white-space: nowrap;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.back-icon {
  font-size: 16px;
}

.project-title {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.project-description {
  margin: 0 0 8px 0;
  font-size: 13px;
  opacity: 0.9;
  line-height: 1.4;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-meta {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.owner-badge {
  background: rgba(255, 255, 255, 0.3);
  font-weight: 600;
}

/* Кнопки в шапке - компактнее */
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.notes-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s ease;
  flex-shrink: 0;
}

.notes-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.create-task-btn {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

.create-task-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.edit-project-btn,
.settings-btn {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

.edit-project-btn:hover,
.settings-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* Секция участников */
.members-section {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 24px;
}

.section-header {
  margin-bottom: 12px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.members-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  transition: all 0.3s ease;
  min-width: 180px;
}

.member-item.owner {
  border-color: #f59e0b;
  background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
}

.member-item:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.member-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 50%;
  font-size: 14px;
  flex-shrink: 0;
}

.member-item.owner .member-avatar {
  background: rgba(245, 158, 11, 0.2);
}

.member-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.member-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-role {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  background: #f8f9fa;
  color: #666;
  font-weight: 500;
  align-self: flex-start;
}

/* Управление видом */
.view-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px auto;
  padding: 0 24px;
}

.view-buttons {
  display: flex;
  gap: 6px;
  background: white;
  padding: 6px;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
}

.view-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.view-btn:hover {
  background: #f8f9fa;
}

.view-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
}

.view-icon {
  font-size: 14px;
}

.refresh-btn {
  background: white;
  border: 1px solid #e0e0e0;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.refresh-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

/* Контент задач */
.tasks-content {
  margin: 0 auto 30px;
  padding: 0 24px;
}

.view-section {
  background: white;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
  overflow: hidden;
}

/* Модальное окно настроек (в стиле шаблона) */
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
  border-radius: 16px;
  width: 90%;
  max-width: 480px;
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
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border-radius: 16px 16px 0 0;
  flex-shrink: 0;
}

.settings-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 22px;
  width: 30px;
  height: 30px;
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
  padding: 20px;
}

.settings-section {
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-label {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.input-with-btn {
  display: flex;
  gap: 10px;
  align-items: center;
}

.form-input {
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.3s ease;
  flex: 1;
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

.form-hint {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

.form-hint strong {
  color: #2c3e50;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e0e0e0;
}

.save-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.cancel-btn {
  padding: 10px 20px;
  background: white;
  color: #666;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
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
  .project-header {
    padding: 12px 16px;
  }

  .project-header-content {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }

  .project-header-left {
    align-self: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: center;
  }

  .project-title {
    font-size: 18px;
  }

  .project-description {
    font-size: 12px;
    -webkit-line-clamp: 1;
  }

  .meta-item {
    font-size: 11px;
    padding: 3px 6px;
  }

  .members-section,
  .view-controls,
  .tasks-content {
    padding: 0 16px;
  }

  .view-controls {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .view-buttons {
    width: 100%;
    justify-content: center;
  }

  .members-list {
    justify-content: center;
  }

  .input-with-btn {
    flex-direction: column;
  }

  .cancel-btn {
    min-width: 100%;
  }
}

@media (max-width: 480px) {
  .project-title {
    font-size: 16px;
  }

  .action-btn {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }

  .notes-btn {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }

  .back-btn {
    padding: 6px 10px;
    font-size: 12px;
  }

  .member-item {
    min-width: 160px;
    padding: 8px 12px;
  }

  .view-btn {
    padding: 6px 10px;
    font-size: 12px;
  }

  .refresh-btn {
    width: 32px;
    height: 32px;
  }
}
</style>
