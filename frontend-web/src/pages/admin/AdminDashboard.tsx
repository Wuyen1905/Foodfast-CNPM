import React,{useCallback,useEffect,useState} from 'react';
import {useNavigate} from 'react-router-dom';
import styled from 'styled-components';
import {Toaster} from 'react-hot-toast';
import {useAdminAuth} from '@/context/AdminAuthContext';
import AdminSidebar from '@/components/admin/AdminSidebar';
import RestaurantTable from '@/components/admin/RestaurantTable';
import CustomerTable from '@/components/admin/CustomerTable';
import SystemLogs from '@/components/admin/SystemLogs';
import AdminOperationsOverview,{type AdminContextFilter} from '@/components/admin/AdminOperationsOverview';
import AdminSupportCenter from '@/components/admin/AdminSupportCenter';
import AdminAnalytics from '@/components/admin/AdminAnalytics';
import AdminOrders from './AdminOrders';
import VoucherManagement from '@/components/VoucherManagement';
import ManagerTable from '@/components/admin/ManagerTable';
import AdminDeliveryDispatch from '@/components/admin/AdminDeliveryDispatch';
import {getAdminOverview,getAdminStats,getAllCustomers,getAllManagers,getAllRestaurants,getAuditLogs,type AdminCustomer,type AdminManager,type AdminOverview,type AdminRestaurant,type AdminStats} from '@/api/adminApi';
import type {SystemLog} from '@/types/admin';

const Layout=styled.div`display:flex;min-height:100vh;background:#f5f7fa;`;
const Main=styled.main`flex:1;margin-left:280px;padding:30px;@media(max-width:768px){margin-left:0;padding:18px;}`;
const Top=styled.header`background:#fff;padding:20px 25px;border-radius:15px;box-shadow:0 2px 10px rgba(0,0,0,.08);display:flex;justify-content:space-between;align-items:center;gap:16px;flex-wrap:wrap;margin-bottom:26px;`;
const Title=styled.h1`font-size:27px;margin:0;`;
const Button=styled.button<{ $danger?:boolean }>`border:0;border-radius:8px;padding:10px 17px;background:${p=>p.$danger?'#dc3545':'#667eea'};color:#fff;font:inherit;font-weight:700;cursor:pointer;margin-left:8px;`;
const Content=styled.div`margin-top:20px;`;
const Empty=styled.div`background:#fff;border-radius:15px;padding:55px;text-align:center;color:#68707e;`;
const Footer=styled.footer`text-align:center;padding:24px;color:#8b919c;margin-top:35px;border-top:1px solid #e1e5e9;`;

export type AdminTab='overview'|'analytics'|'restaurants'|'customers'|'managers'|'orders'|'deliveries'|'vouchers'|'support'|'logs';
interface Props{initialTab?:AdminTab}
const defaultOverview:AdminOverview={ordersToday:0,revenueToday:0,revenueWeek:0,revenueMonth:0,failedDeliveries:0,failedDeliveryOrderIds:[],paymentReconciliationNeeded:0,reconciliationOrderIds:[],openComplaints:0,pendingRefunds:0,pendingRefundOrderIds:[],pendingRestaurants:0,openSupportTickets:0,overdueSupportTickets:0};
const defaultStats:AdminStats={totalCustomers:0,totalRestaurants:0,activeRestaurants:0,pendingRestaurants:0,totalOrders:0,totalRevenue:0,totalCouriers:0,activeCouriers:0,activeDeliveries:0,failedDeliveries:0};
const labels:Record<AdminTab,{icon:string;title:string}>={overview:{icon:'📊',title:'Trung tâm vận hành'},analytics:{icon:'📈',title:'Báo cáo vận hành'},restaurants:{icon:'🏪',title:'Quản lý vòng đời nhà hàng'},customers:{icon:'👥',title:'Hồ sơ khách hàng'},managers:{icon:'🧑‍💼',title:'Nhân viên quản lý'},orders:{icon:'📦',title:'Quản lý đơn hàng'},deliveries:{icon:'🛵',title:'Shipper và phân phối giao hàng'},vouchers:{icon:'🎟️',title:'Quản lý mã giảm giá'},support:{icon:'🎫',title:'Hỗ trợ và báo cáo sự cố'},logs:{icon:'📋',title:'Nhật ký quản trị'}};

