import React, { useMemo, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {
  createManager,
  setManagerActive,
  updateManager,
  type AdminManager,
  type ManagerAccountInput,
  type ManagerPermission,
} from '@/api/adminApi';

const Panel = styled.section`background:#fff;border-radius:15px;padding:24px;box-shadow:0 2px 10px rgba(0,0,0,.08);`;
const Toolbar = styled.div`display:flex;justify-content:space-between;gap:14px;align-items:center;flex-wrap:wrap;margin-bottom:18px;`;
const Search = styled.input`min-width:280px;flex:1;border:1px solid #d7dce3;border-radius:9px;padding:11px 13px;font:inherit;`;
const Button = styled.button<{ $tone?:'primary'|'danger'|'success' }>`
  border:0;border-radius:8px;padding:9px 13px;font:inherit;font-weight:700;cursor:pointer;
  background:${p=>p.$tone==='danger'?'#dc3545':p.$tone==='success'?'#198754':p.$tone==='primary'?'#667eea':'#edf0f5'};
  color:${p=>p.$tone?'#fff':'#303642'};margin:2px;
  &:disabled{opacity:.55;cursor:not-allowed;}
`;
const TableWrap = styled.div`overflow:auto;`;
const Table = styled.table`width:100%;border-collapse:collapse;min-width:940px;th,td{padding:13px 12px;border-bottom:1px solid #e8ebef;text-align:left;vertical-align:top;}th{background:#f7f8fb;font-size:13px;}small{color:#697386;}`;
const Badge = styled.span<{ $active:boolean }>`display:inline-flex;padding:5px 10px;border-radius:999px;font-size:12px;font-weight:800;background:${p=>p.$active?'#d9f4df':'#f9d7db'};color:${p=>p.$active?'#176b3a':'#9d2430'};`;
const Permission = styled.span`display:inline-block;padding:4px 7px;border-radius:6px;background:#eef0ff;color:#4653a3;font-size:12px;font-weight:700;margin:2px;`;
const Overlay = styled.div`position:fixed;inset:0;background:rgba(20,27,39,.48);display:grid;place-items:center;padding:20px;z-index:1200;`;
const Dialog = styled.div`width:min(680px,100%);max-height:92vh;overflow:auto;background:#fff;border-radius:14px;padding:24px;box-shadow:0 20px 60px rgba(0,0,0,.25);`;
const Grid = styled.div`display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;@media(max-width:620px){grid-template-columns:1fr;}`;
const Field = styled.label`display:grid;gap:6px;font-weight:700;input{border:1px solid #d7dce3;border-radius:8px;padding:10px;font:inherit;}small{font-weight:400;color:#697386;}`;
const PermissionGrid = styled.div`display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;margin-top:10px;@media(max-width:620px){grid-template-columns:1fr;}label{display:flex;gap:8px;align-items:flex-start;border:1px solid #e1e5eb;border-radius:8px;padding:10px;font-weight:600;}`;
const Actions = styled.div`display:flex;justify-content:flex-end;gap:8px;flex-wrap:wrap;margin-top:20px;`;

const permissionLabels:Record<ManagerPermission,string>={
  ORDER_MANAGEMENT:'Theo dõi vận hành đơn hàng',
  CUSTOMER_SUPPORT:'Hỗ trợ khách hàng',
  PAYMENT_RECONCILIATION:'Đối soát thanh toán',
  RESTAURANT_SETTLEMENT:'Quyết toán nhà hàng',
};
const allPermissions=Object.keys(permissionLabels) as ManagerPermission[];
const emptyForm:ManagerAccountInput={username:'',password:'',name:'',email:'',phone:'',permissions:[]};

interface Props { managers:AdminManager[]; onUpdate:()=>void|Promise<void>; }

const ManagerTable:React.FC<Props>=({managers,onUpdate})=>{
  const [query,setQuery]=useState('');
  const [editing,setEditing]=useState<AdminManager|null|undefined>(undefined);
  const [form,setForm]=useState<ManagerAccountInput>(emptyForm);
  const [saving,setSaving]=useState(false);
  const [statusTarget,setStatusTarget]=useState<AdminManager|null>(null);
  const [reason,setReason]=useState('');

  const visible=useMemo(()=>managers.filter(item=>`${item.name} ${item.username} ${item.email||''} ${item.phone||''}`.toLowerCase().includes(query.trim().toLowerCase())),[managers,query]);
  const openCreate=()=>{setEditing(null);setForm({...emptyForm,permissions:['ORDER_MANAGEMENT']});};
  const openEdit=(item:AdminManager)=>{setEditing(item);setForm({username:item.username,password:'',name:item.name,email:item.email||'',phone:item.phone||'',permissions:[...item.permissions]});};
  const close=()=>{setEditing(undefined);setStatusTarget(null);setReason('');};
  const togglePermission=(permission:ManagerPermission)=>setForm(current=>({...current,permissions:current.permissions.includes(permission)?current.permissions.filter(item=>item!==permission):[...current.permissions,permission]}));

  const save=async()=>{
    if(!form.username.trim()||!form.name.trim()||editing===null&&!form.password){toast.error('Vui lòng nhập đủ tên, tên đăng nhập và mật khẩu');return;}
    setSaving(true);
    try{
      const payload={...form,username:form.username.trim(),name:form.name.trim(),email:form.email?.trim()||undefined,phone:form.phone?.trim()||undefined,password:form.password||undefined};
      if(editing) await updateManager(editing.id,payload); else await createManager(payload);
      toast.success(editing?'Đã cập nhật nhân viên quản lý':'Đã tạo nhân viên quản lý');
      await onUpdate();close();
    }catch(error:any){toast.error(error.response?.data?.message||'Không thể lưu tài khoản quản lý');}
    finally{setSaving(false);}
  };

  const applyStatus=async()=>{
    if(!statusTarget)return;
    const nextActive=!statusTarget.active;
    if(!nextActive&&reason.trim().length<5){toast.error('Lý do khóa phải có ít nhất 5 ký tự');return;}
    setSaving(true);
    try{await setManagerActive(statusTarget.id,nextActive,nextActive?'Kích hoạt lại bởi quản trị viên':reason.trim());toast.success(nextActive?'Đã kích hoạt tài khoản':'Đã khóa tài khoản');await onUpdate();close();}
    catch(error:any){toast.error(error.response?.data?.message||'Không thể cập nhật trạng thái');}
    finally{setSaving(false);}
  };

  return <Panel>
    <Toolbar><Search aria-label="Tìm nhân viên quản lý" placeholder="Tìm theo tên, tài khoản, email hoặc số điện thoại..." value={query} onChange={e=>setQuery(e.target.value)}/><Button $tone="primary" onClick={openCreate}>＋ Thêm nhân viên quản lý</Button></Toolbar>
    <TableWrap><Table><thead><tr><th>Nhân viên</th><th>Liên hệ</th><th>Quyền được giao</th><th>Đăng nhập gần nhất</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
      {visible.length===0?<tr><td colSpan={6} style={{textAlign:'center',padding:38}}>Không có nhân viên quản lý phù hợp.</td></tr>:visible.map(item=><tr key={item.id}>
        <td><strong>{item.name}</strong><br/><small>@{item.username}</small></td>
        <td>{item.email||'Chưa có email'}<br/><small>{item.phone||'Chưa có số điện thoại'}</small></td>
        <td>{item.permissions.length?item.permissions.map(permission=><Permission key={permission}>{permissionLabels[permission]}</Permission>):<small>Chưa được giao quyền</small>}</td>
        <td>{item.lastLoginAt?new Date(item.lastLoginAt).toLocaleString('vi-VN'):'Chưa đăng nhập'}</td>
        <td><Badge $active={item.active}>{item.active?'Hoạt động':'Đã khóa'}</Badge>{item.suspensionReason&&<><br/><small>{item.suspensionReason}</small></>}</td>
        <td><Button onClick={()=>openEdit(item)}>Sửa và phân quyền</Button><Button $tone={item.active?'danger':'success'} onClick={()=>{setStatusTarget(item);setReason('');}}>{item.active?'Khóa':'Kích hoạt'}</Button></td>
      </tr>)}
    </tbody></Table></TableWrap>

    {editing!==undefined&&<Overlay onMouseDown={e=>e.target===e.currentTarget&&close()}><Dialog role="dialog" aria-modal="true">
      <h2 style={{marginTop:0}}>{editing?'Sửa nhân viên quản lý':'Thêm nhân viên quản lý'}</h2>
      <Grid>
        <Field>Họ tên<input maxLength={100} value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></Field>
        <Field>Tên đăng nhập<input maxLength={50} value={form.username} onChange={e=>setForm({...form,username:e.target.value})}/></Field>
        <Field>Email<input type="email" maxLength={100} value={form.email||''} onChange={e=>setForm({...form,email:e.target.value})}/></Field>
        <Field>Số điện thoại<input maxLength={10} value={form.phone||''} onChange={e=>setForm({...form,phone:e.target.value.replace(/\D/g,'')})}/></Field>
        <Field style={{gridColumn:'1/-1'}}>Mật khẩu<input type="password" maxLength={64} placeholder={editing?'Để trống nếu không đổi mật khẩu':'Tối thiểu 8 ký tự, gồm chữ và số'} value={form.password||''} onChange={e=>setForm({...form,password:e.target.value})}/><small>{editing?'Mật khẩu hiện tại không được hiển thị.':'Mật khẩu sẽ được băm BCrypt trước khi lưu.'}</small></Field>
      </Grid>
      <h3>Phân quyền nghiệp vụ</h3><PermissionGrid>{allPermissions.map(permission=><label key={permission}><input type="checkbox" checked={form.permissions.includes(permission)} onChange={()=>togglePermission(permission)}/><span>{permissionLabels[permission]}</span></label>)}</PermissionGrid>
      <Actions><Button onClick={close}>Hủy</Button><Button $tone="primary" disabled={saving} onClick={()=>void save()}>{saving?'Đang lưu...':'Lưu tài khoản'}</Button></Actions>
    </Dialog></Overlay>}

    {statusTarget&&<Overlay onMouseDown={e=>e.target===e.currentTarget&&close()}><Dialog role="dialog" aria-modal="true">
      <h2 style={{marginTop:0}}>{statusTarget.active?'Khóa':'Kích hoạt'} tài khoản</h2><p>Nhân viên: <strong>{statusTarget.name}</strong> (@{statusTarget.username}).</p>
      {statusTarget.active&&<Field>Lý do khóa<input maxLength={500} placeholder="Nhập lý do cụ thể, ít nhất 5 ký tự" value={reason} onChange={e=>setReason(e.target.value)}/></Field>}
      <Actions><Button onClick={close}>Hủy</Button><Button $tone={statusTarget.active?'danger':'success'} disabled={saving} onClick={()=>void applyStatus()}>{saving?'Đang lưu...':'Xác nhận'}</Button></Actions>
    </Dialog></Overlay>}
  </Panel>;
};

export default ManagerTable;
