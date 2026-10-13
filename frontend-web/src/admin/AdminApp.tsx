import React, { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ThemeProvider } from "styled-components";
import { AdminAuthProvider } from "@/context/AdminAuthContext";
import { GlobalStyle } from "../theme";
import { enhancedTheme } from "../globalStyles";
import AdminProtectedRoute from "@/components/AdminProtectedRoute";

const AdminLogin = lazy(() => import("@/pages/admin/AdminLogin"));
const AdminDashboard = lazy(() => import("@/pages/admin/AdminDashboard"));
const AdminUsers = lazy(() => import("@/pages/admin/AdminUsers"));
const AdminRestaurants = lazy(() => import("@/pages/admin/AdminRestaurants"));
const AdminOrders = lazy(() => import("@/pages/admin/AdminOrders"));
const AdminVouchers = lazy(() => import("@/pages/admin/AdminVouchers"));

/**
 * AdminApp - Completely independent admin management system
 * 
 * This component provides a separate routing and authentication system
 * for admin users, isolated from the main user-facing application.
 * 
 * Routes:
 * - /admin/login → Admin login page
 * - /admin/dashboard → Main admin dashboard (protected)
 * - /admin/users → User management (protected)
 * - /admin/restaurants → Restaurant management (protected)
 * - /admin/orders → Order management (protected)
 */
export default function AdminApp() {
  return (
    <ThemeProvider theme={enhancedTheme}>
      <GlobalStyle />
      <AdminAuthProvider>
        <BrowserRouter>
          <Suspense fallback={<div role="status" style={{ padding: 48, textAlign: "center" }}>Đang tải trang quản trị...</div>}>
          <Routes>
            {/* Admin login page - publicly accessible */}
            <Route path="/admin/login" element={<AdminLogin />} />

            {/* Protected admin routes - require authentication */}
            <Route 
              path="/admin/dashboard" 
              element={
                <AdminProtectedRoute>
                  <AdminDashboard />
                </AdminProtectedRoute>
              } 
            />
            <Route 
              path="/admin/users" 
              element={
                <AdminProtectedRoute>
                  <AdminUsers />
                </AdminProtectedRoute>
              } 
            />
            <Route 
              path="/admin/restaurants" 
              element={
                <AdminProtectedRoute>
                  <AdminRestaurants />
                </AdminProtectedRoute>
              } 
            />
            <Route 
              path="/admin/orders" 
              element={
                <AdminProtectedRoute>
                  <AdminOrders />
                </AdminProtectedRoute>
              } 
            />
            <Route
              path="/admin/vouchers"
              element={
                <AdminProtectedRoute>
                  <AdminVouchers />
                </AdminProtectedRoute>
              }
            />

            {/* Redirect /admin to /admin/login */}
            <Route path="/admin" element={<Navigate to="/admin/login" replace />} />
            
            {/* Catch all admin routes and redirect to login */}
            <Route path="/admin/*" element={<Navigate to="/admin/login" replace />} />
          </Routes>
          </Suspense>
        </BrowserRouter>
      </AdminAuthProvider>
    </ThemeProvider>
  );
}
