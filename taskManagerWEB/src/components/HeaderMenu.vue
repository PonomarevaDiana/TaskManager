<script>
export default {
  name: 'HeaderMenu',
  props: {
    isAuthenticated: Boolean,
    currentUser: Object,
  },
  data() {
    return {
      activeTab: 'my-space',
    }
  },
  methods: {
    setActiveTab(tab) {
      this.activeTab = tab
      this.$emit('tab-changed', tab)
    },
    getInitials(name) {
      if (!name) return 'U'
      return name
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
    },
  },
  emits: ['tab-changed'],
}
</script>
<template>
  <header class="header-menu">
    <div class="header-menu__container">
      <div class="header-menu__brand">
        <h1 class="header-menu__title">TaskManager</h1>
      </div>

      <nav class="header-menu__nav">
        <ul class="header-menu__list">
          <li class="header-menu__item">
            <button
              @click="setActiveTab('my-space')"
              :class="[
                'header-menu__link',
                { 'header-menu__link--active': activeTab === 'my-space' },
              ]"
            >
              🏠 Мое пространство
            </button>
          </li>
          <li class="header-menu__item">
            <button
              @click="setActiveTab('projects')"
              :class="[
                'header-menu__link',
                { 'header-menu__link--active': activeTab === 'projects' },
              ]"
            >
              📁 Проекты
            </button>
          </li>
        </ul>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.header-menu {
  background: white;
  margin-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.header-menu__container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-menu__title {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 1.3rem;
  font-weight: 700;
  margin: 0;
}

.header-menu__nav {
  flex: 1;
  display: flex;
  justify-content: center;
}

.header-menu__list {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 10px;
}

.header-menu__link {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: #7f8c8d;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.9rem;
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.header-menu__link:not(.header-menu__link--active):hover {
  background: rgba(102, 126, 234, 0.08);
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.header-menu__link--active {
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.header-menu__link--active:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #2e8b5a 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}
</style>
