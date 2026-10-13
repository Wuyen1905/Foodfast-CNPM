import React,{useCallback,useEffect,useMemo,useState} from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import ComplaintInbox from '@/components/ComplaintInbox';
import SupportConversation from '@/components/SupportConversation';
import type { SupportTicket } from '@/api/supportApi';
import {getAdminSupportQueue,updateAdminSupportTicket,type AdminSupportQueueItem,type SupportDepartment,type SupportPriority} from '@/api/adminApi';

const Panel=styled.section`background:#fff;border-radius:15px;padding:24px;box-shadow:0 2px 10px rgba(0,0,0,.08);margin-bottom:24px;`;
const Toolbar=styled.div`display:grid;grid-template-columns:1fr 220px auto;gap:12px;margin-bottom:18px;@media(max-width:720px){grid-template-columns:1fr;}`;
const Input=styled.input`border:1px solid #dfe3ea;border-radius:9px;padding:11px 13px;font:inherit;`;
const Select=styled.select`border:1px solid #dfe3ea;border-radius:9px;padding:10px;background:#fff;font:inherit;`;
const Button=styled.button<{ $tone?:'primary'|'success'|'secondary' }>`border:0;border-radius:8px;padding:9px 13px;font:inherit;font-weight:700;cursor:pointer;background:${p=>({primary:'#667eea',success:'#198754',secondary:'#eef1f6'}[p.$tone||'secondary'])};color:${p=>p.$tone==='secondary'?'#273142':'#fff'};&:disabled{opacity:.5;}`;
const Card=styled.article<{ $overdue:boolean }>`border:1px solid ${p=>p.$overdue?'#fca5a5':'#e5e7eb'};border-left:5px solid ${p=>p.$overdue?'#dc3545':'#667eea'};border-radius:12px;padding:18px;margin:12px 0;background:${p=>p.$overdue?'#fffafa':'#fff'};`;
const Header=styled.div`display:flex;justify-content:space-between;gap:15px;align-items:flex-start;flex-wrap:wrap;`;
const Chips=styled.div`display:flex;gap:7px;flex-wrap:wrap;margin:8px 0;span{padding:4px 9px;background:#f0f2f6;border-radius:999px;font-size:12px;font-weight:700;}`;
const ActionRow=styled.div`display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin-top:14px;`;
const Note=styled.textarea`width:100%;min-height:78px;border:2px solid #dfe3ea;border-radius:9px;padding:11px;font:inherit;resize:vertical;margin-top:10px;`;
const History=styled.details`margin-top:12px;background:#f8f9fb;border-radius:9px;padding:10px 12px;p{margin:7px 0;font-size:13px;}`;
const ConversationWrap=styled.div`margin-top:14px;`;

export type SupportQueueFilter='all'|'overdue'|'complaint'|'refund'|'ticket';
interface Props{adminId:string;initialFilter?:SupportQueueFilter;refreshKey?:number;}
const priorityLabel:Record<SupportPriority,string>={LOW:'Thấp',NORMAL:'Bình thường',HIGH:'Cao',URGENT:'Khẩn cấp'};
const deptLabel:Record<SupportDepartment,string>={CUSTOMER_SERVICE:'Chăm sóc khách hàng',OPERATIONS:'Vận hành',FINANCE:'Tài chính'};
const statusLabel:Record<string,string>={OPEN:'Đang chờ tiếp nhận',IN_PROGRESS:'Đang trao đổi',RESOLVED:'Đã hoàn tất',REVIEWING:'Đang xem xét',REJECTED:'Đã từ chối'};
const when=(value?:number)=>value?new Date(value).toLocaleString('vi-VN'):'Chưa đặt';

