import { describe, expect, it } from 'vitest';
import { checkoutSchema } from './checkoutSchema';

describe('checkoutSchema', () => {
  const validForm = {
    phone: '0901234567',
    address: '123 Nguyễn Trãi, Phường Chợ Quán, TP.HCM',
    note: 'Gọi trước khi giao',
    payment: 'cod',
  };

  it('accepts the simplified phone and delivery address form', async () => {
    await expect(checkoutSchema.validate(validForm)).resolves.toMatchObject(validForm);
  });

  it('requires a complete delivery address', async () => {
    await expect(checkoutSchema.validate({ ...validForm, address: '123' }))
      .rejects.toThrow('ít nhất 10 ký tự');
  });

  it('only accepts payment methods shown on the page', async () => {
    await expect(checkoutSchema.validate({ ...validForm, payment: 'momo' }))
      .rejects.toThrow('không hợp lệ');
  });
});
