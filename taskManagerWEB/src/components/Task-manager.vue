<script>
import HeaderMenu from './HeaderMenu.vue'
import MySpace from './MySpace.vue'
import Projects from './Projects.vue'
import ProjectPage from './Project-page.vue'
import TaskService from '@/services/Task-service.js'
import NotesList from './notes/Notes-list.vue'

export default {
  components: { HeaderMenu, MySpace, Projects, ProjectPage, NotesList },
  props: {
    globalData: Object,
    isAuthenticated: Boolean,
    currentUser: Object,
  },
  data() {
    return {
      showForm: false,
      refreshTaskList: false,
      activeTab: 'my-space',
      projectsView: 'list',
      selectedProjectId: null,
      showDeleteModal: false,
      taskToDelete: null,
      deleteLoading: false,
      showNotesList: false,
      objectId: ''
    }
  },
  methods: {
    handleTabChanged(tab) {
      this.activeTab = tab
      if (tab === 'projects') {
        this.projectsView = 'list'
        this.selectedProjectId = null
      }
    },

    openProject(projectId) {
      this.selectedProjectId = projectId
      this.projectsView = 'project'
    },

    showProjectList() {
      this.projectsView = 'list'
      this.selectedProjectId = null
    },

    openDeleteModal(taskId) {
      this.taskToDelete = taskId
      this.showDeleteModal = true
    },

    closeDeleteModal() {
      this.showDeleteModal = false
      this.taskToDelete = null
      this.deleteLoading = false
    },

    async confirmDelete() {
      if (!this.taskToDelete) return

      this.deleteLoading = true
      try {
        const response = await TaskService.deleteTask(this.taskToDelete)

        if (response.status === 204 || response.status === 200) {
          this.showSuccessNotification('Задача успешно удалена!')
          this.refreshTaskList = true
          this.closeDeleteModal()
        } else {
          throw new Error('Ошибка при удалении задачи')
        }
      } catch (error) {
        console.error('Ошибка при удалении задачи:', error)
        this.showErrorNotification('Произошла ошибка при удалении задачи.')
        this.deleteLoading = false
      }
    },

    showSuccessNotification(message) {
      const notification = document.createElement('div')
      notification.className = 'success-notification'
      notification.innerHTML = `
        <div class="notification-content">
          <span class="notification-icon">✅</span>
          <span class="notification-text">${message}</span>
        </div>
      `
      notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: linear-gradient(135deg, #10b981, #059669);
        color: white;
        padding: 16px 20px;
        border-radius: 12px;
        box-shadow: 0 10px 25px rgba(16, 185, 129, 0.3);
        z-index: 10000;
        animation: slideInRight 0.3s ease-out;
        max-width: 300px;
      `

      document.body.appendChild(notification)

      setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.3s ease-in'
        setTimeout(() => {
          if (notification.parentNode) {
            notification.parentNode.removeChild(notification)
          }
        }, 300)
      }, 3000)
    },

    showErrorNotification(message) {
      const notification = document.createElement('div')
      notification.className = 'error-notification'
      notification.innerHTML = `
        <div class="notification-content">
          <span class="notification-icon">❌</span>
          <span class="notification-text">${message}</span>
        </div>
      `
      notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: linear-gradient(135deg, #ef4444, #dc2626);
        color: white;
        padding: 16px 20px;
        border-radius: 12px;
        box-shadow: 0 10px 25px rgba(239, 68, 68, 0.3);
        z-index: 10000;
        animation: slideInRight 0.3s ease-out;
        max-width: 300px;
      `

      document.body.appendChild(notification)

      setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.3s ease-in'
        setTimeout(() => {
          if (notification.parentNode) {
            notification.parentNode.removeChild(notification)
          }
        }, 300)
      }, 4000)
    },
    openDiscussions(taskId) {
      console.log(`Отврытие обсуждений задачи с id: ${taskId}`)
      this.objectId = taskId
      this.showNotesList = true
    },
    closeDiscussions() {
      this.showNotesList = false
      this.objectId = ''
    }
  },
}
</script>

<template>
  <div class="container">
    <HeaderMenu
      :is-authenticated="isAuthenticated"
      :current-user="currentUser"
      @tab-changed="handleTabChanged"
    />

    <div v-if="showDeleteModal" class="delete-modal-overlay" @click.self="closeDeleteModal">
      <div class="delete-modal">
        <div class="delete-modal-header">
          <div class="delete-icon">🗑️</div>
          <h3>Удаление задачи</h3>
        </div>

        <div class="delete-modal-body">
          <div class="delete-warning">Вы уверены, что хотите удалить эту задачу?</div>
        </div>

        <div class="delete-modal-actions">
          <button @click="closeDeleteModal" class="btn-delete-cancel" :disabled="deleteLoading">
            Отмена
          </button>
          <button @click="confirmDelete" class="btn-delete-confirm" :disabled="deleteLoading">
            <span v-if="deleteLoading" class="btn-loading">
              <span class="spinner"></span>
              Удаление...
            </span>
            <span v-else>Удалить задачу</span>
          </button>
        </div>
      </div>
    </div>

    <NotesList
      v-if="this.showNotesList"
      :objectId="objectId"
      :isProject="false"
      :userId="currentUser.id"
      @close="closeDiscussions"
      @showPushMessage="$emit('showPushMessage', $event)"
    />

    <MySpace
      v-if="activeTab === 'my-space'"
      :current-user="currentUser"
      :is-authenticated="isAuthenticated"
      :refresh="refreshTaskList"
      @refreshed="refreshTaskList = false"
      @delete-task="openDeleteModal"
      @open-discussions="openDiscussions"
    />

    <div v-else-if="activeTab === 'projects'" class="projects-container">
      <Projects
        v-if="projectsView === 'list'"
        :current-user="currentUser"
        @select-project="openProject"
      />

      <ProjectPage
        v-else-if="projectsView === 'project'"
        :projectId="selectedProjectId"
        :currentUser="currentUser"
        :onBack="showProjectList"
        :refresh="refreshTaskList"
        @refreshed="refreshTaskList = false"
        @delete-task="openDeleteModal"
        @open-discussions="openDiscussions"
      />
    </div>

    <div v-else-if="activeTab === 'all-tasks'" class="fallback-content">
      <h2>Все задачи</h2>
      <p>Этот раздел теперь в "Моем пространстве"</p>
    </div>
  </div>