const AdminSupportCenter:React.FC<Props>=({adminId,initialFilter='all',refreshKey=0})=>{
  const [items,setItems]=useState<AdminSupportQueueItem[]>([]);const [filter,setFilter]=useState<SupportQueueFilter>(initialFilter);const [query,setQuery]=useState('');const [loading,setLoading]=useState(true);
  const [notes,setNotes]=useState<Record<string,string>>({});const [saving,setSaving]=useState<string|null>(null);
  const [conversationId,setConversationId]=useState<string|null>(null);
  const load=useCallback(async()=>{setLoading(true);try{setItems(await getAdminSupportQueue());}catch{toast.error('Không thể tải hàng đợi hỗ trợ');}finally{setLoading(false);}},[]);
  useEffect(()=>{setFilter(initialFilter);},[initialFilter]);useEffect(()=>{void load();},[load,refreshKey]);
  const visible=useMemo(()=>items.filter(item=>{
    const matches=filter==='all'||(filter==='overdue'&&item.overdue)||(filter==='complaint'&&item.type==='COMPLAINT')||(filter==='refund'&&item.type==='COMPLAINT'&&item.refundStatus==='PENDING')||(filter==='ticket'&&item.type==='SUPPORT_TICKET');
    const text=`${item.id} ${item.orderId||''} ${item.restaurantId||''} ${item.summary}`.toLowerCase();return matches&&(!query.trim()||text.includes(query.trim().toLowerCase()));
  }),[items,filter,query]);
  const update=async(item:AdminSupportQueueItem,request:Parameters<typeof updateAdminSupportTicket>[1])=>{setSaving(item.id);try{await updateAdminSupportTicket(item.id,request);toast.success('Đã cập nhật phiếu hỗ trợ');await load();}catch(error:any){toast.error(error?.response?.data?.message||error?.response?.data?.error||'Không thể cập nhật phiếu');}finally{setSaving(null);}};

  return <>
    <Panel><h2 style={{marginTop:0}}>Hàng đợi hỗ trợ và hoàn tiền</h2><p style={{color:'#68707e'}}>Ưu tiên yêu cầu quá hạn, phân công người xử lý và lưu toàn bộ lịch sử thao tác.</p><p style={{color:'#68707e',fontSize:14}}><strong>Thời gian cam kết:</strong> hoàn tiền/tài chính 4 giờ · khiếu nại 8 giờ · hỗ trợ thông thường 24 giờ.</p>
      <Toolbar><Input aria-label="Tìm yêu cầu hỗ trợ" placeholder="Tìm theo mã phiếu, đơn, nhà hàng hoặc nội dung..." value={query} onChange={e=>setQuery(e.target.value)}/><Select aria-label="Lọc hàng đợi" value={filter} onChange={e=>setFilter(e.target.value as SupportQueueFilter)}><option value="all">Tất cả yêu cầu</option><option value="overdue">Yêu cầu quá hạn</option><option value="ticket">Phiếu hỗ trợ</option><option value="complaint">Khiếu nại</option><option value="refund">Chờ hoàn tiền</option></Select><Button onClick={()=>void load()}>Làm mới</Button></Toolbar>
      {loading?<p>Đang tải hàng đợi...</p>:visible.length===0?<p>Không có yêu cầu phù hợp.</p>:visible.map(item=><Card key={`${item.type}-${item.id}`} $overdue={item.overdue}><Header><div><strong>{item.id}</strong> · {item.type==='COMPLAINT'?'Khiếu nại':'Phiếu hỗ trợ'}{item.orderId&&<> · Đơn {item.orderId}</>}{item.customerName&&<> · {item.customerName}</>}<p>{item.summary}</p></div><div>{item.overdue?<strong style={{color:'#dc3545'}}>Đã quá hạn xử lý</strong>:<span>Hạn xử lý: {when(item.slaDueAt)}</span>}</div></Header>
        <Chips><span>{priorityLabel[item.priority]||item.priority}</span><span>{deptLabel[item.department]||item.department}</span><span>{statusLabel[item.status]||item.status}</span>{item.refundStatus&&<span>Hoàn tiền: {item.refundStatus}</span>}<span>{item.assignedName?`Phụ trách: ${item.assignedName}`:'Chưa phân công'}</span></Chips>
        {item.type==='SUPPORT_TICKET'&&<><ActionRow><Button $tone="primary" onClick={()=>setConversationId(current=>current===item.id?null:item.id)}>{conversationId===item.id?'Ẩn trao đổi':'Trao đổi với khách'}</Button>{item.status!=='RESOLVED'&&<><Select aria-label={`Mức ưu tiên ${item.id}`} value={item.priority} onChange={e=>void update(item,{priority:e.target.value as SupportPriority})}><option value="LOW">Thấp</option><option value="NORMAL">Bình thường</option><option value="HIGH">Cao</option><option value="URGENT">Khẩn cấp</option></Select><Select aria-label={`Bộ phận ${item.id}`} value={item.department} onChange={e=>void update(item,{department:e.target.value as SupportDepartment})}><option value="CUSTOMER_SERVICE">Chăm sóc khách hàng</option><option value="OPERATIONS">Vận hành</option><option value="FINANCE">Tài chính</option></Select><Button $tone="primary" disabled={saving===item.id} onClick={()=>void update(item,{assignToMe:true})}>Nhận xử lý</Button>{item.status==='OPEN'&&<Button $tone="primary" disabled={saving===item.id} onClick={()=>void update(item,{status:'IN_PROGRESS'})}>Bắt đầu xử lý</Button>}</>}</ActionRow>{item.status!=='RESOLVED'&&<><Note aria-label={`Kết quả xử lý ${item.id}`} placeholder="Nhập kết quả xử lý trước khi hoàn tất (ít nhất 5 ký tự)" value={notes[item.id]||''} onChange={e=>setNotes(current=>({...current,[item.id]:e.target.value}))}/><ActionRow><Button $tone="success" disabled={saving===item.id||(notes[item.id]||'').trim().length<5} onClick={()=>void update(item,{status:'RESOLVED',note:(notes[item.id]||'').trim()})}>Hoàn tất phiếu</Button></ActionRow></>}{conversationId===item.id&&<ConversationWrap><SupportConversation ticket={{id:item.id,orderId:item.orderId,restaurantId:item.restaurantId,customerName:item.customerName,intent:'OTHER',message:item.summary,target:'ADMIN',status:item.status as SupportTicket['status'],createdAt:item.createdAt} as SupportTicket} viewerRole="ADMIN" onMessageSent={()=>void load()}/></ConversationWrap>}</>}
        {item.type==='COMPLAINT'&&<p><strong>Xử lý nghiệp vụ:</strong> dùng khu vực khiếu nại bên dưới để duyệt/từ chối và thực hiện hoàn tiền.</p>}
        <History><summary>Nhật ký ({item.history?.length||0})</summary>{!item.history?.length?<p>Chưa có nhật ký.</p>:item.history.map((h,index)=><p key={h.id||index}>{when(h.createdAt)} · {h.action}{h.note?` — ${h.note}`:''}</p>)}</History>
      </Card>)}
    </Panel>
    <ComplaintInbox role="ADMIN" viewerId={adminId}/>
  </>;
};
export default AdminSupportCenter;
