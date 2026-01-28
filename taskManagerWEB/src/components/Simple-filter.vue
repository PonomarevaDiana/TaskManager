<template>
  <div class="simple-filter">
    <div class="filter-header">
      <div class="filter-title">
        <svg class="filter-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path
            d="M4 6H20M7 12H17M9 18H15"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
          />
        </svg>
        <span>Фильтры</span>
      </div>
    </div>

    <div class="filter-main">
      <div class="filter-row">
        <div class="filter-group">
          <div class="filter-with-icon">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M21 21L16.514 16.506M19 10.5C19 15.194 15.194 19 10.5 19C5.806 19 2 15.194 2 10.5C2 5.806 5.806 2 10.5 2C15.194 2 19 5.806 19 10.5Z"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
            <input v-model="search" placeholder="Поиск задач..." class="form-input search-input" />
          </div>
        </div>

        <div class="filter-group" v-if="taskSource !== 'personal'">
          <div class="filter-with-icon">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M16 7C16 9.20914 14.2091 11 12 11C9.79086 11 8 9.20914 8 7C8 4.79086 9.79086 3 12 3C14.2091 3 16 4.79086 16 7Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M12 14C8.13401 14 5 17.134 5 21H19C19 17.134 15.866 14 12 14Z"
                stroke="currentColor"
                stroke-width="2"
              />
            </svg>
            <input v-model="creator" placeholder="Создатель" class="form-input filter-input" />
          </div>
        </div>

        <div class="filter-group">
          <div class="filter-with-icon">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
            <select v-model="status" class="form-input filter-select">
              <option value="">Все статусы</option>
              <option value="1">Активная</option>
              <option value="2">Завершена</option>
              <option value="3">В работе</option>
              <option value="4">На проверке</option>
            </select>
          </div>
        </div>

        <div class="filter-group">
          <div class="filter-with-icon">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M2 9C2 10.1046 2.89543 11 4 11C5.10457 11 6 10.1046 6 9C6 7.89543 5.10457 7 4 7C2.89543 7 2 7.89543 2 9Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M10 9C10 10.1046 10.8954 11 12 11C13.1046 11 14 10.1046 14 9C14 7.89543 13.1046 7 12 7C10.8954 7 10 7.89543 10 9Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M18 9C18 10.1046 18.8954 11 20 11C21.1046 11 22 10.1046 22 9C22 7.89543 21.1046 7 20 7C18.8954 7 18 7.89543 18 9Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M2 15C2 16.1046 2.89543 17 4 17C5.10457 17 6 16.1046 6 15C6 13.8954 5.10457 13 4 13C2.89543 13 2 13.8954 2 15Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M10 15C10 16.1046 10.8954 17 12 17C13.1046 17 14 16.1046 14 15C14 13.8954 13.1046 13 12 13C10.8954 13 10 13.8954 10 15Z"
                stroke="currentColor"
                stroke-width="2"
              />
            </svg>
            <select v-model="priority" class="form-input filter-select">
              <option value="">Все приоритеты</option>
              <option value="1">Высокий</option>
              <option value="2">Средний</option>
              <option value="3">Низкий</option>
            </select>
          </div>
        </div>

        <div class="filter-group">
          <div class="filter-with-icon">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M8 7V3M16 7V3M7 11H17M5 21H19C20.1046 21 21 20.1046 21 19V7C21 5.89543 20.1046 5 19 5H5C3.89543 5 3 5.89543 3 7V19C3 20.1046 3.89543 21 5 21Z"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
            <select v-model="createDate" class="form-input filter-select">
              <option value="">Дата создания</option>
              <option value="today">Сегодня</option>
              <option value="yesterday">Вчера</option>
              <option value="thisWeek">Эта неделя</option>
              <option value="lastWeek">Прошлая неделя</option>
            </select>
          </div>
        </div>

        <div class="filter-group">
          <div class="filter-with-icon">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M12 8V12L15 15M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
            <select v-model="deadlineDate" class="form-input filter-select">
              <option value="">Дедлайн</option>
              <option value="today">Сегодня</option>
              <option value="yesterday">Вчера</option>
              <option value="tomorrow">Завтра</option>
              <option value="thisWeek">На этой неделе</option>
              <option value="nextWeek">На следующей неделе</option>
              <option value="thisMonth">В этом месяце</option>
              <option value="nextMonth">В следующем месяце</option>
              <option value="overdue">Просроченные</option>
            </select>
          </div>
        </div>

        <div class="filter-actions">
          <button @click="resetFilters" class="cancel-btn" title="Сбросить фильтры">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M4 4V9H4.58152M19.9381 11C19.446 7.05369 16.0796 4 12 4C8.64262 4 5.76829 6.06817 4.58152 9M4.58152 9H9M20 20V15H19.4185M19.4185 15C18.2317 17.9318 15.3574 20 12 20C7.92038 20 4.55399 16.9463 4.06189 13M19.4185 15H15"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <span class="btn-text">Сбросить</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SimpleFilter',
  data() {
    return {
      search: '',
      creator: '',
      status: '',
      priority: '',
      createDate: '',
      deadlineDate: '',
    }
  },
  watch: {
    search: 'emitFilters',
    creator: 'emitFilters',
    status: 'emitFilters',
    priority: 'emitFilters',
    createDate: 'emitFilters',
    deadlineDate: 'emitFilters',
  },

  props: {
    taskSource: {
      type: String,
      default: 'personal',
      validator: (value) => ['personal', 'project', 'work'].includes(value),
    },
  },
  methods: {
    emitFilters() {
      this.$emit('filter', {
        search: this.search,
        creator: this.creator,
        status: this.status,
        priority: this.priority,
        createDate: this.createDate,
        deadlineDate: this.deadlineDate,
      })
    },
    applyFilters() {},
    resetFilters() {
      this.search = ''
      this.creator = ''
      this.status = ''
      this.priority = ''
      this.createDate = ''
      this.deadlineDate = ''
    },
  },
}
</script>

<style scoped>
.simple-filter {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  border: 1px solid #e0e0e0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.filter-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.filter-icon {
  color: #667eea;
}

.filter-main {
  width: 100%;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-group {
  flex: 1;
  min-width: 150px;
}

.filter-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  color: #666;
  z-index: 2;
  transition: color 0.3s ease;
}

.form-input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: all 0.3s ease;
  color: #2c3e50;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.search-input::placeholder,
.filter-input::placeholder {
  color: #999;
}

.filter-select {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 14px;
  padding-right: 36px;
  cursor: pointer;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.cancel-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  background: white;
  color: #666;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 100px;
  height: 40px;
}

.cancel-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.btn-text {
  font-size: 14px;
}

.filter-with-icon:hover .input-icon {
  color: #667eea;
}

@media (max-width: 768px) {
  .simple-filter {
    padding: 12px;
  }

  .filter-row {
    gap: 8px;
  }

  .filter-group {
    min-width: 100%;
  }

  .filter-actions {
    width: 100%;
    margin-left: 0;
    margin-top: 8px;
  }

  .cancel-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
