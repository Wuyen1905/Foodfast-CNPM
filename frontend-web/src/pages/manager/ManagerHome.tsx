import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import { useAuth } from '@/context/AuthContext';
import {
  createSettlement,
  getSettlementDashboard,
  paySettlement,
  RestaurantSettlement,
  SettlementStatus,
  submitSettlement,
} from '@/api/managerSettlementApi';

const Page=styled.main`min-height:100vh;background:#f4f6fb;padding:28px;@media(max-width:680px){padding:14px;}`;
const Shell=styled.div`max-width:1180px;margin:auto;`;
const Header=styled.header`background:#fff;border-radius:16px;padding:20px 24px;box-shadow:0 3px 16px rgba(35,48,76,.09);display:flex;justify-content:space-between;gap:16px;align-items:center;flex-wrap:wrap;h1{margin:0 0 6px;font-size:28px;}`;
const Logout=styled.button`border:0;border-radius:9px;padding:11px 17px;background:#dc3545;color:#fff;font:inherit;font-weight:700;cursor:pointer;`;
const Filters=styled.section`margin:18px 0;background:#fff;border-radius:14px;padding:16px 20px;display:flex;gap:14px;align-items:end;flex-wrap:wrap;box-shadow:0 2px 12px rgba(35,48,76,.06);label{font-weight:700;display:grid;gap:6px;}input{padding:10px;border:1px solid #cfd6e4;border-radius:8px;font:inherit;}`;
const Action=styled.button<{ $kind?:'primary'|'success'|'danger' }>`border:0;border-radius:8px;padding:10px 15px;background:${p=>p.$kind==='success'?'#168a49':p.$kind==='danger'?'#c93645':'#596ee8'};color:#fff;font:inherit;font-weight:750;cursor:pointer;:disabled{opacity:.48;cursor:not-allowed;}`;
const Stats=styled.div`display:grid;grid-template-columns:repeat(auto-fit,minmax(185px,1fr));gap:14px;margin-bottom:20px;`;
const Stat=styled.article`background:#fff;border-radius:14px;padding:17px;border-left:5px solid #667eea;box-shadow:0 2px 10px rgba(35,48,76,.07);span{color:#697386;}strong{display:block;font-size:25px;margin-top:7px;}small{color:#687386;}`;
const Panel=styled.section`background:#fff;border-radius:15px;padding:20px;margin:18px 0;box-shadow:0 2px 12px rgba(35,48,76,.07);h2{margin:0 0 6px;}p{color:#647084;margin:5px 0 16px;}`;
const PreviewGrid=styled.div`display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;`;
const PreviewCard=styled.article`border:1px solid #dce1eb;border-radius:12px;padding:16px;h3{margin:0 0 9px;}dl{display:grid;grid-template-columns:1fr auto;gap:7px 14px;margin:12px 0;}dt{color:#5c6678;}dd{margin:0;font-weight:700;}`;
const Alert=styled.div`background:#fff4dd;border:1px solid #edc15e;border-radius:10px;padding:11px 13px;margin:8px 0;color:#71520c;`;
const TableWrap=styled.div`overflow:auto;`;
const Table=styled.table`width:100%;border-collapse:collapse;min-width:900px;th,td{text-align:left;padding:12px 10px;border-bottom:1px solid #e6e9ef;vertical-align:top;}th{background:#f6f7fa;}small{display:block;color:#687386;margin-top:4px;}`;
const Badge=styled.span<{ $status:SettlementStatus }>`display:inline-block;padding:5px 9px;border-radius:999px;font-size:13px;font-weight:750;background:${p=>p.$status==='PAID'?'#d9f5e4':p.$status==='PENDING_PAYMENT'?'#fff0c2':'#e5eaff'};color:${p=>p.$status==='PAID'?'#146b3a':p.$status==='PENDING_PAYMENT'?'#785300':'#3448a4'};`;
const Actions=styled.div`display:flex;gap:8px;align-items:center;flex-wrap:wrap;input{width:175px;padding:9px;border:1px solid #ccd3df;border-radius:8px;}`;
const DetailBox=styled.div`margin-top:10px;padding:12px;border-radius:10px;background:#f8f9fc;border:1px solid #e4e8f0;min-width:280px;`;
const Breakdown=styled.dl`display:grid;grid-template-columns:minmax(150px,1fr) auto;gap:7px 16px;margin:0;dt{color:#5f6878;}dd{margin:0;text-align:right;font-weight:700;white-space:nowrap;}& .deduction{color:#b42318;}& .total{padding-top:9px;border-top:1px solid #d7dce6;color:#172033;font-weight:800;}& dd.total{color:#137a43;font-size:17px;}`;
const PlatformNote=styled.div`margin-top:10px;padding:9px 10px;border-radius:8px;background:#eef4ff;color:#35528a;font-size:14px;`;
const OrderLines=styled.ul`list-style:none;padding:10px 0 0;margin:10px 0 0;border-top:1px solid #e1e5ed;li{display:flex;justify-content:space-between;gap:16px;padding:4px 0;}span{color:#5f6878;}strong{white-space:nowrap;}`;

