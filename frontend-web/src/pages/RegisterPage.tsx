import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "@/context";
import styled from "styled-components";
import { motion } from "framer-motion";
import toast from "react-hot-toast";

// Styled Components - Reusing Login page design system
const RegisterContainer = styled.div`
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
`;

const RegisterCard = styled(motion.div)`
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 480px;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #FF6600 0%, #FF8C00 100%);
  }
`;

const RegisterTitle = styled.h2`
  text-align: center;
  margin: 0 0 32px 0;
  color: #333;
  font-size: 28px;
  font-weight: 700;
`;

const FormGroup = styled.div`
  margin-bottom: 24px;
  position: relative;
`;

const InputContainer = styled.div`
  position: relative;
  display: flex;
  align-items: center;
`;

const InputIcon = styled.div`
  position: absolute;
  left: 16px;
  color: #666;
  font-size: 18px;
  z-index: 1;
`;

const StyledInput = styled.input<{ $hasError?: boolean }>`
  width: 100%;
  padding: 16px 16px 16px 48px;
  border: 2px solid ${props => props.$hasError ? '#dc3545' : '#e1e5e9'};
  border-radius: 12px;
  font-size: 16px;
  transition: all 0.3s ease;
  background: #f8f9fa;
  
  &:focus {
    outline: none;
    border-color: ${props => props.$hasError ? '#dc3545' : '#FF6600'};
    background: white;
    box-shadow: 0 0 0 3px ${props => props.$hasError ? 'rgba(220, 53, 69, 0.1)' : 'rgba(255, 102, 0, 0.1)'};
  }
  
  &::placeholder {
    color: #999;
  }
`;

const ErrorMessage = styled(motion.div)`
  color: #dc3545;
  font-size: 14px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
`;

const RegisterButton = styled(motion.button)`
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #FF6600 0%, #FF8C00 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 8px;
  
  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(255, 102, 0, 0.3);
  }
  
  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }
`;

const LoginLink = styled.div`
  text-align: center;
  margin-top: 24px;
  color: #666;
  font-size: 14px;
  
  a {
    color: #FF6600;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.3s ease;
    
    &:hover {
      color: #FF8C00;
    }
  }
`;

const BackToHome = styled(motion.div)`
  text-align: center;
  margin-top: 24px;
  
  a {
    color: #FF6600;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.3s ease;
    
    &:hover {
      color: #FF8C00;
    }
  }
`;

// Form validation types
interface FormErrors {
  fullName?: string;
  email?: string;
  phone?: string;
  password?: string;
  confirmPassword?: string;
}

interface RegisterFormData {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
}

