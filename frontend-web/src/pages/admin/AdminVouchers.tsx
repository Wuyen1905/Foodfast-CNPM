import React from 'react';
import AdminNavigation from '@/components/admin/AdminNavigation';
import VoucherManagement from '@/components/VoucherManagement';

const AdminVouchers: React.FC = () => (
  <div style={{ minHeight: '100vh', background: '#f8f9fa', padding: 20 }}>
    <AdminNavigation />
    <VoucherManagement admin primary="#007bff" />
  </div>
);

export default AdminVouchers;
