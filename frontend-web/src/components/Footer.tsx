import React from 'react';
import styled from 'styled-components';

const Wrap = styled.footer`
  margin-top: 40px;
  padding: 24px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: #fff;
  text-align: center;
  border-top-left-radius: var(--radius-lg);
  border-top-right-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
`;

const SocialIcons = styled.div`
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 12px;
`;

const SocialIcon = styled.a`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  color: #fff;
  text-decoration: none;
  transition: transform 0.2s ease;
  
  &:hover {
    transform: scale(1.1);
    background: rgba(255, 255, 255, 0.3);
  }
`;

const Footer: React.FC = () => {
  return (
    <Wrap>
      ©2026 FoodFast | Đặt món và giao tận nơi
      <SocialIcons>
        <SocialIcon href="#" aria-label="Facebook">
          📘
        </SocialIcon>
        <SocialIcon href="#" aria-label="Instagram">
          📷
        </SocialIcon>
        <SocialIcon href="#" aria-label="Twitter">
          🐦
        </SocialIcon>
      </SocialIcons>
    </Wrap>
  );
};

export default Footer;
