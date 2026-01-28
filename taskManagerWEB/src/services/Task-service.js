import axios from 'axios'

const API_BASE_URL = '/api/tasks'

const TaskService = {
  getAllTasks() {
    return axios.get(API_BASE_URL)
  },

  getTaskById(id) {
    return axios.get(`${API_BASE_URL}/${id}`)
  },

  createTask(task) {
    return axios.post(API_BASE_URL, task)
  },

  updateTask(id, task) {
    return axios.put(`${API_BASE_URL}/${id}`, task)
  },

  deleteTask(id) {
    return axios.delete(`${API_BASE_URL}/${id}`)
  },
  getTasksByAssignee(id) {
    return axios.get(`${API_BASE_URL}/assignee/${id}`)
  },

  getTasksByPersonalId(id) {
    return axios.get(`${API_BASE_URL}/personal/${id}`)
  },
}

export default TaskService
