/**
 * Admin Data Helpers
 * Helper functions for admin dashboard data loading
 */

import { AdminRestaurant, AdminCustomer, SystemLog } from '../types/admin';

// Get restaurants for admin dashboard - uses backend API
export const getAdminRestaurants = async (): Promise<AdminRestaurant[]> => {
  const { getAllRestaurants } = await import('../api/adminApi');
  return await getAllRestaurants();
};

// Get customers for admin dashboard - uses backend API
export const getAdminCustomers = async (): Promise<AdminCustomer[]> => {
  const { getAllCustomers } = await import('../api/adminApi');
  return await getAllCustomers();
};

// Get system logs - would need backend endpoint
export const getSystemLogs = async (): Promise<SystemLog[]> => {
  // System logs would need a dedicated backend endpoint
  // For now, return empty array
  return [];
};

// Export initial data as a function to get fresh copies
export const getInitialAdminData = async () => ({
  restaurants: await getAdminRestaurants(),
  customers: await getAdminCustomers(),
  logs: await getSystemLogs()
});

