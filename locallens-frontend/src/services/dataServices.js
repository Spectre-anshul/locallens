import api from './api';

export const tripService = {
  createTrip: (data) => api.post('/api/trips', data),
  getUserTrips: () => api.get('/api/trips'),
  getTripById: (id) => api.get(`/api/trips/${id}`),
  deleteTrip: (id) => api.delete(`/api/trips/${id}`),
  uploadDocument: (tripId, file, type) => {
    const form = new FormData();
    form.append('file', file);
    form.append('type', type);
    return api.post(`/api/trips/${tripId}/documents`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export const itineraryService = {
  generate: (tripId) => api.post('/api/itinerary/generate', { tripId }),
  getDays: (tripId) => api.get(`/api/itinerary/${tripId}/days`),
  getDay: (tripId, dayNumber) => api.get(`/api/itinerary/${tripId}/days/${dayNumber}`),
};

export const experienceService = {
  search: (params) => api.get('/api/experiences', { params }),
  getById: (id) => api.get(`/api/experiences/${id}`),
};

export const eventService = {
  getTonight: (city) => api.get('/api/events/tonight', { params: { city } }),
};

export const mapService = {
  getHeatmap: (lat, lng, radiusKm, hour) =>
    api.get('/api/map/heatmap', { params: { lat, lng, radiusKm, hour } }),
  getPOIs: (lat, lng, radiusKm, category) =>
    api.get('/api/map/pois', { params: { lat, lng, radiusKm, category } }),
};

export const notificationService = {
  getAll: (unreadOnly = false) => api.get('/api/notifications', { params: { unreadOnly } }),
  markRead: (id) => api.patch(`/api/notifications/${id}/read`),
  getUnreadCount: () => api.get('/api/notifications/unread-count'),
};