const RegisterPage: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState<RegisterFormData>({
    fullName: "",
    email: "",
    phone: "",
    password: "",
    confirmPassword: ""
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [busy, setBusy] = useState(false);

  // Validation functions
  const validateEmail = (email: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const validatePhone = (phone: string): boolean => {
    const digits = phone.replace(/\D/g, '');
    return /^0\d{9}$/.test(digits) || /^84\d{9}$/.test(digits);
  };

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Full name validation
    if (!formData.fullName.trim()) {
      newErrors.fullName = "Họ tên là bắt buộc";
    } else if (formData.fullName.trim().length < 2) {
      newErrors.fullName = "Họ tên phải có ít nhất 2 ký tự";
    }

    // Email validation
    if (!formData.email.trim()) {
      newErrors.email = "Email là bắt buộc";
    } else if (!validateEmail(formData.email)) {
      newErrors.email = "Email không hợp lệ";
    }

    if (!formData.phone.trim()) {
      newErrors.phone = "Số điện thoại là bắt buộc";
    } else if (!validatePhone(formData.phone)) {
      newErrors.phone = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc 84xxxxxxxxx";
    }

    // Password validation
    if (!formData.password) {
      newErrors.password = "Mật khẩu là bắt buộc";
    } else if (formData.password.length < 6) {
      newErrors.password = "Mật khẩu phải có ít nhất 6 ký tự";
    }

    // Confirm password validation
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = "Xác nhận mật khẩu là bắt buộc";
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = "Mật khẩu xác nhận không khớp";
    }

    setErrors(newErrors);

    const isValid = Object.keys(newErrors).length === 0;

    return isValid;
  };

  const handleInputChange = (field: keyof RegisterFormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate form first
    const isValid = validateForm();

    if (!isValid) {
      toast.error("Vui lòng kiểm tra lại thông tin");
      return;
    }

    setBusy(true);

    try {
      const result = await register({
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        password: formData.password
      });

      if (result.ok) {
        toast.success("🎉 Đăng ký thành công! Chào mừng đến với FoodFast.");
        navigate("/login", {
          state: { registeredIdentity: formData.email.trim().toLowerCase() }
        });
      } else {
        console.error("❌ [RegisterPage] Registration failed:", result.message);
        toast.error(result.message || "Đăng ký thất bại");
      }
    } catch (error) {
      console.error("💥 [RegisterPage] Registration error:", error);
      toast.error("Có lỗi xảy ra, vui lòng thử lại");
    } finally {
      setBusy(false);
    }
  };

  const isFormValid = () => {
    const hasValidFields = formData.fullName.trim() &&
           formData.email.trim() &&
           validateEmail(formData.email) &&
           validatePhone(formData.phone) &&
           formData.password.length >= 6 &&
           formData.confirmPassword === formData.password;
    
    const hasNoErrors = Object.values(errors).every(error => !error);

    return hasValidFields && hasNoErrors;
  };

  // Auto-format phone number as user types
  const formatPhoneNumber = (value: string) => {
    // Remove all non-digits
    const digits = value.replace(/\D/g, '');
    // Limit to 11 digits
    const limited = digits.slice(0, 11);
    return limited;
  };

  return (
    <RegisterContainer>
      <RegisterCard
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
      >
        <RegisterTitle>Đăng ký tài khoản</RegisterTitle>
        
        <form onSubmit={handleSubmit}>
          <FormGroup>
            <InputContainer>
              <InputIcon>👤</InputIcon>
              <StyledInput
                type="text"
                placeholder="Họ và tên"
                value={formData.fullName}
                onChange={(e) => handleInputChange('fullName', e.target.value)}
                disabled={busy}
                $hasError={!!errors.fullName}
              />
            </InputContainer>
            {errors.fullName && (
              <ErrorMessage
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
              >
                ⚠️ {errors.fullName}
              </ErrorMessage>
            )}
          </FormGroup>

          <FormGroup>
            <InputContainer>
              <InputIcon>📱</InputIcon>
              <StyledInput
                type="tel"
                inputMode="numeric"
                autoComplete="tel"
                placeholder="Số điện thoại"
                value={formData.phone}
                onChange={(e) => handleInputChange('phone', formatPhoneNumber(e.target.value))}
                disabled={busy}
                $hasError={!!errors.phone}
              />
            </InputContainer>
            {errors.phone && (
              <ErrorMessage
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
              >
                ⚠️ {errors.phone}
              </ErrorMessage>
            )}
          </FormGroup>

          <FormGroup>
            <InputContainer>
              <InputIcon>📧</InputIcon>
              <StyledInput
                type="email"
                placeholder="Email"
                value={formData.email}
                onChange={(e) => handleInputChange('email', e.target.value)}
                disabled={busy}
                $hasError={!!errors.email}
              />
            </InputContainer>
            {errors.email && (
              <ErrorMessage
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
              >
                ⚠️ {errors.email}
              </ErrorMessage>
            )}
          </FormGroup>

          <FormGroup>
            <InputContainer>
              <InputIcon>🔒</InputIcon>
              <StyledInput
                type="password"
                placeholder="Mật khẩu (tối thiểu 6 ký tự)"
                value={formData.password}
                onChange={(e) => handleInputChange('password', e.target.value)}
                disabled={busy}
                $hasError={!!errors.password}
              />
            </InputContainer>
            {errors.password && (
              <ErrorMessage
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
              >
                ⚠️ {errors.password}
              </ErrorMessage>
            )}
          </FormGroup>

          <FormGroup>
            <InputContainer>
              <InputIcon>🔒</InputIcon>
              <StyledInput
                type="password"
                placeholder="Xác nhận mật khẩu"
                value={formData.confirmPassword}
                onChange={(e) => handleInputChange('confirmPassword', e.target.value)}
                disabled={busy}
                $hasError={!!errors.confirmPassword}
              />
            </InputContainer>
            {errors.confirmPassword && (
              <ErrorMessage
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
              >
                ⚠️ {errors.confirmPassword}
              </ErrorMessage>
            )}
          </FormGroup>

          <RegisterButton
            type="submit"
            disabled={busy || !isFormValid()}
            whileHover={{ scale: busy ? 1 : 1.02 }}
            whileTap={{ scale: busy ? 1 : 0.98 }}
            style={{
              opacity: (!busy && isFormValid()) ? 1 : 0.7,
              cursor: (!busy && isFormValid()) ? 'pointer' : 'not-allowed'
            }}
          >
            {busy ? "Đang đăng ký..." : isFormValid() ? "Đăng ký" : "Vui lòng điền đầy đủ thông tin"}
          </RegisterButton>
        </form>

        <LoginLink>
          Đã có tài khoản? <Link to="/login">Đăng nhập tại đây</Link>
        </LoginLink>

        <BackToHome>
          <Link to="/">← Quay về trang chủ</Link>
        </BackToHome>
      </RegisterCard>
    </RegisterContainer>
  );
};

export default RegisterPage;
