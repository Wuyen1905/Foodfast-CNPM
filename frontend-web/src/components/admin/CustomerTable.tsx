import React,{useMemo,useState} from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {getCustomerDetails,reactivateCustomer,suspendCustomer,type AdminCustomer,type AdminCustomerDetails} from '@/api/adminApi';
import {formatVND} from '@/utils/currency';

const Panel=styled.section`background:#fff;border-radius:15px;padding:24px;box-shadow:0 2px 10px rgba(0,0,0,.08);`;
const Toolbar=styled.div`display:grid;grid-template-columns:1fr 220px;gap:12px;margin-bottom:20px;@media(max-width:650px){grid-template-columns:1fr;}`;
const Input=styled.input`border:1px solid #dfe3ea;border-radius:9px;padding:11px 13px;font:inherit;`;
const Select=styled.select`border:1px solid #dfe3ea;border-radius:9px;padding:11px 13px;background:#fff;font:inherit;`;
const TableWrap=styled.div`overflow-x:auto;`;
const Table=styled.table`width:100%;border-collapse:collapse;th,td{text-align:left;padding:13px 11px;border-bottom:1px solid #edf0f4;vertical-align:top;}th{background:#f8f9fb;color:#59606d;font-size:13px;}`;
const Badge=styled.span<{ $active:boolean }>`display:inline-flex;padding:5px 10px;border-radius:999px;font-size:12px;font-weight:800;background:${p=>p.$active?'#d9f4df':'#f9d7db'};`;
const Button=styled.button<{ $tone?:'danger'|'success'|'secondary' }>`border:0;border-radius:8px;padding:9px 13px;font:inherit;font-weight:700;cursor:pointer;margin:2px;background:${p=>({danger:'#dc3545',success:'#198754',secondary:'#eef1f6'}[p.$tone||'secondary'])};color:${p=>p.$tone==='secondary'?'#273142':'#fff'};&:disabled{opacity:.5;}`;
const Overlay=styled.div`position:fixed;inset:0;z-index:1200;background:rgba(15,23,42,.58);display:grid;place-items:center;padding:20px;`;
const Dialog=styled.div`width:min(900px,100%);max-height:90vh;overflow:auto;background:#fff;border-radius:16px;padding:26px;box-shadow:0 18px 55px rgba(0,0,0,.25);`;
const Summary=styled.div`display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;margin:18px 0;@media(max-width:720px){grid-template-columns:repeat(2,1fr);}`;
const SummaryCard=styled.div`background:#f8f9fb;border-radius:10px;padding:13px;strong{display:block;font-size:20px;margin-top:4px;}`;
const Section=styled.section`margin-top:24px;h3{margin:0 0 10px;}`;
const Reason=styled.textarea`width:100%;min-height:90px;padding:12px;border:2px solid #dfe3ea;border-radius:9px;font:inherit;resize:vertical;`;
const Actions=styled.div`display:flex;justify-content:flex-end;gap:9px;flex-wrap:wrap;margin-top:18px;`;

interface Props{customers:AdminCustomer[];onUpdate:()=>void|Promise<void>}
type Mode='view'|'suspend'|'activate'|null;
const when=(value?:number)=>value?new Date(value).toLocaleString('vi-VN'):'Chưa có';

