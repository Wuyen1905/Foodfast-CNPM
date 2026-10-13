import * as yup from 'yup';

export const checkoutSchema = yup.object({
  phone: yup
    .string()
    .required('Số điện thoại là bắt buộc')
    .matches(/^(0|\+84)[0-9]{9,10}$/, 'Số điện thoại không hợp lệ (VD: 0123456789 hoặc +84123456789)'),

  address: yup
    .string()
    .trim()
    .required('Địa chỉ giao hàng là bắt buộc')
    .min(10, 'Địa chỉ giao hàng phải có ít nhất 10 ký tự')
    .max(250, 'Địa chỉ giao hàng không được quá 250 ký tự'),

  note: yup
    .string()
    .optional()
    .max(200, 'Ghi chú không được quá 200 ký tự'),

  payment: yup
    .string()
    .oneOf(['cod', 'vnpay'], 'Phương thức thanh toán không hợp lệ')
    .required('Vui lòng chọn phương thức thanh toán'),
});

export type CheckoutFormData = yup.InferType<typeof checkoutSchema>;
