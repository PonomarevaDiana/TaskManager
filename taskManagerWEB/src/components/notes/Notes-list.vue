<script>
import NoteService from '@/services/Note-service.js'
import Note from './Note-item.vue'

export default {
  emits: ['showPushMessage', 'close'],
  name: 'NoteList',
  components: {
    Note
  },
  props: {
    userId: {
      type: String,
      required: true
    },
    objectId: {
      type: String,
      required: true
    },
    isProject: {
      type: Boolean,
      required: true
    }
  },
  data() {
    return {
      newNoteTitle: '',
      newNoteContent: '',
      useMarkdown: false,
      showCreateNote: false,
      notes: [],
      isLoading: false
    }
  },
  computed: {
    sortedNotes() {
      return [...this.notes].reverse();
    }
  },
  methods: {
    closeNoteList() {
      this.$emit('close');
    },

    async createNote() {
      if (!this.newNoteTitle.trim() || !this.newNoteContent.trim()) {
        this.$emit('showPushMessage', 'Заполните заголовок и содержание заметки', 'OK', 3000);
        return;
      }

      this.isLoading = true;

      try {
        const noteData = {
          title: this.newNoteTitle,
          content: this.newNoteContent,
          userId: this.userId,
          useMarkdown: this.useMarkdown,
          isProject: this.isProject
        };

        const response = await NoteService.createObjectsNote(this.objectId, noteData);

        if (response.status === 200) {
          this.$emit('showPushMessage', 'Заметка создана', 'OK', 3000);
          await this.loadNotes();
          this.newNoteTitle = '';
          this.newNoteContent = '';
          this.useMarkdown = false;
        }
      } catch (error) {
        console.error('Ошибка при создании заметки:', error);
        this.$emit('showPushMessage', 'Ошибка при создании заметки', 'OK', 5000);
      } finally {
        this.isLoading = false;
      }
    },

    toggleCreateNote() {
      this.showCreateNote = !this.showCreateNote;
    },

    async loadNotes() {
      this.isLoading = true;
      try {
        const response = await NoteService.getObjectsNotes(this.objectId, this.isProject);
        this.notes = response.data;
      } catch (error) {
        console.error('Ошибка при загрузке заметок:', error);
        this.$emit('showPushMessage', 'Ошибка загрузки заметок', 'OK', 5000);
        this.notes = [];
      } finally {
        this.isLoading = false;
      }
    },

    async refreshNotes() {
      await this.loadNotes();
      this.$emit('showPushMessage', 'Заметки обновлены', 'OK', 2000);
    }
  },

  async mounted() {
    document.body.style.overflow = 'hidden';
    await this.loadNotes();
  },

  beforeUnmount() {
    document.body.style.overflow = '';
  }
}
</script>

