import React,{useCallback,useEffect,useMemo,useState} from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {Bar,CartesianGrid,Cell,ComposedChart,Legend,Line,Pie,PieChart,ResponsiveContainer,Tooltip,XAxis,YAxis} from 'recharts';
import {getAdminAnalytics,type AdminAnalyticsReport} from '@/api/adminApi';
import {formatVND} from '@/utils/currency';
import {getStatusLabel} from '@/utils/orderStatus';

const Panel=styled.section`background:#fff;border-radius:15px;padding:24px;box-shadow:0 2px 10px rgba(0,0,0,.08);margin-bottom:22px;`;
const Toolbar=styled.div`display:flex;justify-content:space-between;align-items:center;gap:14px;flex-wrap:wrap;margin-bottom:20px;`;
const Actions=styled.div`display:flex;gap:10px;flex-wrap:wrap;`;
const Select=styled.select`border:1px solid #dfe3ea;border-radius:9px;padding:10px 13px;background:#fff;font:inherit;`;
const Button=styled.button`border:0;border-radius:9px;padding:10px 15px;background:#667eea;color:#fff;font:inherit;font-weight:700;cursor:pointer;&:disabled{opacity:.55;cursor:not-allowed;}`;
const Grid=styled.div`display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-bottom:22px;@media(max-width:1050px){grid-template-columns:repeat(2,1fr);}@media(max-width:580px){grid-template-columns:1fr;}`;
const Kpi=styled.article`border:1px solid #e5e7eb;border-left:5px solid #667eea;border-radius:12px;padding:17px;background:#fff;small{display:block;color:#68707e;margin-bottom:8px;}strong{font-size:24px;color:#20242b;}span{display:block;color:#737985;font-size:12px;margin-top:7px;}`;
const TwoColumn=styled.div`display:grid;grid-template-columns:minmax(0,2fr) minmax(280px,1fr);gap:22px;@media(max-width:950px){grid-template-columns:1fr;}`;
const ChartBox=styled.div`height:340px;`;
const StatusLegend=styled.div`display:flex;justify-content:center;gap:10px 16px;flex-wrap:wrap;font-size:13px;color:#4b5563;margin-top:-8px;`;
const StatusLegendItem=styled.span`display:inline-flex;align-items:center;gap:6px;white-space:nowrap;`;
const StatusDot=styled.i<{$color:string}>`width:10px;height:10px;border-radius:50%;background:${({$color})=>$color};display:inline-block;`;
const TableWrap=styled.div`overflow-x:auto;`;
const Table=styled.table`width:100%;border-collapse:collapse;th,td{text-align:left;padding:13px 12px;border-bottom:1px solid #edf0f4;}th{background:#f8f9fb;color:#59606d;font-size:13px;}tbody tr:hover{background:#fafbff;}`;
const Empty=styled.div`padding:45px;text-align:center;color:#68707e;`;
const COLORS=['#667eea','#22c55e','#f59e0b','#ef4444','#8b5cf6','#06b6d4','#94a3b8'];

const reasonLabels:Record<string,string>={CUSTOMER_ORDERED_BY_MISTAKE:'Khách đặt nhầm',CUSTOMER_WRONG_ADDRESS:'Sai địa chỉ',CUSTOMER_CHANGED_MIND:'Khách đổi ý',CUSTOMER_WAIT_TOO_LONG:'Khách chờ quá lâu',RESTAURANT_OUT_OF_STOCK:'Nhà hàng hết món',RESTAURANT_OVERLOADED:'Nhà hàng quá tải',RESTAURANT_CLOSED:'Nhà hàng đóng cửa',RESTAURANT_CANNOT_DELIVER:'Không thể giao hàng',PAYMENT_EXPIRED:'Thanh toán hết hạn',OTHER:'Lý do khác'};
const percent=(value:number|null)=>value==null?'Chưa đủ dữ liệu':`${Number(value).toLocaleString('vi-VN',{maximumFractionDigits:1})}%`;
const minutes=(value:number|null)=>value==null?'Chưa đủ dữ liệu':`${Number(value).toLocaleString('vi-VN',{maximumFractionDigits:1})} phút`;