const money=(value:number)=>new Intl.NumberFormat('vi-VN').format(value||0)+' đ';
const statusLabel:Record<SettlementStatus,string>={PENDING_REVIEW:'Chờ kiểm tra',PENDING_PAYMENT:'Chờ thanh toán',PAID:'Đã thanh toán'};
const localDate=(date:Date)=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const startIso=(value:string)=>new Date(`${value}T00:00:00`).toISOString();
const endExclusiveIso=(value:string)=>{const date=new Date(`${value}T00:00:00`);date.setDate(date.getDate()+1);return date.toISOString();};
const errorMessage=(error:any)=>error?.response?.data?.message||error?.message||'Không thể thực hiện thao tác';

const SettlementDetails:React.FC<{item:RestaurantSettlement}>=({item})=><details>
  <summary style={{cursor:'pointer',fontWeight:700}}>Xem chi tiết đối soát</summary>
  <DetailBox>
    <Breakdown>
      <dt>Tiền món</dt><dd>{money(item.foodAmount)}</dd>
      {item.restaurantTaxAmount>0&&<><dt>Thuế thuộc nhà hàng</dt><dd>+ {money(item.restaurantTaxAmount)}</dd></>}
      {item.restaurantDiscountAmount>0&&<><dt>Giảm giá nhà hàng chịu</dt><dd className="deduction">− {money(item.restaurantDiscountAmount)}</dd></>}
      <dt>Hoa hồng FoodFast ({item.commissionRateBps/100}%)</dt><dd className="deduction">− {money(item.commissionAmount)}</dd>
      <dt className="total">Nhà hàng thực nhận</dt><dd className="total">{money(item.payableAmount)}</dd>
    </Breakdown>
    {item.platformDiscountAmount>0&&<PlatformNote>FoodFast chịu {money(item.platformDiscountAmount)} ưu đãi toàn hệ thống; khoản này không trừ vào tiền nhà hàng.</PlatformNote>}
    <OrderLines>{item.orders.map(order=><li key={order.orderId}><span>{order.orderId}</span><strong>{money(order.payableAmount)}</strong></li>)}</OrderLines>
  </DetailBox>
</details>;

