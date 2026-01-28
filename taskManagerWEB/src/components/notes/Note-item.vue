<script>
import { renderMarkdown } from './markdown-utils.js';
import userService from '@/services/User-service.js';

export default {
  name: 'Note-item',
  props: {
    note: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      authorName: '',
      isLoadingAuthor: false
    }
  },
  methods: {
    formatDate(dateString) {
      const date = new Date(dateString);
      return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    },

    async loadUserById(id) {
      if (!id) return '';

      this.isLoadingAuthor = true;
      try {
        const response = await userService.getUserById(id);
        return response.data.username
      } catch (error) {
        console.error('Ошибка загрузки пользователя:', error);
        return id;
      } finally {
        this.isLoadingAuthor = false;
      }
    },

    async loadAuthor() {
      if (this.note.userId) {
        this.authorName = await this.loadUserById(this.note.userId);
      }
    },

    renderMarkdown
  },
  async mounted() {
    await this.loadAuthor();
  }
}
</script>

<template>
  <div class="note-item">
    <div class="note-header">
      <h3 class="note-title">{{ note.title }}</h3>
      <div class="note-actions">
        <span class="markdown-badge" v-if="note.useMarkdown" title="Markdown форматирование">
          MD
        </span>
      </div>
    </div>

    <div
      class="note-content"
      v-if="note.useMarkdown"
      v-html="renderMarkdown(note.content)"
    ></div>
    <p
      class="note-content"
      v-else
    >{{ note.content }}</p>

    <div class="note-meta">
      <span class="note-author" v-if="note.userId">
        Автор: {{ isLoadingAuthor ? 'Загрузка...' : authorName }}
      </span>
      <span class="note-date">{{ formatDate(note.createdAt) }}</span>
    </div>
  </div>
</template>

<style scoped>
.note-author {
  font-size: 12px;
  font-weight: 500;
  color: #6c757d;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.note-date {
  font-size: 12px;
  color: #6c757d;
}

.note-item {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  transition: all 0.3s ease;
  text-align: left;
}

.note-item:hover {
  background: #e9ecef;
  transform: translateY(-1px);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.note-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  flex: 1;
  text-align: left;
}

.note-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.markdown-badge {
  background: #667eea;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
}

.delete-note-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.3s ease;
}

.delete-note-btn:hover {
  background: rgba(220, 53, 69, 0.1);
}

.note-content {
  margin: 0 0 12px 0;
  color: #495057;
  line-height: 1.4;
  font-size: 14px;
  text-align: left;
  word-wrap: break-word;
}

/* Стили для блока реакций */
.note-reactions {
  margin: 12px 0;
  border-top: 1px solid #e9ecef;
  padding-top: 12px;
}

.note-content:deep(h1) {
  font-size: 18px;
  font-weight: 600;
  margin: 8px 0;
  color: #2c3e50;
  text-align: left;
}

.note-content:deep(h2) {
  font-size: 16px;
  font-weight: 600;
  margin: 6px 0;
  color: #2c3e50;
  text-align: left;
}

.note-content:deep(h3) {
  font-size: 14px;
  font-weight: 600;
  margin: 4px 0;
  color: #2c3e50;
  text-align: left;
}

.note-content:deep(h4) {
  font-size: 13px;
  font-weight: 600;
  margin: 4px 0;
  color: #2c3e50;
  text-align: left;
}

.note-content:deep(h5) {
  font-size: 12px;
  font-weight: 600;
  margin: 4px 0;
  color: #2c3e50;
  text-align: left;
}

.note-content:deep(h6) {
  font-size: 11px;
  font-weight: 600;
  margin: 4px 0;
  color: #2c3e50;
  text-align: left;
}

.note-content:deep(strong) {
  font-weight: 600;
  color: #2c3e50;
}

.note-content:deep(em) {
  font-style: italic;
}

.note-content:deep(del) {
  text-decoration: line-through;
  color: #6c757d;
}

.note-content:deep(code) {
  background: #e9ecef;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
  text-align: left;
  display: inline-block;
}

.note-content:deep(pre) {
  background: #2d3748;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
  text-align: left;
}

.note-content:deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
  text-align: left;
  display: block;
}

.note-content:deep(a) {
  color: #667eea;
  text-decoration: none;
}

.note-content:deep(a:hover) {
  text-decoration: underline;
}

.note-content:deep(blockquote) {
  border-left: 4px solid #667eea;
  padding-left: 12px;
  margin: 8px 0;
  color: #6c757d;
  font-style: italic;
  text-align: left;
}

.note-content:deep(ul),
.note-content:deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
  text-align: left;
}

.note-content:deep(li) {
  margin: 4px 0;
  text-align: left;
}

.note-content:deep(p) {
  text-align: left;
  margin: 4px 0;
}

.note-content:deep(hr) {
  border: none;
  border-top: 1px solid #e9ecef;
  margin: 16px 0;
}

.note-content:deep(br) {
  content: "";
  display: block;
  margin: 4px 0;
}

.note-content:deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
  text-align: left;
}

.note-content:deep(th),
.note-content:deep(td) {
  border: 1px solid #e9ecef;
  padding: 6px 8px;
  text-align: left;
}

.note-content:deep(th) {
  background: #f8f9fa;
  font-weight: 600;
}
</style>
