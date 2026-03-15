import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import api from '../services/api';

export const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      darkMode: false,

      login: async (email, password) => {
        const { data } = await api.post('/api/auth/login', { email, password });
        set({ user: data.user, accessToken: data.accessToken, refreshToken: data.refreshToken, isAuthenticated: true });
        return data;
      },

      register: async (email, password, firstName, lastName, role) => {
        const { data } = await api.post('/api/auth/register', { email, password, firstName, lastName, role });
        set({ user: data.user, accessToken: data.accessToken, refreshToken: data.refreshToken, isAuthenticated: true });
        return data;
      },

      logout: () => {
        set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
      },

      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),

      toggleDarkMode: () => set((state) => ({ darkMode: !state.darkMode })),
    }),
    { name: 'locallens-auth' }
  )
);
