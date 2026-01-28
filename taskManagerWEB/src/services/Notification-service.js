import axios from 'axios';

const API_BASE_URL = '/api/notification';

const NotificationService = {
  getNotificationsById() {
    return axios.get(`${API_BASE_URL}`);
  },

  getUnreadNotifications() {
    return axios.get(`${API_BASE_URL}/unread`);
  },

  getUnreadCount() {
    return axios.get(`${API_BASE_URL}/unread-count`);
  },

  markAsRead(notificationId) {
    return axios.put(`${API_BASE_URL}/${notificationId}/read`);
  },

  markAllAsRead() {
    return axios.put(`${API_BASE_URL}/read-all`);
  },

  deleteNotification(notificationId) {
    return axios.delete(`${API_BASE_URL}/${notificationId}`);
  },

  deleteAllNotifications() {
    return axios.delete(`${API_BASE_URL}`);
  },
};

export default NotificationService;