const AdminDashboard:React.FC<Props>=({initialTab='overview'})=>{
  const {admin,logout}=useAdminAuth();const navigate=useNavigate();
  const [activeTab,setActiveTab]=useState<AdminTab>(initialTab);const [filter,setFilter]=useState<AdminContextFilter>({});
  const [restaurants,setRestaurants]=useState<AdminRestaurant[]>([]);const [customers,setCustomers]=useState<AdminCustomer[]>([]);const [managers,setManagers]=useState<AdminManager[]>([]);const [logs,setLogs]=useState<SystemLog[]>([]);
  const [stats,setStats]=useState<AdminStats>(defaultStats);const [overview,setOverview]=useState<AdminOverview>(defaultOverview);const [refreshKey,setRefreshKey]=useState(0);const [loading,setLoading]=useState(true);
  const load=useCallback(async()=>{setLoading(true);try{const [restaurantData,customerData,managerData,statsData,logData,overviewData]=await Promise.all([getAllRestaurants(),getAllCustomers(),getAllManagers(),getAdminStats(),getAuditLogs(),getAdminOverview()]);setRestaurants(restaurantData);setCustomers(customerData);setManagers(managerData);setStats(statsData);setLogs(logData);setOverview(overviewData);setRefreshKey(v=>v+1);}finally{setLoading(false);}},[]);
  useEffect(()=>{void load();},[load]);
  useEffect(()=>{if(!admin)navigate('/admin/login');},[admin,navigate]);
  if(!admin)return null;
  const switchTab=(tab:AdminTab,nextFilter:AdminContextFilter={})=>{setFilter(nextFilter);setActiveTab(tab);};
  const changeFromSidebar=(tab:AdminTab)=>switchTab(tab,{});

  return <Layout><AdminSidebar activeTab={activeTab} onTabChange={changeFromSidebar} adminName={admin.name} stats={{pendingRestaurants:overview.pendingRestaurants||stats.pendingRestaurants}}/>
    <Main><Top><Title>{labels[activeTab].icon} {labels[activeTab].title}</Title><div><Button onClick={()=>void load()}>Làm mới</Button><Button $danger onClick={()=>{logout();navigate('/admin/login');}}>Đăng xuất</Button></div></Top>
      {loading&&activeTab==='overview'?<Empty>Đang tải dữ liệu vận hành...</Empty>:<>
        {activeTab==='overview'&&<Content><AdminOperationsOverview overview={overview} onNavigate={switchTab}/></Content>}
        {activeTab==='analytics'&&<Content><AdminAnalytics/></Content>}
        {activeTab==='restaurants'&&<Content>{restaurants.length?<RestaurantTable key={`restaurant-${refreshKey}`} restaurants={restaurants} onUpdate={load} initialStatusFilter={filter.restaurantStatus||'all'}/>:<Empty>Chưa có dữ liệu nhà hàng.</Empty>}</Content>}
        {activeTab==='customers'&&<Content><CustomerTable key={`customer-${refreshKey}`} customers={customers} onUpdate={load}/></Content>}
        {activeTab==='managers'&&<Content><ManagerTable key={`manager-${refreshKey}`} managers={managers} onUpdate={load}/></Content>}
        {activeTab==='orders'&&<Content><AdminOrders initialDateFilter={filter.orderDate} initialOrderIds={filter.orderIds}/></Content>}
        {activeTab==='deliveries'&&<Content><AdminDeliveryDispatch/></Content>}
        {activeTab==='vouchers'&&<Content><VoucherManagement admin primary="#667eea"/></Content>}
        {activeTab==='support'&&<Content><AdminSupportCenter adminId={admin.id||admin.username||'admin'} initialFilter={filter.supportFilter||'all'} refreshKey={refreshKey}/></Content>}
        {activeTab==='logs'&&<Content><SystemLogs key={`logs-${refreshKey}`} logs={logs}/></Content>}
      </>}
      <Footer>Trung tâm Quản trị © 2026 FoodFast — dữ liệu nghiệp vụ được cập nhật từ backend</Footer>
    </Main><Toaster position="top-right"/>
  </Layout>;
};
export default AdminDashboard;
