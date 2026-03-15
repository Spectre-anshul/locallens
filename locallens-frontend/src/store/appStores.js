import { create } from 'zustand';

export const useTripStore = create((set) => ({
  activeTripId: null,
  selectedDay: 1,
  panelOrder: ['overview', 'itinerary', 'flights', 'accommodation', 'budget', 'events', 'notes', 'map'],

  setActiveTrip: (tripId) => set({ activeTripId: tripId, selectedDay: 1 }),
  setSelectedDay: (day) => set({ selectedDay: day }),
  reorderPanels: (newOrder) => set({ panelOrder: newOrder }),
}));

export const useMapStore = create((set) => ({
  viewport: { latitude: 35.01, longitude: 135.77, zoom: 12, pitch: 45 },
  activeTimeFilter: 'NOW',
  visibleLayers: { heatmap: true, pois: true, hiddenGems: true, route: true, traffic: false },
  selectedPOI: null,

  setViewport: (viewport) => set({ viewport }),
  setTimeFilter: (filter) => set({ activeTimeFilter: filter }),
  toggleLayer: (layer) => set((s) => ({
    visibleLayers: { ...s.visibleLayers, [layer]: !s.visibleLayers[layer] }
  })),
  selectPOI: (poi) => set({ selectedPOI: poi }),
}));

export const useNotificationStore = create((set) => ({
  unreadCount: 0,
  toasts: [],
  addToast: (toast) => set((s) => ({ toasts: [...s.toasts, { ...toast, id: Date.now() }] })),
  dismissToast: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
  setUnreadCount: (count) => set({ unreadCount: count }),
}));