const escapeCsv=(value:unknown)=>`"${String(value??'').replace(/"/g,'""')}"`;
const downloadCsv=(report:AdminAnalyticsReport)=>{
  const rows=[
    ['Ngày','Số đơn','Đã giao','Đã hủy','Doanh thu'],
    ...report.trend.map(item=>[item.date,item.orders,item.completed,item.cancelled,item.revenue])
  ];
  rows.push([],['Nhà hàng','Số đơn','Đã giao','Đã hủy','Doanh thu','Tỷ lệ hoàn tất','Tỷ lệ hủy']);
  report.restaurants.forEach(item=>rows.push([item.name,item.orders,item.completed,item.cancelled,item.revenue,item.completionRate,item.cancellationRate]));
  const blob=new Blob([`\uFEFF${rows.map(row=>row.map(escapeCsv).join(',')).join('\n')}`],{type:'text/csv;charset=utf-8'});
  const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=`bao-cao-van-hanh-${report.days}-ngay.csv`;link.click();URL.revokeObjectURL(url);
};

const AdminAnalytics:React.FC=()=>{
  const [days,setDays]=useState<7|30|90>(30);const [report,setReport]=useState<AdminAnalyticsReport|null>(null);const [loading,setLoading]=useState(true);
  const load=useCallback(async()=>{setLoading(true);try{setReport(await getAdminAnalytics(days));}catch{toast.error('Không thể tải báo cáo vận hành');}finally{setLoading(false);}},[days]);
  useEffect(()=>{void load();},[load]);
  const statusData=useMemo(()=>report?Object.entries(report.statusBreakdown).filter(([,value])=>value>0).map(([key,value])=>({name:getStatusLabel(key),value})):[],[report]);

  if(loading&&!report)return <Panel><Empty>Đang tổng hợp dữ liệu vận hành...</Empty></Panel>;
  if(!report)return <Panel><Empty>Chưa có dữ liệu báo cáo.</Empty></Panel>;
  const {summary}=report;
  return <>
    <Panel><Toolbar><div><h2 style={{margin:'0 0 6px'}}>Báo cáo vận hành</h2><span style={{color:'#68707e'}}>Dữ liệu từ {new Date(report.from).toLocaleDateString('vi-VN')} đến {new Date(report.to).toLocaleDateString('vi-VN')}</span></div><Actions><Select aria-label="Khoảng báo cáo" value={days} onChange={e=>setDays(Number(e.target.value) as 7|30|90)}><option value={7}>7 ngày gần nhất</option><option value={30}>30 ngày gần nhất</option><option value={90}>90 ngày gần nhất</option></Select><Button onClick={()=>downloadCsv(report)}>Xuất CSV</Button><Button disabled={loading} onClick={()=>void load()}>{loading?'Đang tải...':'Làm mới'}</Button></Actions></Toolbar>
      <Grid>
        <Kpi><small>Tổng đơn</small><strong>{summary.totalOrders.toLocaleString('vi-VN')}</strong><span>{summary.completedOrders} đơn đã giao</span></Kpi>
        <Kpi><small>Doanh thu thuần</small><strong>{formatVND(summary.netRevenue)}</strong><span>Đơn trung bình {formatVND(summary.averageOrderValue)}</span></Kpi>
        <Kpi><small>Tỷ lệ hoàn tất</small><strong>{percent(summary.completionRate)}</strong><span>Tỷ lệ hủy {percent(summary.cancellationRate)}</span></Kpi>
        <Kpi><small>Giao đúng hẹn</small><strong>{percent(summary.onTimeDeliveryRate)}</strong><span>Chỉ tính đơn có ETA và giờ giao thực tế</span></Kpi>
        <Kpi><small>Xác nhận trung bình</small><strong>{minutes(summary.averageConfirmationMinutes)}</strong><span>Từ khi tạo tới khi nhà hàng xác nhận</span></Kpi>
        <Kpi><small>Chuẩn bị trung bình</small><strong>{minutes(summary.averagePreparationMinutes)}</strong><span>Từ xác nhận tới khi sẵn sàng giao</span></Kpi>
        <Kpi><small>Giao hàng trung bình</small><strong>{minutes(summary.averageDeliveryMinutes)}</strong><span>Từ lúc bắt đầu giao tới lúc hoàn tất</span></Kpi>
        <Kpi><small>Đơn đã hủy</small><strong>{summary.cancelledOrders.toLocaleString('vi-VN')}</strong><span>Theo lý do được ghi nhận bên dưới</span></Kpi>
      </Grid>
      <TwoColumn><div><h3>Xu hướng đơn hàng và doanh thu</h3><ChartBox><ResponsiveContainer width="100%" height="100%"><ComposedChart data={report.trend}><CartesianGrid strokeDasharray="3 3"/><XAxis dataKey="label"/><YAxis yAxisId="orders" allowDecimals={false}/><YAxis yAxisId="revenue" orientation="right" tickFormatter={v=>`${Math.round(Number(v)/1000)}k`}/><Tooltip formatter={(value,name)=>name==='Doanh thu'?formatVND(Number(value)):Number(value).toLocaleString('vi-VN')}/><Legend/><Bar yAxisId="orders" dataKey="orders" name="Số đơn" fill="#667eea" radius={[4,4,0,0]}/><Line yAxisId="revenue" type="monotone" dataKey="revenue" name="Doanh thu" stroke="#ef4444" strokeWidth={3} dot={false}/></ComposedChart></ResponsiveContainer></ChartBox></div><div><h3>Cơ cấu trạng thái</h3><ChartBox><ResponsiveContainer width="100%" height="82%"><PieChart margin={{top:8,right:8,bottom:0,left:8}}><Pie data={statusData} dataKey="value" nameKey="name" innerRadius={62} outerRadius={96} paddingAngle={2} labelLine={false}>{statusData.map((_,index)=><Cell key={index} fill={COLORS[index%COLORS.length]}/>)}</Pie><Tooltip formatter={(value,name)=>[Number(value).toLocaleString('vi-VN'),name]}/></PieChart></ResponsiveContainer><StatusLegend>{statusData.map((item,index)=><StatusLegendItem key={item.name}><StatusDot $color={COLORS[index%COLORS.length]}/>{item.name}: <strong>{item.value}</strong></StatusLegendItem>)}</StatusLegend></ChartBox></div></TwoColumn>
    </Panel>
    <Panel><h2 style={{marginTop:0}}>Hiệu quả theo nhà hàng</h2><TableWrap><Table><thead><tr><th>Nhà hàng</th><th>Số đơn</th><th>Đã giao</th><th>Đã hủy</th><th>Doanh thu thuần</th><th>Hoàn tất</th><th>Tỷ lệ hủy</th></tr></thead><tbody>{report.restaurants.length===0?<tr><td colSpan={7}>Chưa có dữ liệu trong kỳ.</td></tr>:report.restaurants.map(item=><tr key={item.id}><td><strong>{item.name}</strong><br/><small>{item.id}</small></td><td>{item.orders}</td><td>{item.completed}</td><td>{item.cancelled}</td><td>{formatVND(item.revenue)}</td><td>{percent(item.completionRate)}</td><td>{percent(item.cancellationRate)}</td></tr>)}</tbody></Table></TableWrap></Panel>
    <Panel><h2 style={{marginTop:0}}>Nguyên nhân hủy đơn</h2>{report.cancellationReasons.length===0?<p>Không có đơn hủy trong kỳ.</p>:<TableWrap><Table><thead><tr><th>Nguyên nhân</th><th>Số đơn</th><th>Tỷ trọng trên đơn hủy</th></tr></thead><tbody>{report.cancellationReasons.map(item=><tr key={item.code}><td>{reasonLabels[item.code]||item.code}</td><td>{item.count}</td><td>{percent(summary.cancelledOrders?item.count*100/summary.cancelledOrders:0)}</td></tr>)}</tbody></Table></TableWrap>}</Panel>
  </>;
};

export default AdminAnalytics;
