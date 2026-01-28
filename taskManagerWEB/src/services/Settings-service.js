import axios from 'axios';

class SettingsService {
  constructor() {
    this.authClient = axios.create({
      baseURL: '/auth/settings',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true
    });

    this.apiClient = axios.create({
      baseURL: '/api/settings',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true
    });
  }

  // Профиль
  async updateProfile(profileData) {
    try {
      const response = await this.apiClient.post('/update-profile', {
        userId: profileData.userId,
        firstName: profileData.firstName,
        lastName: profileData.lastName,
        username: profileData.username
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  // Пароль
  async changePassword(passwordData) {
    try {
      const response = await this.authClient.post('/set-new-password', {
        userId: passwordData.userId,
        oldPassword: passwordData.oldPassword,
        newPassword: passwordData.newPassword
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  handleError(error) {
    if (error.response) {
      const message = error.response.data?.error || error.response.data?.message || 'Ошибка сервера';
      return new Error(message);
    } else if (error.request) {
      return new Error('Ошибка подключения к серверу');
    } else {
      return new Error('Ошибка при отправке запроса');
    }
  }
}

export default new SettingsService();
