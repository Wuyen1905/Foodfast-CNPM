import React from 'react';

type Props = { method: 'visa'|'momo'|'zalopay'|'cod' };

const PaymentIcons: React.FC<Props> = ({ method }) => {
  switch (method) {
    case 'visa':
      return <span role="img" aria-label="Thẻ Visa">💳</span>;
    case 'momo':
      return <span role="img" aria-label="Ví MoMo">🌸</span>;
    case 'zalopay':
      return <span role="img" aria-label="Ví ZaloPay">💰</span>;
    case 'cod':
      return <span role="img" aria-label="Thanh toán khi nhận hàng">🪙</span>;
    default:
      return null;
  }
};

export default PaymentIcons;


