<script>
import Canban from './Canban.vue'
import Calendar from './Calendar.vue'
import TaskForm from './Task-form.vue'
import List from './List.vue'
import SimpleFilter from './Simple-filter.vue'
import TaskService from '@/services/Task-service'

export default {
  name: 'MySpace',
  components: { Canban, TaskForm, List, Calendar, SimpleFilter },
  props: {
    currentUser: Object,
    isAuthenticated: Boolean,
  },
  data() {
    return {
      showForm: false,
      refreshTaskList: false,
      viewMode: 'kanban',
      currentFilters: {},
      showFilterWarning: false,
      taskType: 'personal',
      showDeleteModal: false,
      taskToDelete: null,
      deleteLoading: false,
    }
  },
  computed: {
    hasActiveFilters() {
      return Object.values(this.currentFilters).some((value) => value !== '')
    },
    taskSource() {
      switch (this.taskType) {
        case 'work':
          return 'work'
        case 'personal':
        default:
          return 'personal'
      }
    },
    sourceId() {
      return this.currentUser.id
    },
  },
  methods: {
    openForm() {
      this.showForm = true
    },
    closeForm() {
      this.showForm = false
    },
    handleTaskCreated(newTask) {
      console.log('Новая задача создана:', newTask)
      this.refreshTaskList = true
      this.closeForm()
      if (this.hasActiveFilters) {
        this.showFilterWarning = true
        setTimeout(() => {
          this.showFilterWarning = false
        }, 5000)
      }
    },
    setViewMode(mode) {
      this.viewMode = mode
    },
    setTaskType(type) {
      this.taskType = type
      this.refreshTaskList = true
      this.clearFilters()
    },

    handleDeleteTask(taskId) {
      this.$emit('delete-task', taskId)
    },

    applyFilter(filters) {
      this.currentFilters = filters
      this.showFilterWarning = false
    },
    clearFilters() {
      this.currentFilters = {}
      this.showFilterWarning = false
    },
    openDiscussions(taskId) {
      this.$emit('open-discussions', taskId)
    }
  },
  emits: ['refreshed', 'delete-task', 'open-discussions'],
}
</script>

<template>
  <div class="my-space">
    <div class="space-header">
      <div class="header-left">
        <div class="task-type-selector">
          <button
            @click="setTaskType('personal')"
            :class="['type-btn', { 'type-btn--active': taskType === 'personal' }]"
          >
            <span class="type-icon">👤</span>
            Личные задачи
          </button>
          <button
            @click="setTaskType('work')"
            :class="['type-btn', { 'type-btn--active': taskType === 'work' }]"
          >
            <span class="type-icon">💼</span>
            Рабочие задачи
          </button>
        </div>
      </div>
      <button @click="openForm" class="create-btn">+ Создать задачу</button>
    </div>

    <TaskForm
      :is-visible="showForm"
      :currentUserId="currentUser?.id"
      @task-created="handleTaskCreated"
      @close="closeForm"
      task-source="personal"
      :source-id="sourceId"
    />

    <SimpleFilter
      :current-filters="currentFilters"
      @filter="applyFilter"
      :task-source="taskSource"
    />

    <div class="view-controls">
      <div class="view-buttons">
        <button
          @click="setViewMode('kanban')"
          :class="['view-btn', { 'view-btn--active': viewMode === 'kanban' }]"
        >
          <span class="view-icon">📊</span>
          Канбан
        </button>
        <button
          @click="setViewMode('list')"
          :class="['view-btn', { 'view-btn--active': viewMode === 'list' }]"
        >
          <span class="view-icon">📋</span>
          Список
        </button>
        <button
          @click="setViewMode('calendar')"
          :class="['view-btn', { 'view-btn--active': viewMode === 'calendar' }]"
        >
          <span class="view-icon">📅</span>
          Календарь
        </button>
      </div>
    </div>

    <div class="tasks-content">
      <div v-if="viewMode === 'list'" class="view-section">
        <List
          :refresh="refreshTaskList"
          @refreshed="refreshTaskList = false"
          :external-filters="currentFilters"
          :task-source="taskSource"
          :source-id="sourceId"
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
          :task-source="taskSource"
          :source-id="sourceId"
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
          :task-source="taskSource"
          :source-id="sourceId"
          @delete-task="handleDeleteTask"
          @open-discussions="openDiscussions"
          :current-user="currentUser"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-space {
  max-width: 100%;
  margin: 0 auto;
}

.space-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.task-type-selector {
  display: flex;
  gap: 8px;
  background: white;
  padding: 6px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.type-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  color: #7f8c8d;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.type-btn:hover {
  background: #f8f9fa;
  color: #2c3e50;
}

.type-btn--active {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.type-icon {
  font-size: 1.1rem;
}

.create-btn {
  background: linear-gradient(to right, #15cd8d, #4ab3ff);
  color: white;
  border: none;
  padding: 14px 28px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 3px 10px rgba(21, 205, 141, 0.3);
  white-space: nowrap;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(21, 205, 141, 0.4);
}

.view-controls {
  margin: 24px 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.task-type-info {
  flex: 1;
}

.task-type-info h2 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 1.5rem;
}

.task-type-description {
  margin: 0;
  color: #7f8c8d;
  font-size: 0.95rem;
}

.view-buttons {
  display: flex;
  gap: 8px;
  background: white;
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: fit-content;
}

.view-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  color: #7f8c8d;
  transition: all 0.3s ease;
}

.view-btn:hover {
  background: #f8f9fa;
  color: #2c3e50;
}

.view-btn--active {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.view-icon {
  font-size: 1.1rem;
}

.tasks-content {
  margin-bottom: 32px;
}

.view-section {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
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

  .stats-cards {
    grid-template-columns: 1fr;
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
  z-index: 1000;
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
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
