import React from 'react';
import styled from 'styled-components';
import type { AdminOverview } from '@/api/adminApi';
import { formatVND } from '@/utils/currency';
import type { AdminTab } from '@/pages/admin/AdminDashboard';

const Grid=styled.div`display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px;`;
const Card=styled.button<{ $alert?:boolean }>`appearance:none;text-align:left;border:1px solid ${p=>p.$alert?'#fecaca':'#e5e7eb'};border-left:5px solid ${p=>p.$alert?'#dc3545':'#667eea'};background:#fff;border-radius:14px;padding:19px;box-shadow:0 2px 10px rgba(0,0,0,.07);cursor:pointer;transition:.18s ease;&:hover{transform:translateY(-3px);box-shadow:0 8px 22px rgba(0,0,0,.12);}small{display:block;color:#69707d;margin-top:7px;}strong{display:block;font-size:27px;color:#20242b;margin:8px 0 2px;}`;
const Revenue=styled.section`margin-top:20px;background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;border-radius:15px;padding:22px;display:grid;grid-template-columns:repeat(3,1fr);gap:18px;@media(max-width:700px){grid-template-columns:1fr;}div{padding:6px 12px;border-left:1px solid rgba(255,255,255,.3);}strong{display:block;font-size:24px;margin-top:6px;}`;

export type AdminContextFilter = {
  restaurantStatus?: 'PENDING';
  orderDate?: 'today';
  orderIds?: string[];
  supportFilter?: 'all'|'overdue'|'complaint'|'refund'|'ticket';
};
interface Props{overview:AdminOverview;onNavigate:(tab:AdminTab,filter?:AdminContextFilter)=>void;}

const AdminOperationsOverview:React.FC<Props>=({overview,onNavigate})=><>
  <Grid>
    <Card onClick={()=>onNavigate('orders',{orderDate:'today'})}><span>📦 Đơn hôm nay</span><strong>{overview.ordersToday}</strong><small>Mở danh sách đơn được tạo hôm nay</small></Card>
    <Card $alert={overview.failedDeliveries>0} onClick={()=>onNavigate('orders',{orderIds:overview.failedDeliveryOrderIds})}><span>🛵 Giao thất bại</span><strong>{overview.failedDeliveries}</strong><small>Đơn cần kiểm tra nguyên nhân giao thất bại</small></Card>
    <Card $alert={overview.paymentReconciliationNeeded>0} onClick={()=>onNavigate('orders',{orderIds:overview.reconciliationOrderIds})}><span>💳 Cần đối soát</span><strong>{overview.paymentReconciliationNeeded}</strong><small>Thanh toán thiếu dữ liệu hoặc chờ quá hạn</small></Card>
    <Card $alert={overview.openComplaints>0} onClick={()=>onNavigate('support',{supportFilter:'complaint'})}><span>🛟 Khiếu nại đang mở</span><strong>{overview.openComplaints}</strong><small>Mở hàng đợi xử lý khiếu nại</small></Card>
    <Card $alert={overview.pendingRefunds>0} onClick={()=>onNavigate('support',{supportFilter:'refund'})}><span>💸 Chờ hoàn tiền</span><strong>{overview.pendingRefunds}</strong><small>Khoản hoàn đang cần xử lý</small></Card>
    <Card $alert={overview.pendingRestaurants>0} onClick={()=>onNavigate('restaurants',{restaurantStatus:'PENDING'})}><span>🏪 Nhà hàng chờ duyệt</span><strong>{overview.pendingRestaurants}</strong><small>Kiểm tra hồ sơ và phê duyệt</small></Card>
    <Card onClick={()=>onNavigate('support',{supportFilter:'ticket'})}><span>🎫 Phiếu hỗ trợ đang mở</span><strong>{overview.openSupportTickets}</strong><small>Hàng đợi hỗ trợ toàn hệ thống</small></Card>
    <Card $alert={overview.overdueSupportTickets>0} onClick={()=>onNavigate('support',{supportFilter:'overdue'})}><span>⏰ Yêu cầu hỗ trợ quá hạn</span><strong>{overview.overdueSupportTickets}</strong><small>Đã quá thời gian cam kết xử lý</small></Card>
  </Grid>
  <Revenue><div>Doanh thu hôm nay<strong>{formatVND(overview.revenueToday)}</strong></div><div>Doanh thu tuần này<strong>{formatVND(overview.revenueWeek)}</strong></div><div>Doanh thu tháng này<strong>{formatVND(overview.revenueMonth)}</strong></div></Revenue>
</>;
export default AdminOperationsOverview;
