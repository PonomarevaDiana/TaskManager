<script>
import TaskService from '@/services/Task-service.js'
import SimpleFilter from '@/components/Simple-filter.vue'
import TaskForm from '@/components/Task-form.vue'
import ProjectService from '@/services/Project-service.js'

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
      sortField: 'title',
      sortDirection: 'asc',
      searchQuery: '',
    }
  },
  computed: {
    sortedAndFilteredTasks() {
      let tasks = [...this.filteredTasks]

      if (this.searchQuery.trim()) {
        tasks = tasks.filter((task) =>
          task.title?.toLowerCase().includes(this.searchQuery.toLowerCase()),
        )
      }

      if (this.sortField) {
        tasks.sort((a, b) => {
          let aValue = this.getSortValue(a, this.sortField)
          let bValue = this.getSortValue(b, this.sortField)

          if (typeof aValue === 'string' && typeof bValue === 'string') {
            aValue = aValue.toLowerCase()
            bValue = bValue.toLowerCase()
          }

          if (aValue instanceof Date && bValue instanceof Date) {
            aValue = aValue.getTime()
            bValue = bValue.getTime()
          }

          if (this.sortDirection === 'asc') {
            if (aValue < bValue) return -1
            if (aValue > bValue) return 1
            return 0
          } else {
            if (aValue > bValue) return -1
            if (aValue < bValue) return 1
            return 0
          }
        })
      }

      return tasks
    },
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
    currentUser: {
      handler(newUser) {
        if (newUser && newUser.id) {
          this.fetchTasks()
        }
      },
      immediate: true,
      deep: true,
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
      if (!task?.creator || !this.currentUser?.id) {
        return false
      }

      const creatorId = typeof task.creator === 'object' ? task.creator.id : task.creator
      return creatorId.toString() === this.currentUser.id.toString()
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
      } catch (err) {
        console.log(err)
      }
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
        this.filteredTasks = [...this.tasks]
        return
      }

      this.filteredTasks = this.tasks.filter((task) => {
        const matchesSearch =
          !filters.search || task.title?.toLowerCase().includes(filters.search.toLowerCase())

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

    getSortValue(task, field) {
      switch (field) {
        case 'title':
          return task.title || ''
        case 'status':
          const statusValue = this.getStatusDisplayName(task.status)
          const statusWeights = {
            активная: 1,
            'в работе': 2,
            'на проверке': 3,
            завершена: 4,
          }
          return statusWeights[statusValue?.toLowerCase()] || 5
        case 'priority':
          const priorityValue = this.getPriorityDisplayName(task.priority)
          const priorityWeights = {
            высокий: 1,
            средний: 2,
            низкий: 3,
            'без приоритета': 4,
          }
          return priorityWeights[priorityValue?.toLowerCase()] || 5
        case 'deadline':
          if (!task.deadlineDate) return new Date('9999-12-31')
          try {
            return new Date(task.deadlineDate)
          } catch {
            return new Date('9999-12-31')
          }
        case 'creator':
          return this.getPersonName(task.creator) || 'zzz'
        case 'created':
          if (!task.createDate) return new Date(0)
          try {
            return new Date(task.createDate)
          } catch {
            return new Date(0)
          }
        default:
          return ''
      }
    },

    sortTasks(field) {
      if (this.sortField === field) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc'
      } else {
        this.sortField = field
        this.sortDirection = 'asc'
      }

      this.$forceUpdate()
    },

    getSortIcon(field) {
      if (this.sortField !== field) return '↕️'
      return this.sortDirection === 'asc' ? '↑' : '↓'
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

    getStatusDisplayName(status) {
      if (!status) return 'Не указан'

      let statusName = ''
      if (typeof status === 'string') {
        statusName = status
      } else if (status.name) {
        statusName = status.name
      } else if (status.id) {
        statusName = this.getStatusNameById(status.id)
      }

      const statusMap = {
        активная: 'Активная',
        'в работе': 'В работе',
        'на проверке': 'На проверке',
        завершена: 'Завершена',
      }
      return statusMap[statusName?.toLowerCase()] || statusName || 'Не указан'
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

    getPriorityColor(priority) {
      if (!priority) return '#6b7280'

      let priorityName = ''
      if (typeof priority === 'string') {
        priorityName = priority.toLowerCase()
      } else if (priority.name) {
        priorityName = priority.name.toLowerCase()
      } else if (priority.id) {
        priorityName = this.getPriorityNameById(priority.id)
      }

      const colors = {
        высокий: '#ef4444',
        средний: '#f59e0b',
        низкий: '#10b981',
      }
      return colors[priorityName] || '#6b7280'
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
      }

      const colors = {
        активная: '#f59e0b',
        'в работе': '#3b82f6',
        'на проверке': '#8b5cf6',
        завершена: '#10b981',
      }
      return colors[statusName] || '#6b7280'
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
      console.log('🔄 Задача обновлена:', updatedTask)
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

    openDiscussions(taskId) {
      this.$emit('open-discussions', taskId)
    },
  },
}
</script>

<template>
  <div class="task-list-view">
    <div class="list-controls">
      <div class="sort-controls">
        <span class="sort-label">Сортировка:</span>
        <button
          @click="sortTasks('title')"
          :class="['sort-btn', { active: sortField === 'title' }]"
        >
          Название {{ getSortIcon('title') }}
        </button>
        <button
          @click="sortTasks('status')"
          :class="['sort-btn', { active: sortField === 'status' }]"
        >
          Статус {{ getSortIcon('status') }}
        </button>
        <button
          @click="sortTasks('priority')"
          :class="['sort-btn', { active: sortField === 'priority' }]"
        >
          Приоритет {{ getSortIcon('priority') }}
        </button>
        <button
          @click="sortTasks('deadline')"
          :class="['sort-btn', { active: sortField === 'deadline' }]"
        >
          Дедлайн {{ getSortIcon('deadline') }}
        </button>
      </div>
    </div>

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

    <div v-else-if="sortedAndFilteredTasks.length === 0" class="empty-state">
      <div class="empty-icon">📋</div>
      <h3>Задачи не найдены</h3>
    </div>

    <div v-else class="tasks-list">
      <div class="list-header">
        <div class="header-cell">Задача</div>
        <div class="header-cell">Статус</div>
        <div class="header-cell">Приоритет</div>
        <div class="header-cell">Дедлайн</div>
        <div class="header-cell">Создатель</div>
        <div class="header-cell">Исполнители</div>
        <div class="header-cell actions">Действия</div>
      </div>

      <transition-group name="task-list" tag="div" class="list-body">
        <div v-for="task in sortedAndFilteredTasks" :key="task.id" class="task-row">
          <div class="cell task-info">
            <h4 class="task-title">{{ task.title }}</h4>
            <div class="task-meta" v-if="task.createDate">
              Создана: {{ formatDate(task.createDate) }}
            </div>
          </div>

          <div class="cell">
            <span class="status-badge" :style="{ backgroundColor: getStatusColor(task.status) }">
              {{ getStatusDisplayName(task.status) }}
            </span>
          </div>

          <div class="cell">
            <span
              class="priority-badge"
              :style="{ backgroundColor: getPriorityColor(task.priority) }"
            >
              {{ getPriorityDisplayName(task.priority) }}
            </span>
          </div>

          <div class="cell">
            <span
              class="deadline"
              :class="{ overdue: task.deadlineDate && new Date(task.deadlineDate) < new Date() }"
            >
              {{ formatDate(task.deadlineDate) || 'Не указан' }}
            </span>
          </div>

          <div class="cell">
            <div class="person-info">
              <div class="person-avatar">
                {{ getPersonInitial(task.creator) }}
              </div>
              <span class="person-name">{{ getPersonName(task.creator) }}</span>
            </div>
          </div>

          <div class="cell">
            <div class="assignees-names">
              <template v-if="task.assignees && task.assignees.length > 0">
                <span
                  v-for="(assignee, index) in task.assignees"
                  :key="assignee.id"
                  class="assignee-name"
                >
                  {{ getPersonName(assignee)
                  }}<template v-if="index < task.assignees.length - 1">, </template>
                </span>
              </template>
              <span v-else class="no-assignees">Не назначены</span>
            </div>
          </div>

          <div class="cell actions">
            <div class="action-buttons">
              <button
                class="btn-discussions"
                @click="openDiscussions(task.id)"
                :title="`Открыть обсуждения по задаче: ${task.title}`"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="discussions-icon">
                  <path
                    d="M20 2H4C2.9 2 2 2.9 2 4V22L6 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2Z"
                    fill="currentColor"
                  />
                </svg>
              </button>
              <div class="dropdown">
                <button class="btn-icon dropdown-toggle" @click="toggleDropdown(task.id)">⋮</button>
                <div class="dropdown-menu" :class="{ show: task.showDropdown }">
                  <button class="dropdown-item" @click="editTask(task)">Редактировать</button>
                  <button class="dropdown-item delete-task" @click="handleDeleteTask(task.id)">
                    Удалить
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition-group>
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
.task-list-view {
  padding: 0;
  animation: fadeIn 0.3s ease;
}

.list-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 20px;
  padding: 20px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.sort-label {
  font-size: 14px;
  color: #2c3e50;
  font-weight: 600;
}

.sort-btn {
  padding: 10px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  color: #2c3e50;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.sort-btn:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
}

.sort-btn.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.tasks-list {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease;
}

.list-header {
  display: grid;
  grid-template-columns: minmax(240px, 2fr) 120px 120px 120px 140px 160px 100px;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  font-weight: 600;
  color: white;
  font-size: 14px;
  align-items: center;
}

.list-body {
  max-height: 600px;
  overflow-y: auto;
}

.task-row {
  display: grid;
  grid-template-columns: minmax(240px, 2fr) 120px 120px 120px 140px 160px 100px;
  gap: 16px;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  align-items: center;
  background: white;
}

.task-row:hover {
  background: #f8f9fa;
}

.task-row:last-child {
  border-bottom: none;
}

.cell {
  display: flex;
  align-items: center;
  min-width: 0;
}

.task-info {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
  width: 100%;
}

.task-title {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  line-height: 1.4;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  font-size: 12px;
  color: #6b7280;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge,
.priority-badge {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  letter-spacing: 0.5px;
  white-space: nowrap;
  text-align: center;
  width: fit-content;
  min-width: 80px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.deadline {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
  white-space: nowrap;
}

.deadline.overdue {
  color: #ef4444;
  font-weight: 600;
}

.person-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  width: 100%;
}

.person-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.person-name {
  font-size: 14px;
  color: #2c3e50;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.assignees-list {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.assignee-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  border: 2px solid white;
  flex-shrink: 0;
}

.more-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #6b7280;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}

.actions {
  justify-content: center;
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.btn-discussions {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  padding: 8px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-discussions:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-discussions:active {
  transform: translateY(0);
}

.discussions-icon {
  flex-shrink: 0;
}

.btn-icon {
  background: none;
  border: 2px solid #e0e0e0;
  color: #2c3e50;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-size: 16px;
  font-weight: bold;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.btn-icon:hover {
  border-color: #667eea;
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}

.dropdown {
  position: relative;
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
}

.dropdown-menu.show {
  display: block;
  animation: fadeIn 0.2s ease;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
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
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.3s ease;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e0e0e0;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error-icon,
.empty-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.error-content h3,
.empty-state h3 {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
}

.error-content p {
  color: #6b7280;
  margin-bottom: 20px;
  max-width: 400px;
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

.btn-retry:active {
  transform: translateY(0);
}

.assignees-names {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #2c3e50;
  line-height: 1.4;
  width: 100%;
}

.assignee-name {
  background: #f8f9fa;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.assignee-name:hover {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.no-assignees {
  color: #9ca3af;
  font-style: italic;
  font-size: 13px;
  white-space: nowrap;
}

.header-cell {
  justify-content: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.cell:nth-child(1) {
}
.cell:nth-child(2) {
}
.cell:nth-child(3) {
}
.cell:nth-child(4) {
}
.cell:nth-child(5) {
}
.cell:nth-child(6) {
}
.cell:nth-child(7) {
}

@media (max-width: 1200px) {
  .list-header,
  .task-row {
    grid-template-columns: minmax(200px, 2fr) 100px 100px 120px 140px 60px;
  }

  .list-header .header-cell:nth-child(5),
  .task-row .cell:nth-child(5) {
    display: none;
  }
}

@media (max-width: 768px) {
  .list-controls {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
    border-radius: 16px;
  }

  .sort-controls {
    justify-content: center;
  }

  .list-header,
  .task-row {
    grid-template-columns: minmax(150px, 1fr) 80px 80px 60px;
    gap: 12px;
    padding: 16px;
  }

  .list-header .header-cell:nth-child(4),
  .list-header .header-cell:nth-child(5),
  .list-header .header-cell:nth-child(6),
  .task-row .cell:nth-child(4),
  .task-row .cell:nth-child(5),
  .task-row .cell:nth-child(6) {
    display: none;
  }

  .task-info .task-meta {
    display: none;
  }

  .status-badge,
  .priority-badge {
    min-width: 70px;
    padding: 6px 8px;
    font-size: 11px;
  }

  .person-avatar {
    width: 28px;
    height: 28px;
    font-size: 11px;
  }

  .person-name {
    font-size: 12px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 6px;
  }
}

@media (max-width: 480px) {
  .list-header,
  .task-row {
    grid-template-columns: 1fr 70px;
    gap: 8px;
    padding: 12px;
  }

  .list-header .header-cell:nth-child(2),
  .list-header .header-cell:nth-child(3),
  .task-row .cell:nth-child(2),
  .task-row .cell:nth-child(3) {
    display: none;
  }

  .task-title {
    font-size: 14px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }

  .tasks-list {
    border-radius: 16px;
  }
}

.task-list-enter-active,
.task-list-leave-active,
.task-list-move {
  transition: all 0.5s ease;
}

.task-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.task-list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.task-list-leave-active {
  position: absolute;
  width: 100%;
}

.list-body {
  position: relative;
}

.task-row {
  transition: all 0.3s ease;
}

.tasks-list {
  overflow: hidden;
}

.list-body {
  min-height: 100px;
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
</style>
