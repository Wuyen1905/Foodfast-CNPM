/**
 * Admin Sidebar Component
 * Professional sidebar navigation for admin dashboard
 */

import React from 'react';
import styled from 'styled-components';
import { motion } from 'framer-motion';

const SidebarContainer = styled(motion.div)`
  width: 280px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  height: 100vh;
  padding: 30px 0 0;
  box-sizing: border-box;
  position: fixed;
  left: 0;
  top: 0;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.1);
  z-index: 100;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.45) transparent;

  &::-webkit-scrollbar { width: 7px; }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.4);
    border-radius: 999px;
  }

  @media (max-height: 760px) and (min-width: 769px) {
    padding-top: 18px;
  }
  
  @media (max-width: 768px) {
    width: 100%;
    height: auto;
    position: relative;
    padding: 20px 0;
    overflow: visible;
  }
`;

const Logo = styled.div`
  padding: 0 30px 30px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  margin-bottom: 30px;

  @media (max-height: 760px) and (min-width: 769px) {
    padding-bottom: 14px;
    margin-bottom: 12px;
  }
`;

const LogoTitle = styled.h1`
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 5px 0;
  display: flex;
  align-items: center;
  gap: 10px;

  @media (max-height: 760px) and (min-width: 769px) {
    font-size: 20px;
  }
`;

const LogoSubtitle = styled.p`
  font-size: 12px;
  opacity: 0.8;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 1px;
`;

const NavSection = styled.div`
  margin-bottom: 30px;

  @media (max-height: 760px) and (min-width: 769px) {
    margin-bottom: 10px;
  }
`;

const SectionTitle = styled.div`
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  opacity: 0.6;
  padding: 0 30px;
  margin-bottom: 15px;

  @media (max-height: 760px) and (min-width: 769px) {
    margin-bottom: 7px;
  }
`;

const NavItem = styled(motion.div)<{ $active?: boolean }>`
  padding: 15px 30px;
  display: flex;
  align-items: center;
  gap: 15px;
  cursor: pointer;
  position: relative;
  background: ${props => props.$active ? 'rgba(255, 255, 255, 0.15)' : 'transparent'};
  border-left: 3px solid ${props => props.$active ? '#ffffff' : 'transparent'};

  @media (max-height: 760px) and (min-width: 769px) {
    padding: 9px 30px;
  }
  
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: white;
    opacity: ${props => props.$active ? 1 : 0};
    transition: opacity 0.3s ease;
  }
  
  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
`;

const NavIcon = styled.div`
  font-size: 20px;
  width: 24px;
  text-align: center;
`;

const NavLabel = styled.div`
  font-size: 15px;
  font-weight: 500;
  flex: 1;
`;

const NavBadge = styled.div<{ $color?: string }>`
  background: ${props => props.$color || 'rgba(255, 255, 255, 0.2)'};
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
`;

const UserInfo = styled.div`
  position: relative;
  padding: 20px 30px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(0, 0, 0, 0.1);

  @media (max-height: 760px) and (min-width: 769px) {
    padding: 11px 30px;
  }
  
  @media (max-width: 768px) {
    margin-top: 30px;
  }
`;

const UserName = styled.div`
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 5px;
`;

const UserRole = styled.div`
  font-size: 12px;
  opacity: 0.7;
`;

interface AdminSidebarProps {
  activeTab: 'overview' | 'analytics' | 'restaurants' | 'customers' | 'managers' | 'orders' | 'deliveries' | 'vouchers' | 'support' | 'logs';
  onTabChange: (tab: 'overview' | 'analytics' | 'restaurants' | 'customers' | 'managers' | 'orders' | 'deliveries' | 'vouchers' | 'support' | 'logs') => void;
  adminName: string;
  stats?: {
    pendingRestaurants?: number;
  };
}

const AdminSidebar: React.FC<AdminSidebarProps> = ({ 
  activeTab, 
  onTabChange, 
  adminName,
  stats 
}) => {
  return (
    <SidebarContainer
      initial={{ x: -300 }}
      animate={{ x: 0 }}
      transition={{ type: 'spring', stiffness: 100 }}
    >
      <Logo>
        <LogoTitle>
          <span>🛵</span>
          FoodFast Quản trị
        </LogoTitle>
        <LogoSubtitle>Trung tâm Quản trị</LogoSubtitle>
      </Logo>
      
      <NavSection>
        <SectionTitle>Bảng điều khiển</SectionTitle>
        <NavItem
          $active={activeTab === 'overview'}
          onClick={() => onTabChange('overview')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>📊</NavIcon>
          <NavLabel>Tổng quan</NavLabel>
        </NavItem>

        <NavItem
          $active={activeTab === 'analytics'}
          onClick={() => onTabChange('analytics')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>📈</NavIcon>
          <NavLabel>Báo cáo vận hành</NavLabel>
        </NavItem>
      </NavSection>
      
      <NavSection>
        <SectionTitle>Quản lý</SectionTitle>
        
        <NavItem
          $active={activeTab === 'restaurants'}
          onClick={() => onTabChange('restaurants')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>🏪</NavIcon>
          <NavLabel>Nhà hàng</NavLabel>
          {stats?.pendingRestaurants ? (
            <NavBadge $color="#FF6B6B">{stats.pendingRestaurants}</NavBadge>
          ) : null}
        </NavItem>
        
        <NavItem
          $active={activeTab === 'customers'}
          onClick={() => onTabChange('customers')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>👥</NavIcon>
          <NavLabel>Khách hàng</NavLabel>
        </NavItem>

        <NavItem
          $active={activeTab === 'managers'}
          onClick={() => onTabChange('managers')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>🧑‍💼</NavIcon>
          <NavLabel>Nhân viên quản lý</NavLabel>
        </NavItem>

        <NavItem
          $active={activeTab === 'orders'}
          onClick={() => onTabChange('orders')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>📦</NavIcon>
          <NavLabel>Đơn hàng</NavLabel>
        </NavItem>

        <NavItem
          $active={activeTab === 'deliveries'}
          onClick={() => onTabChange('deliveries')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>🛵</NavIcon>
          <NavLabel>Shipper và giao hàng</NavLabel>
        </NavItem>

        <NavItem
          $active={activeTab === 'vouchers'}
          onClick={() => onTabChange('vouchers')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>🎟️</NavIcon>
          <NavLabel>Mã giảm giá</NavLabel>
        </NavItem>

      </NavSection>
      
      <NavSection>
        <SectionTitle>Hệ thống</SectionTitle>

        <NavItem
          $active={activeTab === 'support'}
          onClick={() => onTabChange('support')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>🎫</NavIcon>
          <NavLabel>Yêu cầu hỗ trợ</NavLabel>
        </NavItem>

        <NavItem
          $active={activeTab === 'logs'}
          onClick={() => onTabChange('logs')}
          whileHover={{ x: 5 }}
          whileTap={{ scale: 0.98 }}
        >
          <NavIcon>📋</NavIcon>
          <NavLabel>Nhật ký hệ thống</NavLabel>
        </NavItem>
      </NavSection>
      
      <UserInfo>
        <UserName>{adminName}</UserName>
        <UserRole>Quản trị viên hệ thống</UserRole>
      </UserInfo>
    </SidebarContainer>
  );
};

export default AdminSidebar;

