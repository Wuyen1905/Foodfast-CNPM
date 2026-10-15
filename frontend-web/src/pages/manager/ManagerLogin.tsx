import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import { useAuth } from '@/context/AuthContext';
import type { UserRole } from '@/types/auth';

const Page = styled.main`
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  background:
    radial-gradient(circle at 12% 16%, rgba(89, 110, 232, 0.18), transparent 34%),
    linear-gradient(145deg, #eef2f9 0%, #f8f9fc 100%);

  @media (max-width: 680px) {
    padding: 14px;
  }
`;

const LoginShell = styled.section`
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  overflow: hidden;
  border: 1px solid #dfe4ee;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(36, 50, 88, 0.16);

  @media (max-width: 780px) {
    grid-template-columns: 1fr;
  }
`;

const Introduction = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 42px;
  color: #fff;
  background: linear-gradient(155deg, #263b7d 0%, #596ee8 100%);

  h1 {
    margin: 18px 0 12px;
    font-size: clamp(28px, 4vw, 38px);
    line-height: 1.15;
  }

  p {
    margin: 0;
    color: #e8edff;
    line-height: 1.65;
  }

  @media (max-width: 780px) {
    padding: 28px;
  }
`;

const Brand = styled.div`
  font-size: 20px;
  font-weight: 850;
  letter-spacing: 0.2px;
`;

const Responsibilities = styled.ul`
  display: grid;
  gap: 10px;
  margin: 38px 0 0;
  padding: 0;
  list-style: none;
  color: #f6f8ff;

  li::before {
    content: '✓';
    margin-right: 10px;
    color: #9ee7bd;
    font-weight: 900;
  }

  @media (max-width: 780px) {
    display: none;
  }
`;

const FormPanel = styled.div`
  padding: 42px 48px;

  h2 {
    margin: 0 0 8px;
    color: #20283a;
    font-size: 28px;
  }

  > p {
    margin: 0 0 26px;
    color: #697386;
    line-height: 1.5;
  }

  @media (max-width: 680px) {
    padding: 28px 22px;
  }
`;

const Notice = styled.div<{ $tone?: 'info' | 'error' }>`
  margin-bottom: 18px;
  padding: 12px 14px;
  border: 1px solid ${({ $tone }) => ($tone === 'error' ? '#f1b8be' : '#bcd3ff')};
  border-radius: 10px;
  background: ${({ $tone }) => ($tone === 'error' ? '#fff1f2' : '#eef5ff')};
  color: ${({ $tone }) => ($tone === 'error' ? '#9e2532' : '#294f89')};
  font-size: 14px;
  line-height: 1.5;
`;

const Form = styled.form`
  display: grid;
  gap: 18px;
`;

const Field = styled.label`
  display: grid;
  gap: 7px;
  color: #30394c;
  font-weight: 700;
`;

const InputBox = styled.div`
  position: relative;
`;

const Input = styled.input`
  width: 100%;
  min-height: 50px;
  padding: 12px 46px 12px 14px;
  border: 1px solid #cbd3e1;
  border-radius: 10px;
  background: #fbfcfe;
  color: #20283a;
  font: inherit;

  &:focus {
    outline: none;
    border-color: #596ee8;
    box-shadow: 0 0 0 3px rgba(89, 110, 232, 0.16);
    background: #fff;
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
`;

const PasswordToggle = styled.button`
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #596579;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background: #edf0f7;
    outline: none;
  }
`;

const Submit = styled.button`
  min-height: 50px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f64dc, #394ba9);
  color: #fff;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(70, 88, 193, 0.24);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 13px 26px rgba(70, 88, 193, 0.3);
  }

  &:disabled {
    opacity: 0.62;
    cursor: not-allowed;
  }
`;

const Links = styled.div`
  display: flex;
  justify-content: center;
  gap: 18px;
  margin-top: 22px;
  font-size: 14px;

  a {
    color: #4358c1;
    font-weight: 700;
    text-decoration: none;
  }

  a:hover {
    text-decoration: underline;
  }
