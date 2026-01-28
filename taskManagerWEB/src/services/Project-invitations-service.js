import axios from 'axios';

const API_BASE_URL = '/api/project-invitations';

const ProjectInvitationsService = {
  getPendingInvitations() {
    return axios.get(`${API_BASE_URL}/pending`)
  },

  acceptInvitation(invitationId) {
    return axios.post(`${API_BASE_URL}/${invitationId}/accept`)
  },

  declineInvitation(invitationId) {
    return axios.post(`${API_BASE_URL}/${invitationId}/decline`)
  }
};

export default ProjectInvitationsService;
