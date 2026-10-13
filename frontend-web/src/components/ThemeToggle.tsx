import React from 'react';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { useTheme } from '@/context/ThemeContext';

const ToggleButton = styled(motion.button)`
  position: relative;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--text);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: all 0.3s ease;

  &:hover {
    transform: scale(1.1);
  }
`;

const ThemeToggle: React.FC = () => {
  const { theme, toggleTheme } = useTheme();

  return (
    <ToggleButton
      onClick={toggleTheme}
      whileHover={{ scale: 1.1 }}
      whileTap={{ scale: 0.95 }}
      animate={{ rotate: theme === 'dark' ? 180 : 0 }}
      transition={{ duration: 0.3 }}
      aria-label={theme === 'light' ? 'Bật chế độ tối' : 'Bật chế độ sáng'}
      title={theme === 'light' ? 'Bật chế độ tối' : 'Bật chế độ sáng'}
    >
      {theme === 'light' ? '🌙' : '☀️'}
    </ToggleButton>
  );
};

export default ThemeToggle;
