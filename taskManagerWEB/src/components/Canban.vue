<script>
import TaskService from '@/services/Task-service.js'
import SimpleFilter from '@/components/Simple-filter.vue'
import ProjectService from '@/services/Project-service.js'
import TaskForm from '@/components/Task-form.vue'

export default {
  components: {
    SimpleFilter,
    TaskForm,
  },
  emits: ['open-discussions'],
  props: {
    currentUser: Object,
    projectMembers: {
      type: Array,
      default: () => [],
    },
    refresh: Boolean,
    externalFilters: {
      type: Object,
      default: () => ({}),
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
    customTasks: {
      type: Array,
      default: () => [],
    },
    customFilter: {
      type: Function,
      default: null,
    },
  },
  data() {
    return {
      taskList: [],
      tasks: [],
      loading: true,
      error: null,
      filteredTasks: [],
      showCreateForm: false,
      currentFilters: {},
      editingTask: null,
      showDeleteModal: false,
      taskToDelete: null,
    }
  },
  watch: {
    refresh(newVal) {
      if (newVal) {
        this.fetchTasks()
        this.$emit('refreshed')
      }
    },
    externalFilters: {
      handler(newFilters) {
        this.applyFilter(newFilters)
      },
      deep: true,
      immediate: true,
    },
  },
  async mounted() {
    await this.fetchTasks()

    this.refreshInterval = setInterval(() => {
      this.silentFetchTasks()
    }, 30000)
  },

  beforeDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval)
    }
  },

  methods: {
    isTaskCreator(task) {
      if (!task || !task.creator) return false

      const currentUserId = this.getCurrentUserId()
      if (!currentUserId) {
        return false
      }

      const creatorId = typeof task.creator === 'object' ? task.creator.id : task.creator
      return creatorId === currentUserId
    },

    getCurrentUserId() {
      if (this.currentUser && this.currentUser.id) {
        return this.currentUser.id
      }
    },
    async fetchTasks() {
      this.loading = true
      this.error = null
      try {
        let response
        switch (this.taskSource) {
          case 'personal':
            response = await TaskService.getTasksByPersonalId(this.sourceId)
            break

          case 'work':
            if (!this.sourceId) {
              throw new Error('User ID is required for assigned tasks')
            }
            response = await TaskService.getTasksByAssignee(this.sourceId)
            if (response.data) {
              response.data = response.data.filter((task) => {
                const isProjectTask = task.project !== null && task.project !== undefined
                if (isProjectTask) {
                  if (typeof task.project === 'object') {
                    return task.project.id !== undefined
                  }
                  return true
                }

                return false
              })
            }
            break
          case 'project':
            if (!this.sourceId) {
              throw new Error('Project ID is required for project tasks')
            }
            response = await ProjectService.getProjectTasks(this.sourceId)
            break
        }

        this.tasks = response.data || response
        this.filteredTasks = this.applyCustomFilter(this.tasks)
      } catch (err) {
        this.error = err.message
      } finally {
        this.loading = false
      }
    },

    async silentFetchTasks() {
      try {
        let response

        switch (this.taskSource) {
          case 'personal':
            response = await TaskService.getTasksByPersonalId(this.sourceId)
            break
          case 'work':
            if (!this.sourceId) {
              return
            }
            response = await TaskService.getTasksByAssignee(this.sourceId)
            if (response.data) {
              response.data = response.data.filter((task) => {
                const isProjectTask = task.project !== null && task.project !== undefined
                if (isProjectTask) {
                  if (typeof task.project === 'object') {
                    return task.project.id !== undefined
                  }
                  return true
                }
                return false
              })
            }
            break
          case 'project':
            if (!this.sourceId) {
              return
            }
            response = await ProjectService.getProjectTasks(this.sourceId)
            break
        }

        this.tasks = response.data || response
        this.filteredTasks = this.applyCustomFilter(this.tasks)
      } catch (err) {}
    },

    handleCustomTasks(tasks) {
      this.tasks = Array.isArray(tasks) ? tasks : []
      this.filteredTasks = this.applyCustomFilter(this.tasks)
      this.loading = false
    },

    applyCustomFilter(tasks) {
      if (this.customFilter && typeof this.customFilter === 'function') {
        return tasks.filter(this.customFilter)
      }
      return tasks
    },

    handleTaskCreated(newTask) {
      this.tasks.unshift(newTask)
      this.applyFilter(this.currentFilters)
    },

    applyFilter(filters) {
      this.currentFilters = filters

      if (Object.values(filters).every((value) => !value)) {
        this.filteredTasks = this.tasks
        return
      }

      this.filteredTasks = this.tasks.filter((task) => {
        const matchesSearch =
          !filters.search || task.title.toLowerCase().includes(filters.search.toLowerCase())

        const matchesStatus =
          !filters.status ||
          (task.status && task.status.id && task.status.id.toString().includes(filters.status))

        const matchesPriority =
          !filters.priority ||
          (task.priority &&
            task.priority.id &&
            task.priority.id.toString().includes(filters.priority))

        const matchesCreator =
          !filters.creator ||
          (task.creator &&
            ((typeof task.creator === 'object' &&
              task.creator.name &&
              task.creator.name.toLowerCase().includes(filters.creator.toLowerCase())) ||
              (typeof task.creator === 'string' &&
                task.creator.toLowerCase().includes(filters.creator.toLowerCase()))))

        const matchesCreateDate =
          !filters.createDate || this.matchesDateFilter(task.createDate, filters.createDate)

        const matchesDeadline =
          !filters.deadlineDate || this.matchesDateFilter(task.deadlineDate, filters.deadlineDate)

        return (
          matchesSearch &&
          matchesStatus &&
          matchesPriority &&
          matchesCreator &&
          matchesCreateDate &&
          matchesDeadline
        )
      })
    },

    matchesDateFilter(taskDate, filterValue) {
      if (filterValue && !taskDate) return false
      if (!filterValue || !taskDate) return true

      const getUTCDate = (date) => {
        if (!date) return null
        const d = new Date(date)
        return new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()))
      }

      const taskUTCDate = getUTCDate(taskDate)
      const todayUTCDate = getUTCDate(new Date())

      if (!taskUTCDate || !todayUTCDate) return false

      switch (filterValue) {
        case 'overdue':
          return taskUTCDate < todayUTCDate

        case 'yesterday':
          const yesterdayUTCDate = new Date(todayUTCDate)
          yesterdayUTCDate.setUTCDate(todayUTCDate.getUTCDate() - 1)
          return taskUTCDate.getTime() === yesterdayUTCDate.getTime()

        case 'today':
          return taskUTCDate.getTime() === todayUTCDate.getTime()

        case 'tomorrow':
          const tomorrowUTCDate = new Date(todayUTCDate)
          tomorrowUTCDate.setUTCDate(todayUTCDate.getUTCDate() + 1)
          return taskUTCDate.getTime() === tomorrowUTCDate.getTime()

        case 'thisWeek':
          const endOfThisWeekUTC = new Date(todayUTCDate)
          endOfThisWeekUTC.setUTCDate(todayUTCDate.getUTCDate() + 6)
          return taskUTCDate >= todayUTCDate && taskUTCDate <= endOfThisWeekUTC

        case 'nextWeek':
          const startOfNextWeekUTC = new Date(todayUTCDate)
          startOfNextWeekUTC.setUTCDate(todayUTCDate.getUTCDate() + 7)
          const endOfNextWeekUTC = new Date(todayUTCDate)
          endOfNextWeekUTC.setUTCDate(todayUTCDate.getUTCDate() + 13)
          return taskUTCDate >= startOfNextWeekUTC && taskUTCDate <= endOfNextWeekUTC

        case 'thisMonth':
          const endOfThisMonthUTC = new Date(
            Date.UTC(todayUTCDate.getUTCFullYear(), todayUTCDate.getUTCMonth() + 1, 0),
          )
          return taskUTCDate >= todayUTCDate && taskUTCDate <= endOfThisMonthUTC

        case 'nextMonth':
          const nextMonthFirstDayUTC = new Date(
            Date.UTC(todayUTCDate.getUTCFullYear(), todayUTCDate.getUTCMonth() + 1, 1),
          )
          const nextMonthLastDayUTC = new Date(
            Date.UTC(todayUTCDate.getUTCFullYear(), todayUTCDate.getUTCMonth() + 2, 0),
          )
          return taskUTCDate >= nextMonthFirstDayUTC && taskUTCDate <= nextMonthLastDayUTC

        case 'future':
          return taskUTCDate > todayUTCDate

        default:
          return true
      }
    },

    getTasksByStatus(status) {
      const statusIdMap = {
        активная: 1,
        'в работе': 3,
        'на проверке': 4,
        завершена: 2,
      }

      const targetStatusId = statusIdMap[status.toLowerCase()]

      const filteredTasks = this.filteredTasks.filter((task) => {
        if (!task.status) return false

        if (typeof task.status === 'object' && task.status.id) {
          return task.status.id === targetStatusId
        } else if (typeof task.status === 'number') {
          return task.status === targetStatusId
        } else if (typeof task.status === 'string') {
          const taskStatusName = task.status.toLowerCase().trim()
          return taskStatusName === status.toLowerCase()
        }

        return false
      })
      return filteredTasks.sort((a, b) => (a.priority?.id || 4) - (b.priority?.id || 4))
    },
    getPriorityDisplayName(priority) {
      if (!priority) return 'Без приоритета'

      if (typeof priority === 'string') {
        return priority
      } else if (priority.name) {
        return priority.name
      } else if (priority.id) {
        return this.getPriorityNameById(priority.id)
      }

      return 'Без приоритета'
    },

    getPriorityNameById(priorityId) {
      const priorityMap = {
        1: 'высокий',
        2: 'средний',
        3: 'низкий',
      }
      return priorityMap[priorityId] || 'Без приоритета'
    },

    getStatusMapping(status) {
      const mapping = {
        активная: 'активная',
        'в работе': 'в работе',
        'на проверке': 'на проверке',
        завершена: 'завершена',
      }
      return mapping[status.toLowerCase()] || status
    },

    getPriorityColor(priority) {
      if (!priority) {
        return '#6b7280'
      }

      let priorityName = ''

      if (typeof priority === 'string') {
        priorityName = priority.toLowerCase()
      } else if (priority.name) {
        priorityName = priority.name.toLowerCase()
      } else if (priority.id) {
        priorityName = this.getPriorityNameById(priority.id)
      } else {
        priorityName = String(priority).toLowerCase()
      }

      const colors = {
        высокий: '#ef4444',
        средний: '#f59e0b',
        низкий: '#10b981',
      }

      const color = colors[priorityName] || '#6b7280'
      return color
    },

    getStatusColor(status) {
      if (!status) return '#6b7280'

      let statusName = ''

      if (typeof status === 'string') {
        statusName = status.toLowerCase()
      } else if (status.name) {
        statusName = status.name.toLowerCase()
      } else if (status.id) {
        statusName = this.getStatusNameById(status.id)
      } else {
        statusName = String(status).toLowerCase()
      }

      const colors = {
        активная: '#f59e0b',
        'в работе': '#3b82f6',
        'на проверке': '#8b5cf6',
        завершена: '#10b981',
      }

      return colors[statusName] || '#6b7280'
    },

    getStatusNameById(statusId) {
      const statusMap = {
        1: 'активная',
        2: 'завершена',
        3: 'в работе',
        4: 'на проверке',
      }
      return statusMap[statusId] || 'активная'
    },

    getStatusDisplayName(status) {
      const statusMap = {
        активная: 'Активные',
        'в работе': 'В работе',
        'на проверке': 'На проверке',
        завершена: 'Завершённые',
      }
      return statusMap[status] || status
    },

    formatDate(dateString) {
      if (!dateString) return ''
      try {
        const date = new Date(dateString)
        return date.toLocaleDateString('ru-RU')
      } catch (e) {
        return dateString
      }
    },

    getPersonName(person) {
      if (!person) return 'Неизвестно'
      if (typeof person === 'string') return person
      return person.name || person.username || 'Неизвестно'
    },

    getPersonInitial(person) {
      const name = this.getPersonName(person)
      return name ? name.charAt(0).toUpperCase() : '?'
    },

    toggleDropdown(taskId) {
      this.tasks = this.tasks.map((task) => {
        if (task.id === taskId) {
          task.showDropdown = !task.showDropdown
        } else {
          task.showDropdown = false
        }
        return task
      })
    },

    handleDeleteTask(taskId) {
      this.$emit('delete-task', taskId)
      this.toggleDropdown(taskId)
    },

    editTask(task) {
      this.editingTask = task
    },

    onTaskUpdated(updatedTask) {
      const index = this.tasks.findIndex((t) => t.id === updatedTask.id)
      if (index !== -1) {
        this.tasks.splice(index, 1, updatedTask)
      }

      this.applyFilter(this.currentFilters)
      this.checkTaskVisibility(updatedTask)

      this.closeEditForm()
    },

    closeEditForm() {
      this.editingTask = null
    },

    checkTaskVisibility(updatedTask) {
      if (this.taskSource === 'work' && this.sourceId) {
        const isStillAssignee = updatedTask.assignees?.some((assignee) => {
          const assigneeId = typeof assignee === 'object' ? assignee.id : assignee
          return assigneeId === this.sourceId
        })

        if (!isStillAssignee) {
          this.tasks = this.tasks.filter((t) => t.id !== updatedTask.id)
          this.filteredTasks = this.filteredTasks.filter((t) => t.id !== updatedTask.id)
        }
      }
    },

    openDiscussions(task) {
      this.$emit('open-discussions', task.id)
    }
  },
}
</script>

