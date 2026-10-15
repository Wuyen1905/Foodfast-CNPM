import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import styled from 'styled-components';
import {
  createVNPayUrl,
  createVNPayGroupUrl,
  reconcileVNPayGroup,
  reconcileVNPayOrder,
  verifyVNPayReturn,
  VNPayPaymentResult,
} from '../services/vnpay';
import { useCart } from '@/context/CartContext';
import toast from 'react-hot-toast';
import { formatVND } from '../utils/currency';

const Container = styled.div`
  max-width: 600px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const Card = styled(motion.div)`
  background: var(--card);
  border-radius: 16px;
  padding: 32px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border);
  text-align: center;
  width: 100%;
`;

const Icon = styled(motion.div)`
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
`;

const SuccessIcon = styled(Icon)`
  background: linear-gradient(135deg, #4CAF50, #45a049);
  color: white;
`;

const ErrorIcon = styled(Icon)`
  background: linear-gradient(135deg, #f44336, #d32f2f);
  color: white;
`;

const Title = styled.h2`
  margin: 0 0 16px 0;
  color: var(--text);
  font-size: 24px;
  font-weight: 700;
`;

const Message = styled.p`
  color: var(--secondaryText);
  margin: 0 0 24px 0;
  line-height: 1.6;
`;

const Button = styled(motion.button)`
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.65;
  }
`;

const LoadingSpinner = styled(motion.div)`
  width: 40px;
  height: 40px;
  border: 4px solid var(--border);
  border-top: 4px solid var(--primary);
  border-radius: 50%;
  margin: 0 auto 24px;
`;

const wait = (milliseconds: number) => new Promise(resolve => setTimeout(resolve, milliseconds));

const VNPayReturn: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { clear } = useCart();
  const processedRef = useRef(false);
  const [loading, setLoading] = useState(true);
  const [retrying, setRetrying] = useState(false);
  const [result, setResult] = useState<VNPayPaymentResult | null>(null);
  const queryString = searchParams.toString();

  useEffect(() => {
    const processPayment = async () => {
      if (processedRef.current) return;
      processedRef.current = true;

      try {
        let verifiedResult = await verifyVNPayReturn(new URLSearchParams(queryString));

        // Return URL chỉ mang tính hiển thị. Nếu VNPay báo thành công nhưng IPN đến chậm,
        // hỏi lại backend trong thời gian ngắn thay vì tự đánh dấu đơn đã thanh toán.
        const orderId = verifiedResult.orderId;
        if (verifiedResult.verified && !verifiedResult.success && orderId
            && verifiedResult.responseCode === '00') {
          for (let attempt = 0; attempt < 5 && !verifiedResult.success; attempt += 1) {
            await wait(1200);
            verifiedResult = orderId.startsWith('GROUP-')
              ? await reconcileVNPayGroup(orderId)
              : await reconcileVNPayOrder(orderId);
          }
        }
        setResult(verifiedResult);

        if (verifiedResult.success) {
          clear();
          toast.success('Thanh toán thành công!');
        } else {
          if (verifiedResult.paymentStatus === 'pending') {
            toast('Đang chờ VNPay xác nhận thanh toán.');
          } else {
            toast.error(verifiedResult.verified ? 'Thanh toán chưa thành công!' : 'Không thể xác minh thanh toán!');
          }
        }
      } catch (error) {
        setResult({
          verified: false,
          success: false,
          message: 'Không thể kết nối máy chủ để xác minh thanh toán. Vui lòng thử lại.'
        });
        toast.error('Không thể xác minh thanh toán!');
      } finally {
        setLoading(false);
      }
    };

    processPayment();
  }, [clear, queryString]);

  const handleContinue = async () => {
    if (result?.success) {
      navigate(result.orderId
        ? (result.orderId.startsWith('GROUP-')
          ? `/order-groups/${result.orderId}`
          : `/orders/${result.orderId}`)
        : '/orders');
      return;
    }

    if (!result?.verified || !result.orderId) {
      navigate('/checkout');
      return;
    }

    setRetrying(true);
    try {
      const paymentUrl = result.orderId.startsWith('GROUP-')
        ? await createVNPayGroupUrl(result.orderId)
        : await createVNPayUrl(result.orderId);
      window.location.assign(paymentUrl);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Không thể tạo lại phiên thanh toán');
      setRetrying(false);
    }
  };

  if (loading) {
    return (
      <Container>
        <Card
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
        >
          <LoadingSpinner
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
          />
          <Title>Đang xử lý thanh toán...</Title>
          <Message>Vui lòng chờ trong giây lát</Message>
        </Card>
      </Container>
    );
  }

  return (
    <Container>
      <Card
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
      >
        {result?.success ? (
          <>
            <SuccessIcon
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
            >
              ✓
            </SuccessIcon>
            <Title>Thanh toán thành công!</Title>
            <Message>{result.message}</Message>
            {result.orderId && (
              <Message>
                <strong>{result.orderId.startsWith('GROUP-') ? 'Mã nhóm đơn:' : 'Mã đơn hàng:'}</strong> {result.orderId}
              </Message>
            )}
            {result.transactionId && (
              <Message>
                <strong>Mã giao dịch:</strong> {result.transactionId}
              </Message>
            )}
          </>
        ) : (
          <>
            <ErrorIcon
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
            >
              ✗
            </ErrorIcon>
            <Title>
              {result?.paymentStatus === 'pending'
                ? 'Đang chờ VNPay xác nhận'
                : result?.paymentStatus === 'expired'
                  ? 'Phiên thanh toán đã hết hạn'
                  : result?.verified ? 'Thanh toán chưa thành công' : 'Không thể xác minh thanh toán'}
            </Title>
            <Message>{result?.message}</Message>
          </>
        )}
        
        <Button
          onClick={handleContinue}
          disabled={retrying}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
        >
          {result?.success
            ? 'Xem đơn hàng'
            : retrying
              ? 'Đang tạo lại thanh toán...'
              : result?.verified && result.orderId
                ? 'Thử thanh toán lại'
                : 'Quay lại thanh toán'}
        </Button>
      </Card>
    </Container>
  );
};

export default VNPayReturn;
