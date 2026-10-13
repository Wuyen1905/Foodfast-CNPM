/**
 * System Logs Component
 * Displays all admin actions and system events with filtering
 */

import React, { useState } from 'react';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { SystemLog } from '../../types/admin';

const Container = styled.div`
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  flex-wrap: wrap;
  gap: 20px;
`;

const Title = styled.h2`
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
`;

const FilterGroup = styled.div`
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
`;

const FilterButton = styled.button<{ $active?: boolean }>`
  padding: 8px 16px;
  border: 2px solid ${props => props.$active ? '#667eea' : '#e1e5e9'};
  background: ${props => props.$active ? '#667eea' : 'white'};
  color: ${props => props.$active ? 'white' : '#666'};
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: #667eea;
    background: ${props => props.$active ? '#5568d3' : '#f8f9fa'};
  }
`;

const LogList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 15px;
`;

const LogCard = styled(motion.div)<{ $severity: string }>`
  background: white;
  border-left: 4px solid ${props => {
    switch (props.$severity) {
      case 'critical': return '#dc3545';
      case 'warning': return '#ffc107';
      case 'info': return '#17a2b8';
      default: return '#6c757d';
    }
  }};
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateX(5px);
  }
`;

const LogHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 15px;
`;

const LogAction = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
`;

const ActionIcon = styled.div<{ $severity: string }>`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: ${props => {
    switch (props.$severity) {
      case 'critical': return '#fee';
      case 'warning': return '#fff9e6';
      case 'info': return '#e7f5ff';
      default: return '#f8f9fa';
    }
  }};
  color: ${props => {
    switch (props.$severity) {
      case 'critical': return '#dc3545';
      case 'warning': return '#ffc107';
      case 'info': return '#17a2b8';
      default: return '#6c757d';
    }
  }};
`;

const ActionDetails = styled.div`
  flex: 1;
`;

const ActionTitle = styled.div`
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 3px;
`;

const ActionSubtitle = styled.div`
  font-size: 13px;
  color: #666;
`;

const LogTimestamp = styled.div`
  font-size: 12px;
  color: #999;
  white-space: nowrap;
`;

const LogDetails = styled.div`
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
`;

const LogMeta = styled.div`
  display: flex;
  gap: 20px;
  margin-top: 12px;
  flex-wrap: wrap;
`;

const MetaItem = styled.div`
  font-size: 12px;
  color: #999;
  
  strong {
    color: #666;
    font-weight: 500;
  }
