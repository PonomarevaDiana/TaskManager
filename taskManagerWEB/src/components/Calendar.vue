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
  emits:['open-discussions'],
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
      selectedDay: null,
      selectedTask: null,

      currentDate: new Date(),
      viewMode: 'month',
      weekDays: ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'],
      monthNames: [
        'Январь',
        'Февраль',
        'Март',
        'Апрель',
        'Май',
        'Июнь',
        'Июль',
        'Август',
        'Сентябрь',
        'Октябрь',
        'Ноябрь',
        'Декабрь',
      ],
      shortMonthNames: [
        'Янв',
        'Фев',
        'Мар',
        'Апр',
        'Май',
        'Июн',
        'Июл',
        'Авг',
        'Сен',
        'Окт',
        'Ноя',
        'Дек',
      ],
    }
  },

  computed: {
    currentPeriodLabel() {
      if (this.viewMode === 'month') {
        return `${this.monthNames[this.currentDate.getMonth()]} ${this.currentDate.getFullYear()}`
      } else {
        const start = this.currentWeekDays[0]
        const end = this.currentWeekDays[6]

        if (start.date.getMonth() === end.date.getMonth()) {
          return `${start.date.getDate()} - ${end.date.getDate()} ${this.shortMonthNames[start.date.getMonth()]} ${this.currentDate.getFullYear()}`
        } else {
          return `${start.date.getDate()} ${this.shortMonthNames[start.date.getMonth()]} - ${end.date.getDate()} ${this.shortMonthNames[end.date.getMonth()]} ${this.currentDate.getFullYear()}`
        }
      }
    },

    calendarDays() {
      const year = this.currentDate.getFullYear()
      const month = this.currentDate.getMonth()

      const firstDay = new Date(year, month, 1)
      const lastDay = new Date(year, month + 1, 0)
      const firstDayOfWeek = firstDay.getDay()
      const firstDayOffset = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1

      const days = []

      const prevMonthLastDay = new Date(year, month, 0).getDate()
      for (let i = firstDayOffset - 1; i >= 0; i--) {
        const date = new Date(year, month - 1, prevMonthLastDay - i)
        days.push({
          date,
          isCurrentMonth: false,
          isToday: this.isToday(date),
          isWeekend: date.getDay() === 0 || date.getDay() === 6,
          tasks: this.getTasksForDate(date),
        })
      }

      for (let i = 1; i <= lastDay.getDate(); i++) {
        const date = new Date(year, month, i)
        days.push({
          date,
          isCurrentMonth: true,
          isToday: this.isToday(date),
          isWeekend: date.getDay() === 0 || date.getDay() === 6,
          tasks: this.getTasksForDate(date),
        })
      }

      const daysNeeded = 42 - days.length
      for (let i = 1; i <= daysNeeded; i++) {
        const date = new Date(year, month + 1, i)
        days.push({
          date,
          isCurrentMonth: false,
          isToday: this.isToday(date),
          isWeekend: date.getDay() === 0 || date.getDay() === 6,
          tasks: this.getTasksForDate(date),
        })
      }

      return days
    },

    currentWeekDays() {
      const today = new Date(this.currentDate)
      const currentDayOfWeek = today.getDay()
      const monday = new Date(today)
      monday.setDate(today.getDate() - (currentDayOfWeek === 0 ? 6 : currentDayOfWeek - 1))

      const weekDays = []
      for (let i = 0; i < 7; i++) {
        const date = new Date(monday)
        date.setDate(monday.getDate() + i)

        weekDays.push({
          date,
          dayName: this.weekDays[i],
          monthName: this.shortMonthNames[date.getMonth()],
          isToday: this.isToday(date),
          isWeekend: i >= 5,
          tasks: this.getTasksForDate(date),
        })
      }
      return weekDays
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
    showTaskDetails(task) {
      this.selectedTask = task
    },

    editSelectedTask() {
      this.editingTask = this.selectedTask
      this.selectedTask = null
    },

    async deleteSelectedTask() {
      this.$emit('delete-task', this.selectedTask.id)
      this.toggleDropdown(this.selectedTask.id)
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

    isOverdue(deadlineDate) {
      if (!deadlineDate) return false
      return new Date(deadlineDate) < new Date()
    },

    handleDayTaskClick(task) {
      this.selectedTask = task
      this.selectedDay = null
    },
    handleTaskClick(task) {
      this.showTaskDetails(task)
    },

    getStatusColor(status) {
      if (!status) return '#6b7280'
      let statusName = ''
      if (typeof status === 'string') statusName = status.toLowerCase()
      else if (status.name) statusName = status.name.toLowerCase()
      else if (status.id) statusName = this.getStatusNameById(status.id)

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
        return date.toLocaleDateString('ru-RU', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
        })
      } catch (e) {
        return ''
      }
    },

    previousPeriod() {
      if (this.viewMode === 'month') {
        this.currentDate = new Date(
          this.currentDate.getFullYear(),
          this.currentDate.getMonth() - 1,
          1,
        )
      } else {
        this.currentDate = new Date(this.currentDate)
        this.currentDate.setDate(this.currentDate.getDate() - 7)
      }
    },

    nextPeriod() {
      if (this.viewMode === 'month') {
        this.currentDate = new Date(
          this.currentDate.getFullYear(),
          this.currentDate.getMonth() + 1,
          1,
        )
      } else {
        this.currentDate = new Date(this.currentDate)
        this.currentDate.setDate(this.currentDate.getDate() + 7)
      }
    },

    isToday(date) {
      const today = new Date()
      return date.toDateString() === today.toDateString()
    },

    getTasksForDate(date) {
      return this.filteredTasks.filter((task) => {
        if (!task.deadlineDate && !task.startDate) return false

        const taskDate = task.deadlineDate || task.startDate
        if (!taskDate) return false

        const taskDateObj = new Date(taskDate)

        return taskDateObj.toDateString() === date.toDateString()
      })
    },

    formatTime(dateString) {
      if (!dateString) return ''
      try {
        const date = new Date(dateString)
        return date.toLocaleTimeString('ru-RU', {
          hour: '2-digit',
          minute: '2-digit',
        })
      } catch (e) {
        return ''
      }
    },

    formatFullDate(date) {
      return date.toLocaleDateString('ru-RU', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })
    },

    showDayDetails(day) {
      this.selectedDay = day
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

    applyFilter(filters) {
      this.currentFilters = filters
      this.filteredTasks = this.applyCustomFilter(this.tasks).filter((task) => {
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

    getPriorityDisplayName(priority) {
      if (!priority) return 'Без приоритета'
      if (typeof priority === 'string') return priority
      if (priority.name) return priority.name
      if (priority.id) return this.getPriorityNameById(priority.id)
      return 'Без приоритета'
    },

    getPriorityNameById(priorityId) {
      const priorityMap = { 1: 'высокий', 2: 'средний', 3: 'низкий' }
      return priorityMap[priorityId] || 'Без приоритета'
    },

    getStatusDisplayName(status) {
      if (!status) return 'Не указан'
      let statusName = ''
      if (typeof status === 'string') statusName = status
      else if (status.name) statusName = status.name
      else if (status.id) statusName = this.getStatusNameById(status.id)

      const statusMap = {
        активная: 'Активная',
        'в работе': 'В работе',
        'на проверке': 'На проверке',
        завершена: 'Завершена',
      }
      return statusMap[statusName?.toLowerCase()] || statusName || 'Не указан'
    },

    getStatusNameById(statusId) {
      const statusMap = { 1: 'активная', 2: 'завершена', 3: 'в работе', 4: 'на проверке' }
      return statusMap[statusId] || 'активная'
    },

    getPriorityColor(priority) {
      if (!priority) return '#6b7280'
      let priorityName = ''
      if (typeof priority === 'string') priorityName = priority.toLowerCase()
      else if (priority.name) priorityName = priority.name.toLowerCase()
      else if (priority.id) priorityName = this.getPriorityNameById(priority.id)

      const colors = { высокий: '#ef4444', средний: '#f59e0b', низкий: '#10b981' }
      return colors[priorityName] || '#6b7280'
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

    editTask(task) {
      this.editingTask = task
    },

    async deleteTask(taskId) {
      try {
        if (!confirm('Вы уверены, что хотите удалить эту задачу?')) {
          return
        }
        const response = await TaskService.deleteTask(taskId)
        if (response.status === 204 || response.status === 200) {
          this.tasks = this.tasks.filter((task) => task.id !== taskId)
          alert('Задача успешно удалена!')
          await this.fetchTasks()
        } else {
          alert('Произошла ошибка при удалении задачи.')
        }
      } catch (error) {
        alert('Произошла ошибка при удалении задачи.')
      } finally {
        this.tasks = this.tasks.map((task) => {
          task.showDropdown = false
          return task
        })
      }
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
        default:
          return true
      }
    },

    openDiscussions(taskId) {
      console.log('Открытие обсуждений для задачи ID:', taskId)
      this.$emit('open-discussions', taskId)
    },
  },
}
</script>

<template>
  <div class="task-calendar-view">
    <div class="calendar-controls">
      <div class="calendar-navigation">
        <button @click="previousPeriod" class="nav-btn">‹</button>
        <h2 class="current-period">{{ currentPeriodLabel }}</h2>
        <button @click="nextPeriod" class="nav-btn">›</button>
      </div>

      <div class="view-options">
        <button @click="viewMode = 'month'" :class="['view-btn', { active: viewMode === 'month' }]">
          Месяц
        </button>
        <button @click="viewMode = 'week'" :class="['view-btn', { active: viewMode === 'week' }]">
          Неделя
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

    <div v-else-if="filteredTasks.length === 0" class="empty-state">
      <div class="empty-icon">📅</div>
      <h3>Задачи не найдены</h3>
    </div>

    <div v-else-if="viewMode === 'month'" class="month-calendar">
      <div class="calendar-header">
        <div class="day-header" v-for="day in weekDays" :key="day">
          {{ day }}
        </div>
      </div>

      <transition-group name="calendar-grid" tag="div" class="calendar-grid">
        <div
          v-for="day in calendarDays"
          :key="day.date.toString()"
          :class="[
            'calendar-day',
            {
              'other-month': !day.isCurrentMonth,
              today: day.isToday,
              weekend: day.isWeekend,
              'has-tasks': day.tasks.length > 0,
            },
          ]"
        >
          <div class="day-header">
            <span class="day-number">{{ day.date.getDate() }}</span>
            <span class="tasks-count" v-if="day.tasks.length > 0">
              {{ day.tasks.length }}
            </span>
          </div>

          <transition-group name="day-tasks" tag="div" class="day-tasks">
            <div
              v-for="task in day.tasks.slice(0, 3)"
              :key="task.id"
              class="calendar-task"
              :style="{ borderLeftColor: getPriorityColor(task.priority) }"
              @click="handleTaskClick(task)"
            >
              <div class="task-main">
                <div class="task-title">{{ task.title }}</div>
              </div>
            </div>

            <div
              v-if="day.tasks.length > 3"
              key="more-tasks"
              class="more-tasks"
              @click="showDayDetails(day)"
            >
              +{{ day.tasks.length - 3 }} ещё
            </div>
          </transition-group>
        </div>
      </transition-group>
    </div>

    <div v-else class="week-calendar">
      <div class="week-header">
        <div class="day-column" v-for="day in currentWeekDays" :key="day.date.toString()">
          <div :class="['day-header', { today: day.isToday, weekend: day.isWeekend }]">
            <div class="day-name">{{ day.dayName }}</div>
            <div class="date-number">{{ day.date.getDate() }}</div>
            <div class="month-name">{{ day.monthName }}</div>
          </div>
        </div>
      </div>
      <transition name="modal">
        <div v-if="selectedDay" class="day-modal" @click.self="selectedDay = null">
          <div class="modal-content">
            <div class="modal-header">
              <h3>Задачи на {{ formatFullDate(selectedDay.date) }}</h3>
              <button @click="selectedDay = null" class="close-btn">×</button>
            </div>

            <transition-group name="modal-tasks" tag="div" class="day-tasks-list">
              <div
                v-for="task in selectedDay.tasks"
                :key="task.id"
                class="day-task-item"
                @click="handleDayTaskClick(task)"
              ></div>
            </transition-group>
          </div>
        </div>
      </transition>

      <transition name="modal">
        <div v-if="selectedTask" class="task-modal" @click.self="selectedTask = null">
          <div class="modal-content task-card-modal"></div>
        </div>
      </transition>

      <transition-group name="week-content" tag="div" class="week-content">
        <div
          class="day-column"
          v-for="day in currentWeekDays"
          :key="day.date.toString() + 'content'"
        >
          <div :class="['day-content', { today: day.isToday, weekend: day.isWeekend }]">
            <transition-group name="week-tasks" tag="div">
              <div
                v-for="task in day.tasks"
                :key="task.id"
                class="calendar-task"
                :style="{ borderLeftColor: getPriorityColor(task.priority) }"
                @click="handleTaskClick(task)"
              >
                <div class="task-main">
                  <div class="task-title">{{ task.title }}</div>
                </div>

                <div class="task-meta">
                  <span
                    class="priority-indicator"
                    :style="{ backgroundColor: getPriorityColor(task.priority) }"
                  ></span>
                  <span class="task-status">{{ getStatusDisplayName(task.status) }}</span>
                </div>

                <div class="task-assignees">
                  <span
                    v-for="assignee in task.assignees"
                    :key="assignee.id"
                    class="assignee-avatar"
                    :title="getPersonName(assignee)"
                  >
                    {{ getPersonInitial(assignee) }}
                  </span>
                </div>
              </div>
            </transition-group>

            <div v-if="day.tasks.length === 0" class="no-tasks">Нет задач</div>
          </div>
        </div>
      </transition-group>
    </div>

    <div v-if="selectedDay" class="day-modal" @click.self="selectedDay = null">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Задачи на {{ formatFullDate(selectedDay.date) }}</h3>
          <button @click="selectedDay = null" class="close-btn">×</button>
        </div>

        <div class="day-tasks-list">
          <div
            v-for="task in selectedDay.tasks"
            :key="task.id"
            class="day-task-item"
            @click="handleDayTaskClick(task)"
          >
            <div class="task-main-info">
              <div class="task-title-row">
                <h4>{{ task.title }}</h4>
                <span
                  class="priority-badge"
                  :style="{ backgroundColor: getPriorityColor(task.priority) }"
                >
                  {{ getPriorityDisplayName(task.priority) }}
                </span>
              </div>

              <div class="task-meta">
                <span class="task-status">
                  {{ getStatusDisplayName(task.status) }}
                </span>
              </div>
            </div>

            <div class="task-actions">
              <button
                class="btn-discussions"
                @click.stop="openDiscussions(task.id)"
                :title="`Открыть обсуждения по задаче: ${task.title}`"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="discussions-icon">
                  <path
                    d="M20 2H4C2.9 2 2 2.9 2 4V22L6 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2Z"
                    fill="currentColor"
                  />
                </svg>
              </button>
              <button @click.stop="editTask(task)" class="action-btn">✏️</button>
              <button @click.stop="deleteTask(task.id)" class="action-btn delete">🗑️</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="selectedTask" class="task-modal" @click.self="selectedTask = null">
      <div class="modal-content task-card-modal">
        <div class="modal-header">
          <h3>Карточка задачи</h3>
          <button @click="selectedTask = null" class="close-btn">×</button>
        </div>

        <div class="task-card-content">
          <div class="task-card-header">
            <h2 class="task-card-title">{{ selectedTask.title }}</h2>
            <span
              class="priority-badge large"
              :style="{ backgroundColor: getPriorityColor(selectedTask.priority) }"
            >
              {{ getPriorityDisplayName(selectedTask.priority) }}
            </span>
          </div>
          <div class="task-card-section">
            <label>Статус:</label>
            <span
              class="status-badge"
              :style="{ backgroundColor: getStatusColor(selectedTask.status) }"
            >
              {{ getStatusDisplayName(selectedTask.status) }}
            </span>
          </div>

          <div class="task-card-dates">
            <div class="date-item" v-if="selectedTask.createDate">
              <span class="date-label">Создана:</span>
              <span class="date-value">{{ formatDate(selectedTask.createDate) }}</span>
            </div>
            <div class="date-item" v-if="selectedTask.startDate">
              <span class="date-label">Начало:</span>
              <span class="date-value">{{ formatDate(selectedTask.startDate) }}</span>
            </div>
            <div class="date-item" v-if="selectedTask.deadlineDate">
              <span class="date-label">Дедлайн:</span>
              <span class="date-value" :class="{ overdue: isOverdue(selectedTask.deadlineDate) }">
                {{ formatDate(selectedTask.deadlineDate) }}
                <span v-if="isOverdue(selectedTask.deadlineDate)" class="overdue-badge"
                  >Просрочена</span
                >
              </span>
            </div>
          </div>

          <div class="task-card-section" v-if="selectedTask.creator">
            <label>Создатель:</label>
            <div class="person-info">
              <div class="person-avatar medium">
                {{ getPersonInitial(selectedTask.creator) }}
              </div>
              <span class="person-name">{{ getPersonName(selectedTask.creator) }}</span>
            </div>
          </div>

          <div
            class="task-card-section"
            v-if="selectedTask.assignees && selectedTask.assignees.length > 0"
          >
            <label>Исполнители:</label>
            <div class="assignees-list">
              <div
                v-for="assignee in selectedTask.assignees"
                :key="assignee.id"
                class="assignee-item"
              >
                <div class="person-avatar small">
                  {{ getPersonInitial(assignee) }}
                </div>
                <span class="person-name">{{ getPersonName(assignee) }}</span>
              </div>
            </div>
          </div>

          <div class="task-card-actions">
            <button
              @click="openDiscussions(selectedTask.id)"
              class="btn-discussions-card"
              :title="`Открыть обсуждения по задаче: ${selectedTask.title}`"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="discussions-icon">
                <path
                  d="M20 2H4C2.9 2 2 2.9 2 4V22L6 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2Z"
                  fill="currentColor"
                />
              </svg>
              <span>Обсуждения</span>
            </button>
            <button @click="editSelectedTask" class="btn-primary">✏️ Редактировать</button>
            <button
              v-if="isTaskCreator(selectedTask)"
              @click="deleteSelectedTask"
              class="btn-danger"
            >
              🗑️ Удалить
            </button>
          </div>
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
.task-calendar-view {
  position: relative;
  min-height: 50vh;
  font-family: Arial, sans-serif;
  background: #f8fafc;
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

.calendar-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.3s ease;
}

.calendar-navigation {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-btn {
  padding: 12px 20px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 18px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.nav-btn:active {
  transform: translateY(0);
}

.current-period {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  min-width: 300px;
  text-align: center;
}

.view-options {
  display: flex;
  gap: 8px;
  background: #f8f9fa;
  padding: 6px;
  border-radius: 12px;
}

.view-btn {
  padding: 12px 24px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  transition: all 0.3s ease;
}

.view-btn:hover {
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.view-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 20px;
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
  font-size: 20px;
  font-weight: 600;
}

.error-content p {
  color: #6b7280;
  margin-bottom: 24px;
  max-width: 400px;
  line-height: 1.5;
}

.btn-retry {
  padding: 14px 28px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 12px;
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

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-state h3 {
  color: #2c3e50;
  margin-bottom: 12px;
  font-size: 20px;
  font-weight: 600;
}

.month-calendar {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

.calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
}

.calendar-header .day-header {
  padding: 20px;
  text-align: center;
  font-weight: 600;
  font-size: 14px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
  background: #e0e0e0;
}

.calendar-day {
  background: white;
  min-height: 140px;
  padding: 16px;
  position: relative;
  transition: all 0.3s ease;
  overflow: hidden;
}

.calendar-day:hover {
  background: #f8f9fa;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  z-index: 1;
}

.calendar-day.other-month {
  background: #f8f9fa;
  color: #9ca3af;
}

.calendar-day.today {
  background: #fff7ed;
}

.calendar-day.today .day-number {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
}

.calendar-day.weekend {
  background: #fef2f2;
}

.calendar-day.has-tasks {
  background: #f0fdf4;
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.day-number {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  transition: all 0.3s ease;
}

.calendar-day.other-month .day-number {
  color: #9ca3af;
}

.tasks-count {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border-radius: 12px;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 600;
  min-width: 24px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.day-tasks {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 100px;
  overflow-y: auto;
}

.day-tasks::-webkit-scrollbar {
  width: 4px;
}

.day-tasks::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.day-tasks::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.calendar-task {
  background: white;
  border-left: 4px solid;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  text-align: left;
  min-height: 28px;
  display: flex;
  align-items: flex-start;
  overflow: hidden;
  border: 1px solid #e0e0e0;
}

.calendar-task.overdue {
  background: #fef2f2;
  border-left-color: #ef4444 !important;
  position: relative;
}

.calendar-task.overdue::before {
  content: '!';
  position: absolute;
  left: -10px;
  top: -8px;
  font-size: 12px;
  background: #ef4444;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.calendar-task:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.task-main {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  text-align: left;
  width: 100%;
  min-width: 0;
}

.task-title {
  font-size: 12px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
  width: 100%;
  word-break: break-word;
}

.more-tasks {
  font-size: 11px;
  color: #667eea;
  cursor: pointer;
  padding: 6px 8px;
  text-align: center;
  border-radius: 6px;
  transition: all 0.3s ease;
  background: rgba(102, 126, 234, 0.1);
  margin-top: 4px;
  font-weight: 600;
}

.more-tasks:hover {
  background: rgba(102, 126, 234, 0.2);
  color: #369b70;
}

.week-calendar {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
}

.day-column {
  border-right: 1px solid rgba(255, 255, 255, 0.2);
}

.day-column:last-child {
  border-right: none;
}

.week-header .day-header {
  padding: 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.week-header .day-header.today {
  background: rgba(255, 255, 255, 0.1);
}

.day-name {
  font-size: 14px;
  font-weight: 600;
  opacity: 0.9;
}

.date-number {
  font-size: 24px;
  font-weight: 700;
}

.month-name {
  font-size: 12px;
  opacity: 0.9;
}

.week-content {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  min-height: 500px;
}

.day-content {
  padding: 16px;
  min-height: 500px;
  background: white;
  border-right: 1px solid #e0e0e0;
  border-bottom: 1px solid #e0e0e0;
  overflow: hidden;
}

.day-content.today {
  background: #fff7ed;
}

.day-content.weekend {
  background: #fef2f2;
}

.week-task {
  background: white;
  border-left: 4px solid;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  margin-bottom: 8px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
}

.week-task.overdue {
  background: #fef2f2;
  border-left-color: #ef4444 !important;
}

.week-task:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.week-task-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.week-task-title {
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
}

.week-task.overdue .week-task-title {
  color: #ef4444;
  font-weight: 700;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  text-align: left;
  flex-wrap: wrap;
}

.priority-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.task-status {
  font-size: 11px;
  color: #6b7280;
  font-weight: 600;
  white-space: nowrap;
}

.overdue-status {
  color: #ef4444 !important;
  font-weight: 700;
}

.task-assignees {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  flex-wrap: wrap;
  text-align: left;
}

.assignee-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.no-tasks {
  color: #9ca3af;
  font-size: 14px;
  text-align: center;
  margin-top: 40px;
  font-style: italic;
}

.day-modal,
.task-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-width: 600px;
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

.task-card-modal {
  max-width: 500px;
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
}

.modal-header h3 {
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

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.day-tasks-list {
  padding: 24px;
}

.day-task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  margin-bottom: 12px;
  transition: all 0.3s ease;
  text-align: left;
  overflow: hidden;
  background: white;
}

.day-task-item.overdue {
  border-color: #fecaca;
  background: #fef2f2;
  border-left: 4px solid #ef4444;
}

.day-task-item:hover {
  border-color: #667eea;
  box-shadow: 0 6px 12px rgba(102, 126, 234, 0.15);
  transform: translateY(-2px);
}

.task-main-info {
  flex: 1;
  text-align: left;
  min-width: 0;
  margin-right: 16px;
}

.task-title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
  text-align: left;
}

.task-title-row h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 15px;
  font-weight: 600;
  flex: 1;
  text-align: left;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.day-task-item.overdue .task-title-row h4 {
  color: #ef4444;
}

.priority-badge {
  padding: 6px 12px;
  border-radius: 8px;
  color: white;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.priority-badge.large {
  padding: 8px 16px;
  font-size: 14px;
}

.overdue-alert {
  background: #ef4444;
  color: white;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.task-time {
  font-size: 12px;
  color: #6b7280;
}

.task-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
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
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  flex-shrink: 0;
}

.btn-discussions:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.discussions-icon {
  flex-shrink: 0;
}

.action-btn {
  background: none;
  border: 2px solid #e0e0e0;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}

.action-btn.delete:hover {
  border-color: #ef4444;
  background: #fee2e2;
  color: #ef4444;
}

.task-card-content {
  padding: 24px;
  text-align: left;
}

.task-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
  text-align: left;
}

.task-card-title {
  margin: 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
  flex: 1;
  text-align: left;
  word-break: break-word;
}

.task-card-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  text-align: left;
  flex-wrap: wrap;
}

.task-card-section label {
  font-weight: 600;
  color: #2c3e50;
  min-width: 100px;
  text-align: left;
  font-size: 14px;
}

.status-badge {
  padding: 8px 16px;
  border-radius: 8px;
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.task-card-dates {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.date-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  text-align: left;
  flex-wrap: wrap;
  gap: 12px;
}

.date-label {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.date-value {
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  text-align: right;
}

.date-value.overdue {
  color: #ef4444;
  font-weight: 600;
}

.overdue-badge {
  background: #ef4444;
  color: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.person-info {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
}

.person-avatar {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.person-avatar.medium {
  width: 36px;
  height: 36px;
  font-size: 14px;
}

.person-avatar.small {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.person-name {
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
}

.assignees-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}

.assignee-item {
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.task-card-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
  text-align: left;
  flex-wrap: wrap;
}

.btn-discussions-card {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  border: none;
  border-radius: 12px;
  color: white;
  cursor: pointer;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  font-size: 14px;
  font-weight: 600;
  flex: 1;
  min-width: 160px;
  justify-content: center;
}

.btn-discussions-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.btn-primary {
  padding: 14px 20px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  flex: 1;
  min-width: 160px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.btn-danger {
  padding: 14px 20px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
  flex: 1;
  min-width: 160px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.btn-danger:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
  background: #dc2626;
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

.calendar-grid-enter-active,
.calendar-grid-leave-active,
.calendar-grid-move {
  transition: all 0.5s ease;
}

.calendar-grid-enter-from {
  opacity: 0;
  transform: scale(0.8);
}

.calendar-grid-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.calendar-grid-leave-active {
  position: absolute;
}

.day-tasks-enter-active,
.day-tasks-leave-active {
  transition: all 0.3s ease;
}

.day-tasks-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.day-tasks-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.day-tasks-leave-active {
  position: absolute;
  width: 100%;
}

.week-content-enter-active,
.week-content-leave-active {
  transition: all 0.4s ease;
}

.week-content-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.week-content-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.week-tasks-enter-active,
.week-tasks-leave-active {
  transition: all 0.3s ease;
}

.week-tasks-enter-from {
  opacity: 0;
  transform: scale(0.9);
}

.week-tasks-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.week-tasks-leave-active {
  position: absolute;
  width: calc(100% - 20px);
  margin: 0 10px;
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from {
  opacity: 0;
}

.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  transform: scale(0.9);
}

.modal-enter-active .modal-content,
.modal-leave-active .modal-content {
  transition: all 0.3s ease;
}

.modal-tasks-enter-active,
.modal-tasks-leave-active {
  transition: all 0.3s ease;
}

.modal-tasks-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.modal-tasks-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.modal-tasks-leave-active {
  position: absolute;
  width: 100%;
}

.calendar-grid,
.day-tasks,
.week-content {
  position: relative;
}

.day-tasks {
  min-height: 20px;
}

.calendar-day {
  transition: all 0.2s ease;
}

.calendar-task {
  transition: all 0.2s ease;
}

.calendar-task:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

@media (max-width: 768px) {
  .task-calendar-view {
    padding: 10px;
  }

  .calendar-controls {
    flex-direction: column;
    gap: 16px;
    text-align: center;
    padding: 16px;
  }

  .current-period {
    min-width: auto;
    font-size: 18px;
  }

  .calendar-day {
    min-height: 100px;
    padding: 12px;
  }

  .day-tasks {
    max-height: 80px;
  }

  .week-content {
    min-height: 400px;
  }

  .day-content {
    min-height: 400px;
    padding: 12px;
  }

  .modal-content {
    margin: 10px;
    max-height: calc(100vh - 20px);
  }

  .task-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .task-card-actions {
    flex-direction: column;
  }

  .btn-primary,
  .btn-danger,
  .btn-discussions-card {
    min-width: 100%;
  }

  .calendar-header .day-header {
    padding: 12px;
    font-size: 12px;
  }

  .week-header .day-header {
    padding: 12px;
  }

  .date-number {
    font-size: 18px;
  }
}
</style>