<template>
  <div class="task-manager">
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>Загрузка задач...</span>
    </div>

    <div v-else-if="error" class="error-state">
      <div class="error-icon">⚠️</div>
      <div class="error-content">
        <h3>Ошибка загрузки</h3>
        <p>{{ error }}</p>
        <button @click="fetchTasks" class="btn-retry">Повторить попытку</button>
      </div>
    </div>

    <div v-else class="kanban-board">
      <div
        class="kanban-column"
        v-for="status in ['активная', 'в работе', 'на проверке', 'завершена']"
        :key="status"
      >
        <div class="column-header">
          <div class="status-indicator" :style="{ backgroundColor: getStatusColor(status) }"></div>
          <h3>{{ getStatusDisplayName(status) }}</h3>
          <span class="task-count">{{ getTasksByStatus(status).length }}</span>
        </div>

        <div class="tasks-container">
          <transition-group name="task-animation" tag="div">
            <div
              v-for="task in getTasksByStatus(status)"
              :key="task.id"
              class="task-card"
              :class="`priority-${(task.priority?.name || 'medium').toLowerCase()}`"
            >
              <div class="task-header">
                <div
                  class="priority-badge"
                  :style="{ backgroundColor: getPriorityColor(task.priority) }"
                >
                  {{ getPriorityDisplayName(task.priority) }}
                </div>
                <div class="task-actions">
                  <div class="dropdown">
                    <button class="btn-icon dropdown-toggle" @click="toggleDropdown(task.id)">
                      ⋮
                    </button>
                    <div class="dropdown-menu" :class="{ show: task.showDropdown }">
                      <button class="dropdown-item" @click="editTask(task)">Редактировать</button>
                      <button
                        v-if="isTaskCreator(task)"
                        class="dropdown-item delete-task"
                        @click="handleDeleteTask(task.id)"
                      >
                        Удалить
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <h4 class="task-title">{{ task.title }}</h4>

              <div class="task-people">
                <div class="creator-section" v-if="task.creator">
                  <div class="people-label">Создатель:</div>
                  <div class="creator-badge">
                    <div class="person-avatar small">
                      {{ getPersonInitial(task.creator) }}
                    </div>
                    <span class="person-name">{{ getPersonName(task.creator) }}</span>
                  </div>
                </div>

                <div class="assignees-section" v-if="task.assignees && task.assignees.length > 0">
                  <div class="people-label">Исполнители:</div>
                  <div class="assignees-badges">
                    <div
                      v-for="assignee in task.assignees"
                      :key="assignee.id"
                      class="assignee-badge"
                      :title="getPersonName(assignee)"
                    >
                      <div class="person-avatar small">
                        {{ getPersonInitial(assignee) }}
                      </div>
                      <span class="person-name">{{ getPersonName(assignee) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="task-meta">
                <div class="meta-item" v-if="task.deadlineDate">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path
                      d="M12 8V12L15 15M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z"
                      stroke="currentColor"
                      stroke-width="2"
                    />
                  </svg>
                  <span>{{ formatDate(task.deadlineDate) }}</span>
                </div>
              </div>
              <div class="discussions-section">
                <button
                  class="btn-discussions"
                  @click="openDiscussions(task)"
                  :title="`Открыть обсуждения по задаче: ${task.title}`"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="discussions-icon">
                    <path
                      d="M20 2H4C2.9 2 2 2.9 2 4V22L6 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span>Обсуждения</span>
                  <span class="comments-count" v-if="task.commentsCount">({{ task.commentsCount }})</span>
                </button>
              </div>
            </div>
          </transition-group>
        </div>
      </div>
    </div>

    <TaskForm
      v-if="editingTask"
      :is-visible="!!editingTask"
      :task="editingTask"
      :task-source="taskSource"
      :source-id="sourceId"
      @task-updated="onTaskUpdated"
      @close="closeEditForm"
      :project-members="projectMembers"
    />
  </div>
</template>

<style scoped>
.task-manager {
  padding: 0;
  animation: fadeIn 0.3s ease;
  width: 100%;
}

.kanban-board {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 0;
  padding: 0;
  animation: slideUp 0.3s ease;
  overflow-x: auto;
}

.kanban-column {
  background: white;
  border-radius: 16px;
  padding: 16px;
  min-height: 500px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e0e0;
  transition: all 0.3s ease;
}

.kanban-column:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.column-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

.column-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-count {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  min-width: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.tasks-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 100px;
}

.task-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 8px;
}

.priority-badge {
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  color: white;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-icon {
  background: white;
  border: 1px solid #e0e0e0;
  color: #667eea;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-size: 16px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.btn-icon:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: #667eea;
  transform: translateY(-1px);
}

.task-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.4;
  margin: 0 0 16px 0;
  word-wrap: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-people {
  margin-bottom: 16px;
}

.people-label {
  font-size: 11px;
  color: #6b7280;
  font-weight: 600;
  text-transform: uppercase;
  margin-bottom: 6px;
  letter-spacing: 0.3px;
}

.creator-section,
.assignees-section {
  margin-bottom: 12px;
}

.creator-section:last-child,
.assignees-section:last-child {
  margin-bottom: 0;
}

.creator-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  transition: all 0.2s ease;
}

.creator-badge:hover {
  background: #f1f5f9;
}

.assignees-badges {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.assignee-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #bae6fd;
  transition: all 0.2s ease;
}

.assignee-badge:hover {
  background: #e0f2fe;
}

.person-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.person-avatar.small {
  width: 20px;
  height: 20px;
  font-size: 10px;
}

.person-name {
  font-size: 12px;
  font-weight: 500;
  color: #2c3e50;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.task-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.meta-item svg {
  color: #667eea;
  flex-shrink: 0;
}

.discussions-section {
  margin-top: 12px;
}

.btn-discussions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-discussions:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.discussions-icon {
  flex-shrink: 0;
}

.comments-count {
  font-size: 11px;
  opacity: 0.9;
  font-weight: 500;
}

.dropdown {
  position: relative;
}

.dropdown-toggle {
  background: white;
  border: 1px solid #e0e0e0;
  color: #667eea;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-size: 16px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.dropdown-toggle:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: #667eea;
  transform: translateY(-1px);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  padding: 8px 0;
  min-width: 140px;
  z-index: 10;
  display: none;
  border: 1px solid #e0e0e0;
  animation: fadeIn 0.2s ease;
}

.dropdown-menu.show {
  display: block;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 8px 16px;
  text-align: left;
  border: none;
  background: none;
  cursor: pointer;
  color: #2c3e50;
  font-size: 13px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.dropdown-item:hover {
  background-color: #f8f9fa;
  color: #667eea;
}

.dropdown-item.delete-task {
  color: #ef4444;
}

.dropdown-item.delete-task:hover {
  background-color: #fee2e2;
  color: #dc2626;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  text-align: center;
  animation: slideUp 0.3s ease;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #e0e0e0;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error-icon {
  font-size: 56px;
  margin-bottom: 20px;
  color: #ef4444;
}

.error-content h3 {
  color: #2c3e50;
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 600;
}

.error-content p {
  color: #6b7280;
  margin-bottom: 24px;
  max-width: 400px;
  line-height: 1.5;
}

.btn-retry {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-retry:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.task-animation-enter-active {
  transition: all 0.4s ease;
}

.task-animation-leave-active {
  transition: all 0.3s ease;
  position: absolute;
  width: calc(100% - 16px);
}

.task-animation-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

.task-animation-leave-to {
  opacity: 0;
  transform: translateX(-100%) scale(0.95);
}

.task-animation-move {
  transition: transform 0.3s ease;
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

@media (max-width: 1200px) {
  .kanban-board {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .kanban-board {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .kanban-column {
    min-height: 400px;
    padding: 12px;
  }

  .task-people {
    margin-bottom: 12px;
  }

  .creator-badge,
  .assignee-badge {
    padding: 6px 10px;
  }

  .person-name {
    font-size: 11px;
  }

  .btn-discussions {
    padding: 8px 12px;
    font-size: 11px;
  }

  .task-count {
    padding: 4px 10px;
    font-size: 11px;
  }
}
</style>