const CustomerTable:React.FC<Props>=({customers,onUpdate})=>{
  const [query,setQuery]=useState('');const [filter,setFilter]=useState('all');
  const [selected,setSelected]=useState<AdminCustomer|null>(null);const [details,setDetails]=useState<AdminCustomerDetails|null>(null);
  const [mode,setMode]=useState<Mode>(null);const [reason,setReason]=useState('');const [loading,setLoading]=useState(false);const [saving,setSaving]=useState(false);
  const filtered=useMemo(()=>customers.filter(item=>{
    const text=`${item.name} ${item.email} ${item.phone}`.toLowerCase();return(!query.trim()||text.includes(query.toLowerCase()))&&(filter==='all'||item.accountStatus===filter);
  }),[customers,query,filter]);
  const openView=async(item:AdminCustomer)=>{setSelected(item);setMode('view');setDetails(null);setLoading(true);try{setDetails(await getCustomerDetails(item.id));}catch{toast.error('Không thể tải hồ sơ khách hàng');}finally{setLoading(false);}};
  const openAction=(item:AdminCustomer,next:Mode)=>{setSelected(item);setMode(next);setReason('');};
  const close=()=>{setSelected(null);setDetails(null);setMode(null);setReason('');};
  const apply=async()=>{if(!selected||!mode||mode==='view'||reason.trim().length<5)return;setSaving(true);try{const ok=mode==='suspend'?await suspendCustomer(selected.id,reason.trim()):await reactivateCustomer(selected.id,reason.trim());if(!ok)throw new Error();toast.success(mode==='suspend'?'Đã tạm ngưng tài khoản':'Đã kích hoạt lại tài khoản');await onUpdate();close();}catch{toast.error('Không thể cập nhật tài khoản');}finally{setSaving(false);}};

  return <Panel><Toolbar><Input aria-label="Tìm khách hàng" placeholder="Tìm theo tên, email hoặc số điện thoại..." value={query} onChange={e=>setQuery(e.target.value)}/><Select aria-label="Lọc tài khoản" value={filter} onChange={e=>setFilter(e.target.value)}><option value="all">Tất cả tài khoản</option><option value="Active">Đang hoạt động</option><option value="Suspended">Tạm ngưng</option></Select></Toolbar>
    <TableWrap><Table><thead><tr><th>Khách hàng</th><th>Điện thoại</th><th>Đơn hàng</th><th>Chi tiêu</th><th>Đăng nhập gần nhất</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
      {filtered.length===0?<tr><td colSpan={7} style={{textAlign:'center',padding:40}}>Không có khách hàng phù hợp</td></tr>:filtered.map(item=><tr key={item.id}><td><strong>{item.name}</strong><br/><small>{item.email}</small></td><td>{item.phone}</td><td>{item.totalOrders}</td><td><strong>{formatVND(item.totalSpend)}</strong></td><td>{when(item.lastLoginAt)}</td><td><Badge $active={item.accountStatus==='Active'}>{item.accountStatus==='Active'?'Hoạt động':'Tạm ngưng'}</Badge>{item.suspensionReason&&<><br/><small>Lý do khóa gần nhất: {item.suspensionReason}</small></>}</td><td><Button onClick={()=>void openView(item)}>Xem hồ sơ</Button>{item.accountStatus==='Active'?<Button $tone="danger" onClick={()=>openAction(item,'suspend')}>Tạm ngưng</Button>:<Button $tone="success" onClick={()=>openAction(item,'activate')}>Kích hoạt</Button>}</td></tr>)}
    </tbody></Table></TableWrap>
    {selected&&mode&&<Overlay onMouseDown={e=>e.target===e.currentTarget&&close()}><Dialog role="dialog" aria-modal="true">
      {mode==='view'?<><h2 style={{marginTop:0}}>Hồ sơ khách hàng: {selected.name}</h2>{loading?<p>Đang tải hồ sơ...</p>:details?<>
        <p><strong>{details.email}</strong> · {details.phone} · <Badge $active={details.accountStatus==='Active'}>{details.accountStatus==='Active'?'Hoạt động':'Tạm ngưng'}</Badge></p>
        <p>Ngày tham gia: {when(details.createdAt)} · Đăng nhập gần nhất: {when(details.lastLoginAt)}</p>{details.suspensionReason&&<p><strong>Lý do khóa gần nhất:</strong> {details.suspensionReason}</p>}
        <Summary><SummaryCard>Tổng đơn<strong>{details.totalOrders}</strong></SummaryCard><SummaryCard>Tổng chi tiêu<strong>{formatVND(details.totalSpend)}</strong></SummaryCard><SummaryCard>Khiếu nại<strong>{details.complaints.length}</strong></SummaryCard><SummaryCard>Địa chỉ đã lưu<strong>{details.addresses.length}</strong></SummaryCard></Summary>
        <Section><h3>Đơn hàng và thanh toán</h3><TableWrap><Table><thead><tr><th>Mã đơn</th><th>Đơn hàng</th><th>Thanh toán</th><th>Tổng</th><th>Thời gian</th></tr></thead><tbody>{details.orders.length===0?<tr><td colSpan={5}>Chưa có đơn hàng</td></tr>:details.orders.map(order=>{const payment=details.payments.find(x=>x.orderId===order.id);return <tr key={order.id}><td>{order.id}</td><td>{order.status}</td><td>{payment?.status||'Chưa rõ'} · {payment?.method||''}</td><td>{formatVND(order.total)}</td><td>{when(order.createdAt)}</td></tr>;})}</tbody></Table></TableWrap></Section>
        <Section><h3>Khiếu nại</h3>{details.complaints.length===0?<p>Chưa có khiếu nại.</p>:details.complaints.map(item=><p key={item.id}><strong>{item.id}</strong> · Đơn {item.orderId} · {item.status} · {item.refundStatus||'Không hoàn tiền'}</p>)}</Section>
        <Section><h3>Sổ địa chỉ</h3>{details.addresses.length===0?<p>Chưa lưu địa chỉ.</p>:details.addresses.map(item=><p key={item.id}><strong>{item.label||'Địa chỉ'}</strong>{item.defaultAddress?' · Mặc định':''}<br/>{item.recipientName} · {item.phone}<br/>{item.addressLine}</p>)}</Section>
      </>:<p>Không tải được dữ liệu.</p>}<Actions><Button onClick={close}>Đóng</Button></Actions></>:<><h2 style={{marginTop:0}}>{mode==='suspend'?'Tạm ngưng':'Kích hoạt lại'} tài khoản</h2><p>Khách hàng: <strong>{selected.name}</strong>. Lý do này được lưu trong nhật ký quản trị và hồ sơ tài khoản.</p><Reason aria-label="Lý do cập nhật tài khoản" placeholder="Nhập lý do cụ thể (ít nhất 5 ký tự)" maxLength={500} value={reason} onChange={e=>setReason(e.target.value)}/><Actions><Button onClick={close}>Hủy</Button><Button $tone={mode==='suspend'?'danger':'success'} disabled={saving||reason.trim().length<5} onClick={()=>void apply()}>{saving?'Đang lưu...':'Xác nhận'}</Button></Actions></>}
    </Dialog></Overlay>}
  </Panel>;
};
export default CustomerTable;