const ManagerHome:React.FC=()=>{
  const {user,logout}=useAuth();
  const navigate=useNavigate();
  const today=new Date();
  const thirtyDaysAgo=new Date(today);thirtyDaysAgo.setDate(today.getDate()-29);
  const [fromDate,setFromDate]=useState(localDate(thirtyDaysAgo));
  const [toDate,setToDate]=useState(localDate(today));
  const [data,setData]=useState<Awaited<ReturnType<typeof getSettlementDashboard>>|null>(null);
  const [loading,setLoading]=useState(true);
  const [busy,setBusy]=useState<string>();
  const [references,setReferences]=useState<Record<string,string>>({});
  const permissions=useMemo(()=>new Set((user?.managerPermissions||'').split(',').map(v=>v.trim()).filter(Boolean)),[user?.managerPermissions]);
  const canSettle=permissions.has('RESTAURANT_SETTLEMENT');
  const canPay=permissions.has('PAYMENT_RECONCILIATION');
  const canViewSettlement=canSettle||canPay;

  const load=useCallback(async()=>{
    if(!canViewSettlement){setData(null);setLoading(false);return;}
    setLoading(true);
    try{setData(await getSettlementDashboard(startIso(fromDate),endExclusiveIso(toDate)));}
    catch(error){toast.error(errorMessage(error));}
    finally{setLoading(false);}
  },[canViewSettlement,fromDate,toDate]);
  useEffect(()=>{void load();},[load]);

  const run=async(key:string,action:()=>Promise<unknown>,success:string)=>{
    setBusy(key);
    try{await action();toast.success(success);await load();}
    catch(error){toast.error(errorMessage(error));}
    finally{setBusy(undefined);}
  };

  return <Page><Shell>
    <Header><div><h1>🧑‍💼 Trung tâm quản lý và đối soát</h1><div>Ca hiện tại: <strong>{user?.name}</strong> · Tài khoản: <strong>{user?.username}</strong> · Dữ liệu được tính trực tiếp từ đơn đã giao</div></div><Logout onClick={()=>{logout();navigate('/manager/login');}}>Đăng xuất</Logout></Header>
    {!canViewSettlement&&<Panel><h2>Quyền hiện tại của tài khoản</h2><p>Tài khoản này phụ trách vận hành đơn hàng hoặc hỗ trợ khách hàng, chưa được giao quyền đối soát tài chính. Quản trị viên có thể bổ sung quyền “Đối soát thanh toán” hoặc “Quyết toán nhà hàng”.</p></Panel>}
    {canViewSettlement&&<>
    <Filters><label>Từ ngày<input type="date" value={fromDate} onChange={e=>setFromDate(e.target.value)}/></label><label>Đến ngày<input type="date" value={toDate} onChange={e=>setToDate(e.target.value)}/></label><Action onClick={()=>void load()} disabled={loading}>{loading?'Đang tải…':'Làm mới'}</Action></Filters>
    <Stats>
      <Stat><span>Chờ kiểm tra</span><strong>{data?.pendingReviewCount||0}</strong></Stat>
      <Stat><span>Chờ thanh toán</span><strong>{data?.pendingPaymentCount||0}</strong><small>{money(data?.pendingPaymentAmount||0)}</small></Stat>
      <Stat><span>Đã thanh toán</span><strong>{data?.paidCount||0}</strong><small>{money(data?.paidAmount||0)}</small></Stat>
      <Stat><span>Cảnh báo sai lệch</span><strong>{data?.alerts.length||0}</strong></Stat>
    </Stats>

    <Panel><h2>Đơn chờ đối soát theo nhà hàng</h2><p>Hệ thống tự tính toàn bộ số tiền. Nhân viên chỉ chọn nhà hàng và khoảng thời gian, không được nhập lợi nhuận tùy ý.</p>
      {!loading&&!data?.previews.length&&<div>Không có đơn cần đối soát trong khoảng đã chọn.</div>}
      <PreviewGrid>{data?.previews.map(item=><PreviewCard key={item.restaurantId}>
        <h3>{item.restaurantName}</h3><div>{item.eligibleOrderCount} đơn đủ điều kiện</div>
        <dl><dt>Tiền món</dt><dd>{money(item.foodAmount)}</dd>{item.restaurantTaxAmount>0&&<><dt>Thuế thuộc nhà hàng</dt><dd>+ {money(item.restaurantTaxAmount)}</dd></>}{item.restaurantDiscountAmount>0&&<><dt>Giảm giá nhà hàng chịu</dt><dd>− {money(item.restaurantDiscountAmount)}</dd></>}<dt>Hoa hồng FoodFast</dt><dd>− {money(item.commissionAmount)}</dd><dt>Nhà hàng thực nhận</dt><dd>{money(item.payableAmount)}</dd></dl>
        {item.alerts.map(alert=><Alert key={alert}>⚠️ {alert}</Alert>)}
        {item.eligibleOrderCount>0&&canSettle&&<Action disabled={!!busy} onClick={()=>void run(`create-${item.restaurantId}`,()=>createSettlement({restaurantId:item.restaurantId,periodStart:startIso(fromDate),periodEnd:endExclusiveIso(toDate)}),'Đã lập đợt đối soát')}>{busy===`create-${item.restaurantId}`?'Đang lập…':'Lập đợt đối soát'}</Action>}
      </PreviewCard>)}</PreviewGrid>
    </Panel>

    {!!data?.alerts.length&&<Panel><h2>Cảnh báo cần kiểm tra</h2>{data.alerts.map(alert=><Alert key={alert}>⚠️ {alert}</Alert>)}</Panel>}

    <Panel><h2>Lịch sử đối soát và thanh toán</h2><p>Mỗi đơn chỉ được thuộc một đợt đối soát. Mọi bước đều lưu nhân viên và thời gian thao tác.</p>
      <TableWrap><Table><thead><tr><th>Đợt / Nhà hàng</th><th>Trạng thái</th><th>Số đơn</th><th>Phải trả</th><th>Truy vết</th><th>Thao tác</th></tr></thead><tbody>
        {data?.settlements.map(item=><tr key={item.id}><td><strong>{item.restaurantName}</strong><small>{item.id}</small><SettlementDetails item={item}/></td><td><Badge $status={item.status}>{statusLabel[item.status]}</Badge></td><td>{item.orderCount}</td><td><strong>{money(item.payableAmount)}</strong></td><td><small>Lập bởi: {item.createdBy}<br/>{new Date(item.createdAt).toLocaleString('vi-VN')}</small>{item.submittedAt&&<small>Kiểm tra: {item.submittedBy}<br/>{new Date(item.submittedAt).toLocaleString('vi-VN')}</small>}{item.paidAt&&<small>Thanh toán: {item.paidBy}<br/>{new Date(item.paidAt).toLocaleString('vi-VN')}<br/>Mã: {item.paymentReference}</small>}</td><td><Actions>
          {item.status==='PENDING_REVIEW'&&canSettle&&<Action disabled={!!busy} onClick={()=>void run(`submit-${item.id}`,()=>submitSettlement(item.id),'Đã chuyển sang chờ thanh toán')}>Xác nhận đã kiểm tra</Action>}
          {item.status==='PENDING_PAYMENT'&&canPay&&<><input aria-label={`Mã thanh toán ${item.id}`} placeholder="Mã giao dịch ngân hàng" value={references[item.id]||''} onChange={e=>setReferences({...references,[item.id]:e.target.value})}/><Action $kind="success" disabled={!!busy||!(references[item.id]||'').trim()} onClick={()=>void run(`pay-${item.id}`,()=>paySettlement(item.id,references[item.id]),'Đã ghi nhận thanh toán nhà hàng')}>Đánh dấu đã trả</Action></>}
          {item.status==='PAID'&&<span>Hoàn tất</span>}
        </Actions></td></tr>)}
        {!loading&&!data?.settlements.length&&<tr><td colSpan={6}>Chưa có lịch sử đối soát.</td></tr>}
      </tbody></Table></TableWrap>
    </Panel>
    </>}
  </Shell></Page>;
};

export default ManagerHome;