</template>
<style scoped>
body {
  height: 100vh;
  width: 100vw;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: sans-serif;
  overflow: hidden;
}

.container {
  border-radius: 10px;
  width: 100%;
  text-align: center;
  position: relative;
}

.projects-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  margin-bottom: 20px;
}

h1 {
  font-size: 2.5em;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

.create-btn {
  background: linear-gradient(to right, #15cd8d, #4ab3ff);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 30px;
  cursor: pointer;
  font-size: 1.1em;
  transition: all 0.3s ease;
  box-shadow: 0 3px 5px rgba(0, 0, 0, 0.2);
}

.create-btn:hover {
  background: linear-gradient(to right, #12b87e, #3a9ee6);
  transform: scale(1.05);
  box-shadow: 0 5px 8px rgba(0, 0, 0, 0.3);
}

.create-btn:active {
  transform: scale(0.95);
  box-shadow: none;
}

.fallback-content {
  padding: 40px;
  color: white;
}

.fallback-content h2 {
  margin-bottom: 16px;
}

.delete-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10000;
  backdrop-filter: blur(5px);
  animation: fadeIn 0.3s ease-out;
}

.delete-modal {
  background: white;
  border-radius: 20px;
  padding: 0;
  width: 90%;
  max-width: 450px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.3);
  border: 1px solid #e2e8f0;
  animation: modalAppear 0.3s ease-out;
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10001;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes modalAppear {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.delete-modal-header {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%);
  color: white;
  padding: 2rem;
  text-align: center;
  border-radius: 20px 20px 0 0;
}

.delete-icon {
  font-size: 3.5rem;
  margin-bottom: 1rem;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
  animation: bounceIn 0.6s ease-out;
}

@keyframes bounceIn {
  0% {
    opacity: 0;
    transform: scale(0.3);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

.delete-modal-header h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.delete-modal-body {
  padding: 2rem;
}

.delete-warning {
  text-align: center;
  font-size: 1.2rem;
  color: #2d3748;
  margin-bottom: 1.5rem;
  line-height: 1.5;
  font-weight: 500;
}

.delete-details {
  background: #f8fafc;
  border-radius: 12px;
  padding: 1.25rem;
  margin-bottom: 1.5rem;
  border: 1px solid #e2e8f0;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
}

.detail-item:not(:last-child) {
  border-bottom: 1px solid #e2e8f0;
}

.detail-label {
  color: #64748b;
  font-weight: 500;
  font-size: 0.9rem;
}

.detail-value {
  color: #1e293b;
  font-weight: 600;
  font-size: 0.9rem;
}

.delete-alert {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  background: #fff5f5;
  border: 1px solid #fed7d7;
  border-radius: 12px;
  padding: 1.25rem;
  margin-top: 1rem;
}

.alert-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
  margin-top: 2px;
}

.alert-text {
  color: #dc2626;
  font-size: 0.9rem;
  line-height: 1.5;
  font-weight: 500;
}

.delete-modal-actions {
  display: flex;
  gap: 1rem;
  padding: 1.5rem 2rem 2rem;
  border-top: 1px solid #f1f5f9;
}

.btn-delete-confirm {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border: none;
  padding: 1rem 1.5rem;
  border-radius: 12px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  flex: 1;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
  position: relative;
  overflow: hidden;
}

.btn-delete-confirm::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.btn-delete-confirm:hover:not(:disabled)::before {
  left: 100%;
}

.btn-delete-confirm:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.4);
}

.btn-delete-confirm:active:not(:disabled) {
  transform: translateY(0);
}

.btn-delete-confirm:disabled {
  background: #cbd5e0;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-delete-cancel {
  background: white;
  color: #64748b;
  border: 2px solid #e2e8f0;
  padding: 1rem 1.5rem;
  border-radius: 12px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  flex: 1;
  transition: all 0.3s ease;
}

.btn-delete-cancel:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.btn-delete-cancel:disabled {
  background: #f7fafc;
  color: #a0aec0;
  cursor: not-allowed;
}

.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid transparent;
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .my-space {
    padding: 16px;
  }

  .space-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .header-left {
    justify-content: center;
  }

  .create-btn {
    width: 100%;
    justify-content: center;
  }

  .view-controls {
    flex-direction: column;
    gap: 16px;
  }

  .task-type-info {
    text-align: center;
  }

  .view-buttons {
    width: 100%;
    justify-content: center;
  }

  .view-btn {
    flex: 1;
    justify-content: center;
  }

  .delete-modal {
    width: 95%;
    margin: 0;
    border-radius: 16px;
  }

  .delete-modal-actions {
    flex-direction: column;
    gap: 0.75rem;
  }

  .delete-modal-header {
    padding: 1.5rem;
  }

  .delete-modal-body {
    padding: 1.5rem;
  }

  .delete-icon {
    font-size: 3rem;
  }
}

@media (max-width: 480px) {
  .task-type-selector {
    flex-direction: column;
    width: 100%;
  }

  .view-buttons {
    flex-direction: column;
  }

  .view-btn {
    padding: 12px;
  }

  .delete-modal {
    width: 90%;
    margin: 0;
  }
}
</style>
