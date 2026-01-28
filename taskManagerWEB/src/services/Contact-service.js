import axios from 'axios';

const API_BASE_URL = '/api/contact';

const ContactService = {
  getKey() {
    return axios.get(`${API_BASE_URL}/getMyFriendshipKey`);
  },

  getContacts() {
    return axios.get(`${API_BASE_URL}/getMyContactList`);
  },

  sendRequest(key) {
    return axios.post(`${API_BASE_URL}/sendRequest`, {
      key: key
    });
  },

  acceptRequest(targetUserId) {
    return axios.post(`${API_BASE_URL}/acceptRequest/${targetUserId}`);
  },

  declineRequest(targetUserId) {
    return axios.post(`${API_BASE_URL}/declineRequest/${targetUserId}`);
  },

  getIncomingRequests() {
    return axios.get(`${API_BASE_URL}/incomingRequests`);
  },

  getOutgoingRequests() {
    return axios.get(`${API_BASE_URL}/outgoingRequests`);
  },

  deleteContact(contactId) {
    return axios.delete(`${API_BASE_URL}/delete-contact/${contactId}`)
  }
};

export default ContactService;
