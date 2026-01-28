import axios from 'axios';

const ReactionsService = {
  async pressReactionButton(reactionData) {
    try {
      const response = await axios.post('/api/reactions', reactionData, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json'
        }
      });
      return response;
    } catch (error) {
      console.error('Error adding reaction:', error);
      throw error;
    }
  },

  async getReactionUsers(objectId, objectType, reactionType) {
    try {
      const response = await axios.get(`/api/reactions/users`, {
        params: {
          objectId: objectId,
          objectType: objectType,
          reactionType: reactionType
        },
        withCredentials: true
      });
      return response;
    } catch (error) {
      console.error('Error fetching reaction users:', error);
      console.error('Error response:', error.response?.data);
      throw error;
    }
  },

  async getReactionStats(objectId, objectType) {
    try {
      const response = await axios.get(`/api/reactions/stats`, {
        params: {
          objectId: objectId,
          objectType: objectType
        },
        withCredentials: true
      });
      return response;
    } catch (error) {
      console.error('Error fetching reaction stats:', error);
      console.error('Error response:', error.response?.data);
      throw error;
    }
  }
};

export default ReactionsService;
