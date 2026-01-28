import axios from 'axios'

const API_URL = '/api/users'

class UserService {
  async getAllUsers() {
    try {
      return axios.get(API_URL)
    } catch (error) {
      console.error('Ошибка загрузки пользователей:', error)
      throw error
    }
  }

  async getUserById(id) {
    try {
      return axios.get(`${API_URL}/${id}`)
    } catch (error) {
      console.error('Ошибка загрузки пользователя:', error)
      throw error
    }
  }
}

export default new UserService()
