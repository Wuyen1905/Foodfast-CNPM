import React, { useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {
  getRestaurantDetails,
  updateRestaurantLifecycle,
  type AdminRestaurant,
  type AdminRestaurantDetails,
  type RestaurantLifecycleStatus
} from '@/api/adminApi';
import { formatVND } from '@/utils/currency';

const Panel = styled.section`background:#fff;border-radius:15px;padding:24px;box-shadow:0 2px 10px rgba(0,0,0,.08);`;
const Toolbar = styled.div`display:grid;grid-template-columns:1fr 220px auto;gap:12px;margin-bottom:20px;@media(max-width:720px){grid-template-columns:1fr;}`;
const Input = styled.input`border:1px solid #dfe3ea;border-radius:9px;padding:11px 13px;font:inherit;`;
const Select = styled.select`border:1px solid #dfe3ea;border-radius:9px;padding:11px 13px;background:#fff;font:inherit;`;
const Button = styled.button<{ $tone?: 'danger'|'success'|'warning'|'secondary' }>`border:0;border-radius:8px;padding:9px 13px;font:inherit;font-weight:700;cursor:pointer;background:${p=>({danger:'#dc3545',success:'#198754',warning:'#f59e0b',secondary:'#eef1f6'}[p.$tone||'secondary'])};color:${p=>p.$tone==='secondary'?'#273142':'#fff'};&:disabled{opacity:.5;cursor:not-allowed;}`;
const TableWrap = styled.div`overflow-x:auto;`;
const Table = styled.table`width:100%;border-collapse:collapse;th,td{text-align:left;padding:13px 11px;border-bottom:1px solid #edf0f4;vertical-align:top;}th{background:#f8f9fb;color:#59606d;font-size:13px;}tbody tr:hover{background:#fafbff;}`;
const Badge = styled.span<{ $status:RestaurantLifecycleStatus }>`display:inline-flex;padding:5px 10px;border-radius:999px;font-size:12px;font-weight:800;background:${p=>({ACTIVE:'#d9f4df',PENDING:'#fff3cd',SUSPENDED:'#ffe2cc',CLOSED:'#eceff3'}[p.$status])};color:#30343b;`;
const Actions = styled.div`display:flex;gap:7px;flex-wrap:wrap;`;
const Overlay = styled.div`position:fixed;inset:0;z-index:1200;background:rgba(15,23,42,.58);display:grid;place-items:center;padding:20px;`;
const Dialog = styled.div`width:min(850px,100%);max-height:90vh;overflow:auto;background:#fff;border-radius:16px;padding:26px;box-shadow:0 18px 55px rgba(0,0,0,.25);`;
const Metrics = styled.div`display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;margin:18px 0;@media(max-width:720px){grid-template-columns:repeat(2,1fr);}`;
const Metric = styled.div`background:#f8f9fb;border-radius:10px;padding:14px;strong{display:block;font-size:21px;margin-top:4px;}`;
const Reason = styled.textarea`width:100%;min-height:95px;padding:12px;border:2px solid #dfe3ea;border-radius:9px;resize:vertical;font:inherit;`;
const DialogActions = styled.div`display:flex;justify-content:flex-end;gap:9px;flex-wrap:wrap;margin-top:18px;`;

const statusLabels: Record<RestaurantLifecycleStatus,string> = {
  PENDING:'Chờ duyệt', ACTIVE:'Đang hoạt động', SUSPENDED:'Tạm ngưng', CLOSED:'Đã đóng'
};

const transitions: Record<RestaurantLifecycleStatus, Array<{status:RestaurantLifecycleStatus;label:string;tone:'danger'|'success'|'warning'}>> = {
  PENDING:[{status:'ACTIVE',label:'Phê duyệt',tone:'success'},{status:'CLOSED',label:'Từ chối',tone:'danger'}],
  ACTIVE:[{status:'SUSPENDED',label:'Tạm ngưng',tone:'warning'},{status:'CLOSED',label:'Đóng nhà hàng',tone:'danger'}],
  SUSPENDED:[{status:'ACTIVE',label:'Kích hoạt lại',tone:'success'},{status:'CLOSED',label:'Đóng nhà hàng',tone:'danger'}],
  CLOSED:[{status:'PENDING',label:'Mở lại để duyệt',tone:'warning'}]
};

interface Props { restaurants: AdminRestaurant[]; onUpdate:()=>void|Promise<void>; initialStatusFilter?:RestaurantLifecycleStatus|'all'; }

const RestaurantTable:React.FC<Props> = ({restaurants,onUpdate,initialStatusFilter='all'}) => {
  const [query,setQuery]=useState('');
  const [status,setStatus]=useState<RestaurantLifecycleStatus|'all'>(initialStatusFilter);
  const [selected,setSelected]=useState<AdminRestaurant|null>(null);
  const [details,setDetails]=useState<AdminRestaurantDetails|null>(null);
  const [loadingDetails,setLoadingDetails]=useState(false);
  const [target,setTarget]=useState<RestaurantLifecycleStatus|null>(null);
  const [reason,setReason]=useState('');
  const [saving,setSaving]=useState(false);

  useEffect(()=>setStatus(initialStatusFilter),[initialStatusFilter]);
  const filtered=useMemo(()=>restaurants.filter(item=>{
    const text=`${item.name} ${item.ownerName} ${item.category} ${item.location}`.toLowerCase();
    return (!query.trim()||text.includes(query.trim().toLowerCase()))&&(status==='all'||item.status===status);
  }),[restaurants,query,status]);

  const openDetails=async(item:AdminRestaurant)=>{
    setSelected(item);setDetails(null);setTarget(null);setReason('');setLoadingDetails(true);
    try{setDetails(await getRestaurantDetails(item.id));}catch{toast.error('Không thể tải hồ sơ vận hành nhà hàng');}
    finally{setLoadingDetails(false);}
  };
  const close=()=>{setSelected(null);setDetails(null);setTarget(null);setReason('');};
  const apply=async()=>{
    if(!selected||!target||reason.trim().length<5)return;
    setSaving(true);
    try{await updateRestaurantLifecycle(selected.id,target,reason.trim());toast.success(`Đã chuyển nhà hàng sang “${statusLabels[target]}”`);await onUpdate();close();}
    catch(error:any){toast.error(error?.response?.data?.message||error?.response?.data?.error||'Không thể đổi trạng thái nhà hàng');}
    finally{setSaving(false);}
  };

  return <Panel>
    <Toolbar>
      <Input aria-label="Tìm nhà hàng" placeholder="Tìm theo tên, chủ sở hữu, loại hình, địa điểm..." value={query} onChange={e=>setQuery(e.target.value)}/>
      <Select aria-label="Lọc vòng đời nhà hàng" value={status} onChange={e=>setStatus(e.target.value as RestaurantLifecycleStatus|'all')}>
        <option value="all">Tất cả trạng thái</option>{Object.entries(statusLabels).map(([value,label])=><option key={value} value={value}>{label}</option>)}
      </Select>
      <Button onClick={()=>void onUpdate()}>Làm mới</Button>
    </Toolbar>
    <TableWrap><Table><thead><tr><th>Nhà hàng</th><th>Chủ sở hữu</th><th>Đơn / doanh thu</th><th>Giao hàng</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
      <tbody>{filtered.length===0?<tr><td colSpan={6} style={{textAlign:'center',padding:40}}>Không có nhà hàng phù hợp</td></tr>:filtered.map(item=><tr key={item.id}>
        <td><strong>{item.name}</strong><br/><small>{item.category} · {item.location}</small></td><td>{item.ownerName}</td>
        <td>{item.totalOrders} đơn<br/><strong>{formatVND(item.totalRevenue)}</strong></td><td>{item.courierCount} nhân viên</td>
        <td><Badge $status={item.status}>{statusLabels[item.status]}</Badge>{item.statusReason&&<><br/><small>{item.statusReason}</small></>}</td>
        <td><Actions><Button onClick={()=>void openDetails(item)}>Xem hồ sơ</Button>{transitions[item.status].map(action=><Button key={action.status} $tone={action.tone} onClick={()=>{setSelected(item);setTarget(action.status);setReason('');}}>{action.label}</Button>)}</Actions></td>
      </tr>)}</tbody></Table></TableWrap>

    {selected&&<Overlay onMouseDown={e=>e.target===e.currentTarget&&close()}><Dialog role="dialog" aria-modal="true">
      <h2 style={{marginTop:0}}>{target?`${transitions[selected.status].find(x=>x.status===target)?.label}: ${selected.name}`:`Hồ sơ vận hành: ${selected.name}`}</h2>
      {target?<>
        <p>Chuyển từ <strong>{statusLabels[selected.status]}</strong> sang <strong>{statusLabels[target]}</strong>. Lý do sẽ được lưu trong nhật ký quản trị.</p>
        <Reason aria-label="Lý do thay đổi trạng thái" placeholder="Nhập lý do cụ thể (ít nhất 5 ký tự)" maxLength={500} value={reason} onChange={e=>setReason(e.target.value)}/>
        <DialogActions><Button onClick={()=>{setTarget(null);setReason('');}}>Quay lại</Button><Button $tone={target==='ACTIVE'?'success':target==='SUSPENDED'||target==='PENDING'?'warning':'danger'} disabled={saving||reason.trim().length<5} onClick={()=>void apply()}>{saving?'Đang lưu...':'Xác nhận'}</Button></DialogActions>
      </>:loadingDetails?<p>Đang tải hồ sơ...</p>:details?<>
        <p><Badge $status={details.status}>{statusLabels[details.status]}</Badge>{details.statusReason?` · Lý do gần nhất: ${details.statusReason}`:''}</p>
        <Metrics>
          <Metric>Tổng đơn<strong>{details.totalOrders}</strong></Metric><Metric>Doanh thu<strong>{formatVND(details.totalRevenue)}</strong></Metric>
          <Metric>Tỷ lệ hủy<strong>{details.cancellationRate.toFixed(1)}%</strong></Metric><Metric>Giao thất bại<strong>{details.failedDeliveries}</strong></Metric>
          <Metric>Đã giao<strong>{details.deliveredOrders}</strong></Metric><Metric>Đơn hủy<strong>{details.cancelledOrders}</strong></Metric>
          <Metric>Khiếu nại mở<strong>{details.openComplaintCount}</strong></Metric><Metric>Tổng khiếu nại<strong>{details.complaintCount}</strong></Metric>
        </Metrics>
        <h3>Đơn gần đây</h3><TableWrap><Table><thead><tr><th>Mã đơn</th><th>Trạng thái</th><th>Thanh toán</th><th>Tổng</th><th>Thời gian</th></tr></thead><tbody>{details.recentOrders.length===0?<tr><td colSpan={5}>Chưa có đơn hàng</td></tr>:details.recentOrders.map(order=><tr key={order.id}><td>{order.id}</td><td>{order.status}</td><td>{order.paymentStatus||'Chưa rõ'}</td><td>{formatVND(order.total)}</td><td>{new Date(order.createdAt).toLocaleString('vi-VN')}</td></tr>)}</tbody></Table></TableWrap>
        <DialogActions><Button onClick={close}>Đóng</Button></DialogActions>
      </>:<p>Không tải được dữ liệu.</p>}
    </Dialog></Overlay>}
  </Panel>;
};

export default RestaurantTable;
