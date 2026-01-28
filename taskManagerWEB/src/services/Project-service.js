import axios from 'axios'

const API_BASE_URL = '/api/projects'

const ProjectService = {
  getAllProjects() {
    return axios.get(API_BASE_URL)
  },

  getProject(id) {
    return axios.get(`${API_BASE_URL}/${id}`)
  },

  createProject(projectData) {
    return axios.post(API_BASE_URL, projectData)
  },

  updateProject(id, projectData) {
    return axios.put(`${API_BASE_URL}/${id}`, projectData)
  },

  deleteProject(id) {
    return axios.delete(`${API_BASE_URL}/${id}`)
  },

  getProjectTasks(projectId) {
    return axios.get(`${API_BASE_URL}/${projectId}/tasks`)
  },

  getProjectMembers(projectId) {
    return axios.get(`${API_BASE_URL}/${projectId}/members`)
  },

  getProjectSettings(projectId) {
    return axios.get(`${API_BASE_URL}/${projectId}/settings`)
  },

  updateAutoDeleteDays(projectId, days) {
    if (days === null) {
      return axios.patch(
        `${API_BASE_URL}/${projectId}/settings/auto-delete`,
        { autoDeleteDays: null }
      )
    }

    return axios.patch(
      `${API_BASE_URL}/${projectId}/settings/auto-delete`,
      null,
      {params: { days }}
    );
  },
}

export default ProjectService
