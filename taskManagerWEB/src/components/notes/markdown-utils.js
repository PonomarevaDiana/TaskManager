// markdown-utils.js

/**
 * Преобразует Markdown текст в HTML
 * @param {string} content - Markdown текст
 * @returns {string} HTML строка
 */
export function renderMarkdown(content) {
  if (!content) return '';

  return content
    // Заголовки
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^#### (.*$)/gim, '<h4>$1</h4>')
    .replace(/^##### (.*$)/gim, '<h5>$1</h5>')
    .replace(/^###### (.*$)/gim, '<h6>$1</h6>')

    // Жирный текст
    .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
    .replace(/__(.*?)__/gim, '<strong>$1</strong>')

    // Курсив
    .replace(/\*(.*?)\*/gim, '<em>$1</em>')
    .replace(/_(.*?)_/gim, '<em>$1</em>')

    // Зачеркнутый текст
    .replace(/~~(.*?)~~/gim, '<del>$1</del>')

    // Код
    .replace(/`(.*?)`/gim, '<code>$1</code>')

    // Блочный код (базовый)
    .replace(/```([\s\S]*?)```/gim, '<pre><code>$1</code></pre>')

    // Ссылки
    .replace(/\[([^\[]+)\]\(([^\)]+)\)/gim, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')

    // Списки (базовые)
    .replace(/^\s*\*\s(.*$)/gim, '<ul><li>$1</li></ul>')
    .replace(/^\s*-\s(.*$)/gim, '<ul><li>$1</li></ul>')
    .replace(/^\s*\d\.\s(.*$)/gim, '<ol><li>$1</li></ol>')

    // Цитаты
    .replace(/^\>\s(.*$)/gim, '<blockquote>$1</blockquote>')

    // Горизонтальные линии
    .replace(/^\-\-\-$/gim, '<hr>')
    .replace(/^\*\*\*$/gim, '<hr>')

    // Переносы строк
    .replace(/\n/g, '<br>')

    // Очистка дублирующихся тегов списков
    .replace(/<\/ul>\s*<ul>/gim, '')
    .replace(/<\/ol>\s*<ol>/gim, '')
    .replace(/<\/li>\s*<br>\s*<li>/gim, '</li><li>');
}

/**
 * Проверяет, содержит ли текст Markdown разметку
 * @param {string} content - Текст для проверки
 * @returns {boolean} true если содержит Markdown
 */
export function hasMarkdown(content) {
  if (!content) return false;

  const markdownPatterns = [
    /^#+\s+/m,           // Заголовки
    /\*\*.*\*\*/,        // Жирный текст
    /\*[^*].*[^*]\*/,    // Курсив
    /\[.*\]\(.*\)/,      // Ссылки
    /^[-*]\s+/m,         // Маркированные списки
    /^\d+\.\s+/m,        // Нумерованные списки
    /^>\s+/m,            // Цитаты
    /`[^`]+`/,           // Код
    /~~.*~~/,            // Зачеркнутый текст
  ];

  return markdownPatterns.some(pattern => pattern.test(content));
}

/**
 * Очищает Markdown разметку из текста
 * @param {string} content - Текст с Markdown
 * @returns {string} Очищенный текст
 */
export function stripMarkdown(content) {
  if (!content) return '';

  return content
    .replace(/#{1,6}\s*/g, '')
    .replace(/(\*\*|__)(.*?)\1/g, '$2')
    .replace(/(\*|_)(.*?)\1/g, '$2')
    .replace(/~~(.*?)~~/g, '$1')
    .replace(/`(.*?)`/g, '$1')
    .replace(/\[([^\]]+)\]\([^\)]+\)/g, '$1')
    .replace(/^\s*[-*+]\s+/gm, '')
    .replace(/^\s*\d+\.\s+/gm, '')
    .replace(/^>\s+/gm, '');
}

/**
 * Подсветка синтаксиса для блочного кода (базовая реализация)
 * @param {string} code - Код для подсветки
 * @param {string} language - Язык программирования
 * @returns {string} HTML с подсветкой
 */
export function highlightCode(code, language = '') {
  // Базовая реализация - можно расширить с помощью библиотек типа highlight.js
  return `<pre class="code-block language-${language}"><code>${escapeHtml(code)}</code></pre>`;
}

/**
 * Экранирование HTML специальных символов
 * @param {string} text - Текст для экранирования
 * @returns {string} Экранированный текст
 */
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

export default {
  renderMarkdown,
  hasMarkdown,
  stripMarkdown,
  highlightCode
};