<template>
  <div class="note-list-overlay" @click.self="closeNoteList">
    <div class="note-list-modal">
      <div class="note-list-header">
        <h2>Обсуждения</h2>
        <button class="close-btn" @click="closeNoteList" :disabled="isLoading">×</button>
      </div>

      <div class="note-list-content">
        <div class="create-note-section">
          <div class="create-note-header" @click="toggleCreateNote">
            <span class="create-note-title">Создать новую запись</span>
            <span class="toggle-icon">{{ showCreateNote ? '▼' : '▶' }}</span>
          </div>

          <div v-if="showCreateNote" class="create-note-content">
            <div class="create-note-input-wrapper">
              <input
                v-model="newNoteTitle"
                type="text"
                placeholder="Заголовок заметки..."
                class="create-note-input"
                :disabled="isLoading"
              />
              <button
                class="create-note-btn"
                @click="createNote"
                :disabled="isLoading || !newNoteTitle.trim() || !newNoteContent.trim()"
              >
                {{ isLoading ? 'Создание...' : 'Создать' }}
              </button>
            </div>

            <div class="markdown-toggle">
              <label class="toggle-label">
                <span class="toggle-text">Markdown форматирование</span>
                <div class="toggle-switch">
                  <input
                    type="checkbox"
                    v-model="useMarkdown"
                    class="toggle-input"
                    :disabled="isLoading"
                  >
                  <span class="toggle-slider"></span>
                </div>
              </label>
            </div>

            <textarea
              v-model="newNoteContent"
              :placeholder="useMarkdown ? 'Содержание заметки (Markdown)...' : 'Содержание заметки...'"
              class="note-content-input"
              :class="{ 'markdown-active': useMarkdown }"
              :disabled="isLoading"
              rows="6"
            ></textarea>
          </div>
        </div>

        <div class="notes-section">
          <div class="section-header">
            <span class="section-title">Записи:</span>
            <div class="header-actions">
              <span class="notes-count">{{ notes.length }}</span>
              <button
                class="refresh-btn"
                @click="refreshNotes"
                :disabled="isLoading"
                title="Обновить заметки"
              >
                🔄
              </button>
            </div>
          </div>

          <div class="notes-list">
            <!-- Используем sortedNotes вместо notes -->
            <Note
              v-for="note in sortedNotes"
              :key="note.id"
              :note="note"
            />
          </div>
        </div>

        <div class="no-notes" v-if="isLoading">
          <div class="no-notes-icon">⏳</div>
          <p>Загрузка заметок...</p>
        </div>

        <div class="no-notes" v-else-if="notes.length === 0">
          <div class="no-notes-icon">📝</div>
          <p>Заметок пока нет</p>
          <p class="no-notes-hint">Создайте первую заметку</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.note-list-overlay {
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

.note-list-modal {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 750px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

.note-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #e0e0e0;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border-radius: 20px 20px 0 0;
}

.note-list-header h2 {
  margin: 0;
  font-size: 22px;
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

.note-list-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.create-note-section {
  padding: 0 24px;
  border-bottom: 1px solid #f0f0f0;
}

.create-note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 0;
  cursor: pointer;
  transition: background 0.3s ease;
  border-radius: 8px;
  margin: 0 -12px;
  padding-left: 12px;
  padding-right: 12px;
}

.create-note-header:hover {
  background: #f8f9fa;
}

.create-note-title {
  font-weight: 600;
  color: #2c3e50;
  font-size: 16px;
}

.toggle-icon {
  color: #7f8c8d;
  font-size: 14px;
}

.create-note-content {
  padding: 0 0 20px 0;
  animation: slideDown 0.3s ease;
}

.create-note-input-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.create-note-input {
  flex: 1;
  padding: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 15px;
}

.create-note-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.markdown-toggle {
  margin-bottom: 16px;
}

.toggle-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 10px 0;
}

.toggle-text {
  font-size: 15px;
  color: #2c3e50;
  font-weight: 500;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.toggle-input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

.toggle-input:checked + .toggle-slider {
  background-color: #667eea;
}

.toggle-input:checked + .toggle-slider:before {
  transform: translateX(26px);
}

.toggle-input:disabled + .toggle-slider {
  opacity: 0.6;
  cursor: not-allowed;
}

.note-content-input {
  width: 100%;
  padding: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 15px;
  resize: none;
  font-family: inherit;
  transition: all 0.3s ease;
  margin: 0;
  box-sizing: border-box;
  min-height: 150px;
}

.note-content-input.markdown-active {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.02);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
}

.note-content-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.create-note-btn {
  padding: 14px 24px;
  background: linear-gradient(135deg, #667eea 0%, #369b70 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.create-note-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.create-note-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.notes-section {
  flex: 1;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: #f8f9fa;
}

.section-title {
  font-weight: 600;
  color: #2c3e50;
  font-size: 16px;
}

.notes-count {
  background: #667eea;
  color: white;
  padding: 6px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.refresh-btn {
  background: rgba(102, 126, 234, 0.1);
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.2);
  transform: scale(1.1);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.notes-list {
  overflow-y: auto;
  max-height: 450px;
  padding: 0 24px;
}

.no-notes {
  text-align: center;
  padding: 50px 24px;
  color: #7f8c8d;
}

.no-notes-icon {
  font-size: 56px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.no-notes-hint {
  font-size: 14px;
  color: #7f8c8d;
  margin-top: 12px;
}

.close-btn:disabled {
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

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 500px;
  }
}

@media (max-width: 768px) {
  .note-list-modal {
    width: 95%;
    max-height: 95vh;
    border-radius: 16px;
    max-width: 95%;
  }

  .note-list-header {
    padding: 20px;
  }

  .create-note-input-wrapper {
    flex-direction: column;
  }

  .header-actions {
    flex-direction: column;
    gap: 8px;
  }

  .notes-list {
    padding: 0 20px;
    max-height: 400px;
  }

  .toggle-label {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}

@media (min-width: 1200px) {
  .note-list-modal {
    max-width: 800px;
  }
}
</style>
