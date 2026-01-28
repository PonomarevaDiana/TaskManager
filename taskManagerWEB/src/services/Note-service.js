import axios from 'axios';

const NoteService = {
  async getObjectsNotes(getObjectId, isProject) {
    try {
      const response = await axios.get(`/api/notes/${getObjectId}`, {
        params: {
          isProject: isProject
        },
        withCredentials: true
      });
      return response;
    } catch (error) {
      if (isProject) {
        console.error('Error fetching project notes:', error);
      } else {
        console.error('Error fetching task notes:', error);
      }
      throw error;
    }
  },

  async createObjectsNote(projectId, noteData) {
    try {
      console.log('Creating note for project:', projectId);
      console.log('Note data:', noteData);

      const response = await axios.post(`/api/notes/${projectId}`, noteData, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json'
        }
      });
      return response;
    } catch (error) {
      console.error('Error creating project note:', error);
      console.error('Error response:', error.response?.data);
      throw error;
    }
  }
};

export default NoteService;
