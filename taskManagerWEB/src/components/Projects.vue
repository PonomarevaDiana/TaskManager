<script>
import ProjectsFilter from './Projects-filter.vue'
import CreateProjectForm from './Create-project-form.vue'
import ProjectService from '@/services/Project-service.js'

export default {
  name: 'ProjectsPage',
  components: {
    ProjectsFilter,
    CreateProjectForm,
  },
  data() {
    return {
      deleteLoading: false,
      projects: [],
      projectsLoading: false,
      showCreateForm: false,
      editingProject: null,
      showDeleteConfirm: false,
      projectToDelete: null,
      filters: {
        search: '',
        owner: '',
        status: '',
        createDate: '',
        sortBy: 'newest',
      },
    }
  },

  props: {
    currentUser: Object,
  },
  computed: {
    filteredProjects() {
      let filtered = this.projects

      if (this.filters.search) {
        const query = this.filters.search.toLowerCase()
        filtered = filtered.filter(
          (project) =>
            project.name.toLowerCase().includes(query) ||
            (project.description && project.description.toLowerCase().includes(query)),
        )
      }

      if (this.filters.owner) {
        const ownerQuery = this.filters.owner.toLowerCase()
        filtered = filtered.filter((project) => {
          const owner = this.getProjectOwner(project)
          return owner.name.toLowerCase().includes(ownerQuery)
        })
      }

      if (this.filters.status) {
        filtered = filtered.filter((project) => project.status === this.filters.status)
      }

      if (this.filters.createDate) {
        filtered = this.filterByDate(filtered, this.filters.createDate)
      }

      if (this.filters.sortBy) {
        switch (this.filters.sortBy) {
          case 'newest':
            filtered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
            break
          case 'oldest':
            filtered.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
            break
          case 'name':
            filtered.sort((a, b) => a.name.localeCompare(b.name))
            break
          case 'name_desc':
            filtered.sort((a, b) => b.name.localeCompare(a.name))
            break
        }
      }

      return filtered
    },
  },
  mounted() {
    this.loadProjectsWithMembers()
  },
  methods: {
    async loadProjectsWithMembers() {
      this.projectsLoading = true

      try {
        const response = await ProjectService.getAllProjects()
        console.log('Projects loaded:', response.data)
        this.projects = await this.loadMembersForProjects(response.data || [])
      } catch (error) {
        this.projects = []
      } finally {
        this.projectsLoading = false
      }
    },

    async loadMembersForProjects(projects) {
      const projectsWithMembers = []

      for (const project of projects) {
        try {
          const membersResponse = await ProjectService.getProjectMembers(project.id)

          projectsWithMembers.push({
            ...project,
            members: membersResponse.data || [],
          })

          console.log(
            `Loaded ${membersResponse.data?.length || 0} members for project ${project.name}`,
          )
        } catch (error) {
          console.error(`Error loading members for project ${project.id}:`, error)
          projectsWithMembers.push({
            ...project,
            members: [],
          })
        }
      }

      return projectsWithMembers
    },

    getProjectOwner(project) {
      if (!project.members || project.members.length === 0) {
        return {
          name: this.currentUser?.name || 'Вы',
          id: this.currentUser?.id || 'current-user',
        }
      }

      const owner = project.members.find((member) => member.role === 'ROLE_OWNER')

      if (!owner) {
        return { name: 'Неизвестно', id: 'unknown' }
      }

      return {
        name: owner.username || 'Без имени',
        id: owner.userId,
      }
    },

    isProjectOwner(project) {
      const owner = this.getProjectOwner(project)
      return owner.id === this.currentUser?.id
    },

    getMembersCount(project) {
      return project.members?.length || 0
    },

    getTasksCount(project) {
      return project.tasks?.length || 0
    },

    editProject(project, event) {
      event.stopPropagation()
      this.editingProject = project
      this.showCreateForm = true
    },

    confirmDeleteProject(project, event) {
      event.stopPropagation()
      this.projectToDelete = project
      this.showDeleteConfirm = true
    },

    async deleteProject() {
      if (!this.projectToDelete) return

      this.deleteLoading = true

      try {
        await ProjectService.deleteProject(this.projectToDelete.id)

        this.showDeleteSuccess = true
        setTimeout(() => {
          this.showDeleteSuccess = false
        }, 2000)

        this.showDeleteConfirm = false
        this.projectToDelete = null

        await this.loadProjectsWithMembers()
      } catch (error) {
        console.error('Error deleting project:', error)

        let errorMessage = 'Ошибка при удалении проекта'
        if (error.response?.data?.message) {
          errorMessage += ': ' + error.response.data.message
        }

        alert(errorMessage)
      } finally {
        this.deleteLoading = false
      }
    },

    cancelDelete() {
      this.showDeleteConfirm = false
      this.projectToDelete = null
    },

    handleFilter(filters) {
      this.filters = { ...this.filters, ...filters }
    },

    resetFilters() {
      this.filters = {
        search: '',
        owner: '',
        status: '',
        createDate: '',
        sortBy: 'newest',
      }
    },

    async handleProjectCreated() {
      await this.loadProjectsWithMembers()
      this.showCreateForm = false
    },

    async handleProjectUpdated() {
      await this.loadProjectsWithMembers()
      this.showCreateForm = false
      this.editingProject = null
    },
    closeCreateForm() {
      this.showCreateForm = false
      this.editingProject = null
    },

    filterByDate(projects, dateFilter) {
      const now = new Date()
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

      return projects.filter((project) => {
        const projectDate = new Date(project.createdAt)
        const projectDay = new Date(
          projectDate.getFullYear(),
          projectDate.getMonth(),
          projectDate.getDate(),
        )

        switch (dateFilter) {
          case 'today':
            return projectDay.getTime() === today.getTime()
          case 'yesterday':
            const yesterday = new Date(today)
            yesterday.setDate(yesterday.getDate() - 1)
            return projectDay.getTime() === yesterday.getTime()
          case 'thisWeek':
            const weekStart = new Date(today)
            weekStart.setDate(weekStart.getDate() - weekStart.getDay())
            return projectDay >= weekStart
          case 'lastWeek':
            const lastWeekStart = new Date(today)
            lastWeekStart.setDate(lastWeekStart.getDate() - lastWeekStart.getDay() - 7)
            const lastWeekEnd = new Date(lastWeekStart)
            lastWeekEnd.setDate(lastWeekEnd.getDate() + 6)
            return projectDay >= lastWeekStart && projectDay <= lastWeekEnd
          case 'thisMonth':
            return (
              projectDate.getMonth() === now.getMonth() &&
              projectDate.getFullYear() === now.getFullYear()
            )
          case 'lastMonth':
            const lastMonth = now.getMonth() === 0 ? 11 : now.getMonth() - 1
            const lastMonthYear = now.getMonth() === 0 ? now.getFullYear() - 1 : now.getFullYear()
            return (
              projectDate.getMonth() === lastMonth && projectDate.getFullYear() === lastMonthYear
            )
          default:
            return true
        }
      })
    },

    openProject(project) {
      this.$emit('select-project', project.id)
    },

    formatDate(dateString) {
      return new Date(dateString).toLocaleDateString('ru-RU')
    },
  },
}
</script>