`;

const SeverityBadge = styled.span<{ $severity: string }>`
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  background: ${props => {
    switch (props.$severity) {
      case 'critical': return '#dc3545';
      case 'warning': return '#ffc107';
      case 'info': return '#17a2b8';
      default: return '#6c757d';
    }
  }};
  color: white;
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: #999;
`;

interface SystemLogsProps {
  logs: SystemLog[];
}

const getActionIcon = (action: SystemLog['action']): string => {
  const icons: Record<SystemLog['action'], string> = {
    restaurant_approved: '✅',
    restaurant_suspended: '🔴',
    restaurant_activated: '🟢',
    customer_suspended: '⛔',
    customer_activated: '✅',
    courier_deactivated: '🛵',
    emergency_override: '⚠️',
    order_status_changed: '📦'
  };
  return icons[action] || '📋';
};

const getActionLabel = (action: SystemLog['action']): string => {
  const labels: Record<SystemLog['action'], string> = {
    restaurant_approved: 'Phê duyệt nhà hàng',
    restaurant_suspended: 'Tạm ngưng nhà hàng',
    restaurant_activated: 'Kích hoạt nhà hàng',
    customer_suspended: 'Tạm ngưng khách hàng',
    customer_activated: 'Kích hoạt khách hàng',
    courier_deactivated: 'Ngừng hoạt động nhân viên giao hàng',
    emergency_override: 'Can thiệp khẩn cấp',
    order_status_changed: 'Thay đổi trạng thái đơn hàng'
  };
  return labels[action] || 'Hành động không xác định';
};

const getSeverityLabel = (severity: SystemLog['severity']): string => ({
  info: 'Thông tin',
  warning: 'Cảnh báo',
  critical: 'Nghiêm trọng',
}[severity] || 'Không xác định');

const getTargetTypeLabel = (targetType: SystemLog['targetType']): string => ({
  restaurant: 'Nhà hàng',
  customer: 'Khách hàng',
  courier: 'Nhân viên giao hàng',
  order: 'Đơn hàng',
}[targetType] || 'Không xác định');

const formatTimestamp = (timestamp: number): { date: string; time: string; relative: string } => {
  const date = new Date(timestamp);
  const now = Date.now();
  const diff = now - timestamp;
  
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  
  let relative = '';
  if (seconds < 60) relative = 'Vừa xong';
  else if (minutes < 60) relative = `${minutes} phút trước`;
  else if (hours < 24) relative = `${hours} giờ trước`;
  else if (days < 7) relative = `${days} ngày trước`;
  else relative = date.toLocaleDateString();
  
  return {
    date: date.toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' }),
    time: date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    relative
  };
};

const formatLogDetails = (details: any): string => {
  if (!details) return '';
  
  // If details is already a string, return it
  if (typeof details === 'string') return details;
  
  // If details is an object, format it as key-value pairs
  if (typeof details === 'object' && details !== null) {
    return Object.entries(details)
      .map(([key, value]) => {
        // Handle special cases for better readability
        if (key === 'orderId' && value) return `Đơn hàng: ${value}`;
        if (key === 'courierId' && value) return `Nhân viên giao hàng: ${value}`;
        if (key === 'deliveryTime' && value) return `Thời gian giao: ${value}`;
        if (key === 'amount' && value) return `Số tiền: ${Number(value).toLocaleString('vi-VN')}₫`;
        if (key === 'timeout' && value) return `Thời gian chờ: ${value} giây`;
        if (key === 'duration' && value) return `Thời gian: ${value}`;
        if (key === 'components' && Array.isArray(value)) return `Thành phần: ${value.join(', ')}`;
        
        // Default formatting
        return `${key}: ${value}`;
      })
      .join(' | ');
  }
  
  // Fallback for other types
  return String(details);
};

const SystemLogs: React.FC<SystemLogsProps> = ({ logs }) => {
  const [severityFilter, setSeverityFilter] = useState<'All' | 'info' | 'warning' | 'critical'>('All');
  const [targetFilter, setTargetFilter] = useState<'All' | 'restaurant' | 'customer' | 'courier' | 'order'>('All');

  // Safe array validation
  const safeLogs = Array.isArray(logs) ? logs : [];
  console.log("[SystemLogs] Safe logs:", safeLogs);

  const filteredLogs = safeLogs.filter(log => {
    if (!log || typeof log !== 'object') return false;
    const matchesSeverity = severityFilter === 'All' || log.severity === severityFilter;
    const matchesTarget = targetFilter === 'All' || log.targetType === targetFilter;
    return matchesSeverity && matchesTarget;
  });

  const severityCounts = {
    info: safeLogs.filter(l => l?.severity === 'info').length,
    warning: safeLogs.filter(l => l?.severity === 'warning').length,
    critical: safeLogs.filter(l => l?.severity === 'critical').length
  };

  const targetCounts = {
    restaurant: safeLogs.filter(l => l?.targetType === 'restaurant').length,
    customer: safeLogs.filter(l => l?.targetType === 'customer').length,
    courier: safeLogs.filter(l => l?.targetType === 'courier').length,
    order: safeLogs.filter(l => l?.targetType === 'order').length
  };

  // Early return for invalid data
  if (!Array.isArray(logs)) {
    console.warn("[SystemLogs] Expected logs to be an array, got:", typeof logs);
    return (
      <Container>
        <Header>
          <Title>Nhật ký hoạt động hệ thống</Title>
        </Header>
        <div style={{ textAlign: 'center', padding: '60px 20px', color: '#999' }}>
          <div style={{ fontSize: '48px', marginBottom: '15px' }}>⚠️</div>
          <div>Không có dữ liệu nhật ký để hiển thị</div>
        </div>
      </Container>
    );
  }

  return (
    <Container>
      <Header>
        <Title>Nhật ký hoạt động hệ thống</Title>
      </Header>
      
      <div>
        <div style={{ fontSize: '13px', fontWeight: 600, color: '#666', marginBottom: '10px' }}>
          LỌC THEO MỨC ĐỘ
        </div>
        <FilterGroup>
          <FilterButton 
            $active={severityFilter === 'All'} 
            onClick={() => setSeverityFilter('All')}
          >
            Tất cả ({safeLogs.length})
          </FilterButton>
          <FilterButton 
            $active={severityFilter === 'info'} 
            onClick={() => setSeverityFilter('info')}
          >
            ℹ️ Thông tin ({severityCounts.info})
          </FilterButton>
          <FilterButton 
            $active={severityFilter === 'warning'} 
            onClick={() => setSeverityFilter('warning')}
          >
            ⚠️ Cảnh báo ({severityCounts.warning})
          </FilterButton>
          <FilterButton 
            $active={severityFilter === 'critical'} 
            onClick={() => setSeverityFilter('critical')}
          >
            🔴 Nghiêm trọng ({severityCounts.critical})
          </FilterButton>
        </FilterGroup>
      </div>
      
      <div style={{ marginTop: '20px' }}>
        <div style={{ fontSize: '13px', fontWeight: 600, color: '#666', marginBottom: '10px' }}>
          LỌC THEO LOẠI ĐỐI TƯỢNG
        </div>
        <FilterGroup>
          <FilterButton 
            $active={targetFilter === 'All'} 
            onClick={() => setTargetFilter('All')}
          >
            Tất cả
          </FilterButton>
          <FilterButton 
            $active={targetFilter === 'restaurant'} 
            onClick={() => setTargetFilter('restaurant')}
          >
            🏪 Nhà hàng ({targetCounts.restaurant})
          </FilterButton>
          <FilterButton
            $active={targetFilter === 'customer'}
            onClick={() => setTargetFilter('customer')}
          >
            👥 Khách hàng ({targetCounts.customer})
          </FilterButton>
          <FilterButton
            $active={targetFilter === 'courier'}
            onClick={() => setTargetFilter('courier')}
          >
            🛵 Nhân viên giao hàng ({targetCounts.courier})
          </FilterButton>
          <FilterButton
            $active={targetFilter === 'order'}
            onClick={() => setTargetFilter('order')}
          >
            📦 Đơn hàng ({targetCounts.order})
          </FilterButton>
        </FilterGroup>
      </div>
      
      <LogList style={{ marginTop: '30px' }}>
        {filteredLogs.length === 0 ? (
          <EmptyState>
            <div style={{ fontSize: '48px', marginBottom: '15px' }}>📋</div>
            <div>Không có nhật ký nào phù hợp với bộ lọc</div>
          </EmptyState>
        ) : (
          filteredLogs.map((log, index) => {
            const timestamp = formatTimestamp(log.timestamp);
            
            return (
              <LogCard
                key={log.id}
                $severity={log.severity}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.05 }}
              >
                <LogHeader>
                  <LogAction>
                    <ActionIcon $severity={log.severity}>
                      {getActionIcon(log.action)}
                    </ActionIcon>
                    <ActionDetails>
                      <ActionTitle>{getActionLabel(log.action)}</ActionTitle>
                      <ActionSubtitle>
                        <strong>{log.targetName}</strong> · thực hiện bởi {log.adminName}
                      </ActionSubtitle>
                    </ActionDetails>
                  </LogAction>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '5px' }}>
                    <SeverityBadge $severity={log.severity}>
                      {getSeverityLabel(log.severity)}
                    </SeverityBadge>
                    <LogTimestamp title={`${timestamp.date} lúc ${timestamp.time}`}>
                      {timestamp.relative}
                    </LogTimestamp>
                  </div>
                </LogHeader>
                
                <LogDetails>{formatLogDetails(log.details)}</LogDetails>
                
                <LogMeta>
                  <MetaItem>
                    <strong>Đối tượng:</strong> {getTargetTypeLabel(log.targetType)} ({log.targetId})
                  </MetaItem>
                  <MetaItem>
                    <strong>Quản trị viên:</strong> {log.adminName} ({log.adminId})
                  </MetaItem>
                  <MetaItem>
                    <strong>Thời gian:</strong> {timestamp.date} lúc {timestamp.time}
                  </MetaItem>
                </LogMeta>
              </LogCard>
            );
          })
        )}
      </LogList>
    </Container>
  );
};

export default SystemLogs;

