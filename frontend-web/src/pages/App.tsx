import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { MenuProvider } from '@/context/MenuContext';
import { RestaurantSelectionProvider } from '@/context/RestaurantSelectionContext';
import Footer from '../components/Footer';
import Navbar from '../components/Navbar';
import ResponsiveLayout from '../components/ResponsiveLayout';
import ProtectedRoute from '../components/ProtectedRoute';

const Menu = lazy(() => import('./Menu'));
const Details = lazy(() => import('./Details'));
const Cart = lazy(() => import('./Cart'));
const Checkout = lazy(() => import('./Checkout'));
const VNPayReturn = lazy(() => import('./VNPayReturn'));
const OrderConfirmation = lazy(() => import('./OrderConfirmation'));
const MyOrders = lazy(() => import('./MyOrders'));
const OrderDetail = lazy(() => import('./OrderDetail'));
const OrderGroupDetail = lazy(() => import('./OrderGroupDetail'));
const Login = lazy(() => import('./Login'));
const RegisterPage = lazy(() => import('./RegisterPage'));
const RestaurantDashboard = lazy(() => import('./restaurant/RestaurantDashboard'));
const SweetDreamsDashboard = lazy(() => import('./restaurant/SweetDreamsDashboard'));
const AlohaKitchenDashboard = lazy(() => import('./restaurant/AlohaKitchenDashboard'));
const AlohaDashboard = lazy(() => import('./restaurant/AlohaDashboard'));
const SupportChatWidget = lazy(() => import('../components/SupportChatWidget'));
const Favorites = lazy(() => import('./Favorites'));
const Addresses = lazy(() => import('./Addresses'));
const ManagerHome = lazy(() => import('./manager/ManagerHome'));
const ManagerLogin = lazy(() => import('./manager/ManagerLogin'));

const PageLoading = () => (
  <div role="status" aria-live="polite" style={{ padding: '48px', textAlign: 'center' }}>
    Đang tải nội dung...
  </div>
);

// Role-based route protection component
const RoleGuardedRoute: React.FC<{ children: React.ReactNode; allowedRoles: string[] }> = ({ children, allowedRoles }) => {
  const { user } = useAuth();
  
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  
  if (!allowedRoles.includes(user.role)) {
    // Redirect to appropriate page based on role
    const redirectPath = user.role === 'restaurant' ? '/restaurant' :
                         user.role === 'admin' ? '/admin/dashboard' :
                         user.role === 'manager' ? '/manager' : '/menu';
    return <Navigate to={redirectPath} replace />;
  }
  
  return <>{children}</>;
};

const PublicNavbar: React.FC = () => {
  const { pathname } = useLocation();
  return pathname.startsWith('/admin') || pathname.startsWith('/manager') ? null : <Navbar />;
};

const PublicFooter: React.FC = () => {
  const { pathname } = useLocation();
  return pathname.startsWith('/admin') || pathname.startsWith('/manager') ? null : <><Footer /><SupportChatWidget /></>;
};

const App: React.FC = () => {
  return (
    <RestaurantSelectionProvider>
      <MenuProvider>
        <BrowserRouter>
          <ResponsiveLayout>
            <PublicNavbar />
            <Suspense fallback={<PageLoading />}>
          <Routes>
            <Route path="/" element={<Navigate to="/menu" replace />} />
            <Route path="/home" element={<Navigate to="/menu" replace />} />
            <Route path="/homepage" element={<Navigate to="/menu" replace />} />
            <Route path="/menu" element={<Menu />} />
            <Route path="/menu/:id" element={<Details />} />
            <Route path="/details/:id" element={<Details />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/customer-info" element={<Navigate to="/login" replace />} />

            <Route path="/manager/login" element={<ManagerLogin />} />
            <Route path="/manager" element={
              <ProtectedRoute requireRole="manager" redirectTo="/manager/login">
                <ManagerHome />
              </ProtectedRoute>
            } />
            
            {/* Protected routes for customers only */}
            <Route path="/cart" element={
              <ProtectedRoute requireRole="customer">
                <Cart />
              </ProtectedRoute>
            } />
            <Route path="/checkout" element={
              <RoleGuardedRoute allowedRoles={['customer']}>
                <Checkout />
              </RoleGuardedRoute>
            } />
            <Route path="/vnpay-return" element={<VNPayReturn />} />
            <Route path="/order-confirmation/:id" element={
              <RoleGuardedRoute allowedRoles={['customer']}>
                <OrderConfirmation />
              </RoleGuardedRoute>
            } />
            <Route path="/orders" element={
              <RoleGuardedRoute allowedRoles={['customer']}>
                <MyOrders />
              </RoleGuardedRoute>
            } />
            <Route path="/favorites" element={
              <RoleGuardedRoute allowedRoles={['customer']}>
                <Favorites />
              </RoleGuardedRoute>
            } />
            <Route path="/addresses" element={
              <RoleGuardedRoute allowedRoles={['customer']}>
                <Addresses />
              </RoleGuardedRoute>
            } />
            <Route path="/orders/:id" element={
              <RoleGuardedRoute allowedRoles={['customer']}>
                <OrderDetail />
              </RoleGuardedRoute>
            } />
            <Route path="/order-groups/:id" element={
              <RoleGuardedRoute allowedRoles={['customer']}>
                <OrderGroupDetail />
              </RoleGuardedRoute>
            } />

            {/* Order tracking route removed from main navigation */}
            
            {/* Restaurant-only routes */}
            <Route path="/restaurant" element={
              <ProtectedRoute requireRole="restaurant">
                <RestaurantDashboard />
              </ProtectedRoute>
            } />
            <Route path="/restaurant/sweetdreams" element={
              <ProtectedRoute requireRole="restaurant">
                <SweetDreamsDashboard />
              </ProtectedRoute>
            } />
            <Route path="/sweetdreams" element={
              <ProtectedRoute requireRole="restaurant">
                <SweetDreamsDashboard />
              </ProtectedRoute>
            } />
            <Route path="/aloha" element={
              <ProtectedRoute requireRole="restaurant">
                <AlohaKitchenDashboard />
              </ProtectedRoute>
            } />
            <Route path="/aloha-dashboard" element={
              <ProtectedRoute requireRole="restaurant">
                <AlohaDashboard />
              </ProtectedRoute>
            } />
          </Routes>
          <PublicFooter />
            </Suspense>
          </ResponsiveLayout>
        </BrowserRouter>
      </MenuProvider>
    </RestaurantSelectionProvider>
  );
};

export default App;