<template>
  <div class="projects-container">
    <div class="projects-header">
      <h2>Проекты</h2>
      <button class="create-btn" @click="showCreateForm = true">+ Создать проект</button>
    </div>

    <ProjectsFilter @filter="handleFilter" />

    <div class="projects-content">
      <div v-if="projectsLoading" class="loading-container">
        <div class="loading-spinner"></div>
        <p class="loading-text">Загрузка проектов...</p>
      </div>

      <div v-else-if="filteredProjects.length === 0" class="empty-state">
        <div class="empty-content">
          <div class="empty-icon">🚀</div>
          <h3 class="empty-title">Проектов не найдено</h3>
          <p class="empty-text" v-if="projects.length === 0">У вас пока нет проектов. Создайте первый!</p>
          <p class="empty-text" v-else>Попробуйте изменить параметры фильтра</p>
        </div>
      </div>

      <div v-else class="projects-grid">
        <div
          v-for="project in filteredProjects"
          :key="project.id"
          class="project-card"
          @click="openProject(project)"
        >
          <div class="project-actions" v-if="isProjectOwner(project)">
            <button
              class="action-btn edit-btn"
              @click="editProject(project, $event)"
              title="Редактировать проект"
            >
              ✏️
            </button>
            <button
              class="action-btn delete-btn"
              @click="confirmDeleteProject(project, $event)"
              title="Удалить проект"
            >
              🗑️
            </button>
          </div>

          <div class="card-content">
            <h3 class="project-title">{{ project.name }}</h3>
            <p class="project-description" v-if="project.description">
              {{ project.description }}
            </p>
          </div>

          <div class="card-footer">
            <div class="project-meta">
              <div class="meta-item">
                <span class="meta-label">Владелец:</span>
                <span class="meta-value">{{ getProjectOwner(project).name }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">Создан:</span>
                <span class="meta-value">{{ formatDate(project.createdAt) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">Участников:</span>
                <span class="meta-value">{{ getMembersCount(project) }}</span>
              </div>
              <div class="meta-item" v-if="getTasksCount(project) > 0">
                <span class="meta-label">Задач:</span>
                <span class="meta-value">{{ getTasksCount(project) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Модальное окно удаления -->
    <div v-if="showDeleteConfirm" class="settings-overlay delete-modal-overlay" @click.self="cancelDelete">
      <div class="settings-modal delete-modal">
        <div class="settings-header">
          <h2>Удаление проекта</h2>
          <button class="close-btn" @click="cancelDelete" :disabled="deleteLoading">×</button>
        </div>

        <div class="settings-content">
          <div class="delete-content">
            <div class="delete-icon">🗑️</div>

            <div class="form-group">
              <p class="delete-warning">
                Вы собираетесь удалить проект <strong>"{{ projectToDelete?.name }}"</strong>
              </p>
            </div>

            <div class="delete-details" v-if="projectToDelete">
              <div class="detail-item">
                <span class="detail-label">Участников:</span>
                <span class="detail-value">{{ getMembersCount(projectToDelete) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Создан:</span>
                <span class="detail-value">{{ formatDate(projectToDelete.createdAt) }}</span>
              </div>
            </div>

            <div class="form-actions">
              <button
                @click="deleteProject"
                class="delete-btn-confirm"
                :disabled="deleteLoading"
              >
                <span v-if="deleteLoading" class="btn-loading">
                  <div class="spinner"></div>
                  Удаление...
                </span>
                <span v-else>Удалить проект</span>
              </button>
              <button
                @click="cancelDelete"
                class="cancel-btn"
                :disabled="deleteLoading"
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <CreateProjectForm
      :isVisible="showCreateForm"
      :editingProject="editingProject"
      @project-created="handleProjectCreated"
      @project-updated="handleProjectUpdated"
      @close="closeCreateForm"
      :currentUser="currentUser"
    />
  </div>
</template>

<style scoped>
.projects-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.projects-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.projects-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
}

.create-btn {
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

.create-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.create-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.projects-content {
  margin-top: 20px;
}

/* Загрузка */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
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

/* Пустое состояние */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
}

.empty-content {
  text-align: center;
  padding: 40px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.empty-title {
  margin: 0 0 12px 0;
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
}

.empty-text {
  margin: 0;
  color: #666;
  font-size: 15px;
  line-height: 1.5;
}

/* Сетка проектов */
.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.project-card {
  background: linear-gradient(135deg, #8a99dd 0%, #6fe7b3 100%);
  border-radius: 12px;
  border: 1px solid #e0e0e0;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
}

.project-card:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.project-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
  z-index: 2;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.edit-btn {
  color: #667eea;
}

.edit-btn:hover {
  background: #667eea;
  color: white;
}

.delete-btn {
  color: #e74c3c;
}

.delete-btn:hover {
  background: #e74c3c;
  color: white;
}

.card-content {
  padding: 20px;
  padding-top: 60px; /* Для кнопок действий */
}

.project-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.3;
}

.project-description {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #f8f9fa;
}

.project-meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 11px;
  color: #999;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.meta-value {
  font-size: 13px;
  color: #2c3e50;
  font-weight: 600;
}

/* Модальное окно удаления */
.delete-modal-overlay {
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

.delete-modal {
  width: 90%;
  max-width: 500px;
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

.delete-content {
  text-align: center;
}

.delete-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.delete-warning {
  margin: 0;
  font-size: 16px;
  color: #2c3e50;
  line-height: 1.5;
}

.delete-warning strong {
  color: #e74c3c;
}

.delete-details {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  margin: 20px 0;
  border: 1px solid #e0e0e0;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.detail-item:not(:last-child) {
  border-bottom: 1px solid #e0e0e0;
}

.detail-label {
  font-size: 14px;
  color: #666;
}

.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.delete-btn-confirm {
  padding: 12px 24px;
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
}

.delete-btn-confirm:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.delete-btn-confirm:disabled {
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

.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 768px) {
  .projects-container {
    padding: 16px;
  }

  .projects-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .create-btn {
    width: 100%;
  }

  .projects-grid {
    grid-template-columns: 1fr;
  }

  .project-meta {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .form-actions {
    flex-direction: column;
  }

  .delete-modal {
    width: 95%;
  }
}

@media (max-width: 480px) {
  .settings-content {
    padding: 20px;
  }

  .settings-header {
    padding: 20px;
  }

  .settings-header h2 {
    font-size: 18px;
  }

  .card-content {
    padding: 16px;
    padding-top: 56px;
  }
}
</style>