`;

type ManagerLoginState = {
  from?: { pathname?: string };
  notice?: string;
  registeredIdentity?: string;
};

const portalForRole: Record<UserRole, { path: string; label: string }> = {
  customer: { path: '/login', label: 'Cổng khách hàng' },
  restaurant: { path: '/login', label: 'Cổng nhà hàng' },
  admin: { path: '/admin/login', label: 'Cổng quản trị' },
  manager: { path: '/manager/login', label: 'Cổng nhân viên' },
};

const ManagerLogin: React.FC = () => {
  const { user, login, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state || {}) as ManagerLoginState;
  const [username, setUsername] = useState(state.registeredIdentity || '');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [suggestedPortal, setSuggestedPortal] = useState<{ path: string; label: string }>();

  useEffect(() => {
    if (user?.role === 'manager') {
      navigate('/manager', { replace: true });
    }
  }, [navigate, user]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    setSuggestedPortal(undefined);

    const result = await login(username.trim(), password);
    if (!result.ok || !result.user) {
      const message = result.message || 'Tên đăng nhập hoặc mật khẩu không đúng';
      setError(message);
      toast.error(message);
      setBusy(false);
      return;
    }

    if (result.user.role !== 'manager') {
      const portal = portalForRole[result.user.role];
      logout();
      setSuggestedPortal(portal);
      setError(`Tài khoản này không phải nhân viên quản lý. Vui lòng sử dụng ${portal.label}.`);
      setBusy(false);
      return;
    }

    toast.success('Đăng nhập Cổng nhân viên thành công');
    const requestedPath = state.from?.pathname;
    const destination = requestedPath?.startsWith('/manager') && requestedPath !== '/manager/login'
      ? requestedPath
      : '/manager';
    navigate(destination, { replace: true });
  };

  return (
    <Page>
      <LoginShell aria-label="Cổng đăng nhập nhân viên FoodFast">
        <Introduction>
          <div>
            <Brand>FoodFast · Nội bộ</Brand>
            <h1>Cổng nhân viên quản lý</h1>
            <p>Theo dõi vận hành, hỗ trợ khách hàng và đối soát nhà hàng trong một không gian làm việc thống nhất.</p>
          </div>
          <Responsibilities>
            <li>Một giao diện chung cho các ca làm việc</li>
            <li>Mọi thao tác được ghi theo từng tài khoản</li>
            <li>Dữ liệu và quyền được xác minh từ backend</li>
          </Responsibilities>
        </Introduction>

        <FormPanel>
          <h2>Đăng nhập nhân viên</h2>
          <p>Dành cho nhân viên quản lý nội bộ FoodFast.</p>

          {state.notice && <Notice>{state.notice}</Notice>}
          {user && user.role !== 'manager' && (
            <Notice>
              Tab này đang đăng nhập bằng tài khoản <strong>{user.name || user.username}</strong>.
              Đăng nhập bên dưới sẽ chuyển sang tài khoản nhân viên.
            </Notice>
          )}

          <Form onSubmit={handleSubmit}>
            <Field>
              Tên đăng nhập
              <InputBox>
                <Input
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  autoComplete="username"
                  autoCapitalize="none"
                  placeholder="Nhập tên đăng nhập nhân viên"
                  disabled={busy}
                  required
                />
              </InputBox>
            </Field>

            <Field>
              Mật khẩu
              <InputBox>
                <Input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  autoComplete="current-password"
                  placeholder="Nhập mật khẩu"
                  disabled={busy}
                  required
                />
                <PasswordToggle
                  type="button"
                  onClick={() => setShowPassword((current) => !current)}
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  title={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  disabled={busy}
                >
                  {showPassword ? '🙈' : '👁'}
                </PasswordToggle>
              </InputBox>
            </Field>

            <Submit type="submit" disabled={busy}>
              {busy ? 'Đang xác minh…' : 'Đăng nhập Cổng nhân viên'}
            </Submit>

            {error && (
              <Notice $tone="error" role="alert">
                {error}
                {suggestedPortal && (
                  <div style={{ marginTop: 8 }}>
                    <Link to={suggestedPortal.path}>{`Đi đến ${suggestedPortal.label} →`}</Link>
                  </div>
                )}
              </Notice>
            )}
          </Form>

          <Links>
            <Link to="/">Về trang chủ</Link>
            <Link to="/login">Đăng nhập khách hàng hoặc nhà hàng</Link>
          </Links>
        </FormPanel>
      </LoginShell>
    </Page>
  );
};

export default ManagerLogin;
