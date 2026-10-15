import React, { useCallback, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {
  assignDelivery,
  createCourier,
  deactivateCourier,
  failDelivery,
  listAdminDeliveryQueue,
  listCouriers,
  startDelivery,
  updateCourier,
  type AdminDeliveryQueueItem,
  type RestaurantCourier,
} from '@/api/deliveryApi';
import { formatVND } from '@/utils/currency';

const Panel = styled.section`background:#fff;border-radius:16px;padding:24px;margin-bottom:22px;box-shadow:0 3px 16px rgba(29,38,66,.08);`;
const Header = styled.div`display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;margin-bottom:18px;h2{margin:0;font-size:22px;}p{margin:5px 0 0;color:#68707e;}`;
const Grid = styled.div`display:grid;grid-template-columns:repeat(auto-fit,minmax(290px,1fr));gap:16px;`;
const Card = styled.article`border:1px solid #e3e7ef;border-radius:13px;padding:17px;background:#fbfcff;h3{margin:0 0 8px;}p{margin:6px 0;color:#4b5563;line-height:1.45;}`;
const Form = styled.form`display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;align-items:end;margin-bottom:18px;`;
const Field = styled.label`display:grid;gap:6px;font-weight:700;color:#343a46;input,select,textarea{font:inherit;border:1px solid #ccd3df;border-radius:9px;padding:10px;background:#fff;}textarea{min-height:70px;resize:vertical;}`;
const Button = styled.button<{ $danger?:boolean;$secondary?:boolean }>`border:0;border-radius:9px;padding:10px 14px;background:${p=>p.$danger?'#dc3545':p.$secondary?'#68707e':'#667eea'};color:#fff;font-weight:800;cursor:pointer;&:disabled{opacity:.5;cursor:not-allowed;}`;
const Actions = styled.div`display:flex;gap:9px;flex-wrap:wrap;margin-top:13px;`;
const Badge = styled.span<{ $waiting?:boolean }>`display:inline-block;border-radius:999px;padding:5px 9px;font-size:12px;font-weight:800;background:${p=>p.$waiting?'#fff3cd':'#e6ecff'};color:${p=>p.$waiting?'#8a6500':'#4054b2'};`;
const Empty = styled.div`padding:24px;border:1px dashed #cbd2df;border-radius:12px;text-align:center;color:#68707e;`;

const AdminDeliveryDispatch: React.FC = () => {
  const [couriers,setCouriers]=useState<RestaurantCourier[]>([]);
  const [queue,setQueue]=useState<AdminDeliveryQueueItem[]>([]);
  const [selection,setSelection]=useState<Record<string,string>>({});
  const [busy,setBusy]=useState(false);
  const [editingCourierId,setEditingCourierId]=useState<string|null>(null);
  const [form,setForm]=useState({name:'',phone:'',vehicleType:'Xe máy',licensePlate:''});

  const load=useCallback(async()=>{
    const [courierData,queueData]=await Promise.all([listCouriers(false),listAdminDeliveryQueue()]);
    setCouriers(courierData);setQueue(queueData);
    setSelection(current=>{
      const next={...current};
      queueData.forEach(item=>{if(item.delivery.courierId)next[item.delivery.id]=item.delivery.courierId;});
      return next;
    });
  },[]);
  useEffect(()=>{void load().catch(error=>toast.error(error?.response?.data?.message||'Không tải được dữ liệu giao hàng'));},[load]);

  const activeCouriers=useMemo(()=>couriers.filter(c=>c.active),[couriers]);
  const busyCourierIds=useMemo(()=>new Set(queue
    .filter(item=>['ASSIGNED','DELIVERING'].includes(item.delivery.status))
    .map(item=>item.delivery.courierId)
    .filter((value):value is string=>Boolean(value))),[queue]);
  const run=async(action:()=>Promise<unknown>,success:string)=>{setBusy(true);try{await action();toast.success(success);await load();}catch(error:any){toast.error(error?.response?.data?.message||error?.message||'Thao tác thất bại');}finally{setBusy(false);}};
  const resetCourierForm=()=>{setEditingCourierId(null);setForm({name:'',phone:'',vehicleType:'Xe máy',licensePlate:''});};
  const submitCourier=(event:React.FormEvent)=>{event.preventDefault();void run(async()=>{
    if(editingCourierId)await updateCourier(editingCourierId,form);
    else await createCourier(form);
    resetCourierForm();
  },editingCourierId?'Đã cập nhật hồ sơ shipper':'Đã thêm hồ sơ shipper');};

  return <>
    <Panel><Header><div><h2>🛵 Hồ sơ shipper</h2><p>Shipper do Admin quản lý tập trung; mỗi người chỉ nhận một đơn đang hoạt động.</p></div><Button onClick={()=>void load()} disabled={busy}>Làm mới</Button></Header>
      <Form onSubmit={submitCourier}>
        <Field>Họ tên<input required value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></Field>
        <Field>Số điện thoại<input required placeholder="0901234567" value={form.phone} onChange={e=>setForm({...form,phone:e.target.value})}/></Field>
        <Field>Phương tiện<input value={form.vehicleType} onChange={e=>setForm({...form,vehicleType:e.target.value})}/></Field>
        <Field>Biển số<input value={form.licensePlate} onChange={e=>setForm({...form,licensePlate:e.target.value})}/></Field>
        <Actions><Button type="submit" disabled={busy}>{editingCourierId?'Lưu thay đổi':'Thêm shipper'}</Button>{editingCourierId&&<Button type="button" $secondary onClick={resetCourierForm}>Hủy sửa</Button>}</Actions>
      </Form>
      {couriers.length===0?<Empty>Chưa có hồ sơ shipper.</Empty>:<Grid>{couriers.map(c=><Card key={c.id}><h3>{c.name}</h3><p>📞 {c.phone}</p><p>🛵 {c.vehicleType||'Chưa cập nhật'} {c.licensePlate?`· ${c.licensePlate}`:''}</p><Badge $waiting={!c.active}>{c.active?'Đang hoạt động':'Đã tạm ngưng'}</Badge>{busyCourierIds.has(c.id)&&<Badge>Đang có chuyến</Badge>}<Actions><Button $secondary disabled={busy} onClick={()=>{setEditingCourierId(c.id);setForm({name:c.name,phone:c.phone,vehicleType:c.vehicleType||'',licensePlate:c.licensePlate||''});}}>Sửa hồ sơ</Button>{c.active?<Button $danger disabled={busy||busyCourierIds.has(c.id)} onClick={()=>void run(()=>deactivateCourier(c.id),'Đã tạm ngưng shipper')}>Tạm ngưng</Button>:<Button disabled={busy} onClick={()=>void run(()=>updateCourier(c.id,{active:true}),'Đã kích hoạt lại shipper')}>Kích hoạt</Button>}</Actions></Card>)}</Grid>}
    </Panel>

    <Panel><Header><div><h2>📦 Hàng đợi phân phối giao hàng</h2><p>Mỗi đơn con sẵn sàng giao có một nhiệm vụ riêng, kể cả các đơn cùng nhóm.</p></div></Header>
      {queue.length===0?<Empty>Không có đơn con nào đang chờ hoặc đang giao.</Empty>:<Grid>{queue.map(item=>{
        const d=item.delivery;const chosen=selection[d.id]||'';
        return <Card key={d.id}><h3>{item.restaurantName||item.restaurantId}</h3>
          <p><strong>{item.orderId}</strong>{item.orderGroupId?` · ${item.orderGroupId} / đơn ${item.groupPosition}`:''}</p>
          <p>{item.customerName} · {item.customerPhone}</p><p>📍 {item.address}</p><p><strong>{formatVND(item.orderTotal)}</strong></p>
          <Badge $waiting={d.status==='WAITING'}>{d.status==='WAITING'?'Chờ phân shipper':d.status==='ASSIGNED'?'Đã phân công':'Đang giao'}</Badge>
          {d.status!=='DELIVERING'&&<><Field style={{marginTop:12}}>Chọn shipper<select value={chosen} onChange={e=>setSelection({...selection,[d.id]:e.target.value})}><option value="">-- Chọn người đang rảnh --</option>{activeCouriers.map(c=>{const courierBusy=busyCourierIds.has(c.id)&&c.id!==d.courierId;return <option key={c.id} value={c.id} disabled={courierBusy}>{c.name} · {c.vehicleType||'Chưa rõ xe'}{courierBusy?' · đang có chuyến':''}</option>;})}</select></Field><Actions><Button disabled={busy||!chosen} onClick={()=>void run(()=>assignDelivery({orderId:item.orderId,courierId:chosen}),d.status==='ASSIGNED'?'Đã đổi shipper':'Đã phân shipper')}>{d.status==='ASSIGNED'?'Đổi shipper':'Phân công'}</Button>{d.status==='ASSIGNED'&&<Button $secondary disabled={busy} onClick={()=>void run(()=>startDelivery(d.id),'Đã bắt đầu giao hàng')}>Bắt đầu giao</Button>}</Actions></>}
          {d.status==='DELIVERING'&&<><p>Shipper: <strong>{d.courierName}</strong></p><Actions><Button $danger disabled={busy} onClick={()=>{const reason=window.prompt('Nhập lý do giao thất bại (ít nhất 1 ký tự)');if(reason?.trim())void run(()=>failDelivery(d.id,reason.trim()),'Đã đưa đơn về hàng đợi');}}>Báo giao thất bại</Button></Actions></>}
        </Card>;
      })}</Grid>}
    </Panel>
  </>;
};

export default AdminDeliveryDispatch;
