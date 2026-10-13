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

  it('accepts a short free-form delivery instruction', async () => {
    await expect(checkoutSchema.validate({ ...validForm, address: 'KTX A' }))
      .resolves.toMatchObject({ address: 'KTX A' });
  });

  it('does not impose an arbitrary character limit on the delivery address', async () => {
    const longAddress = 'Khu A, '.repeat(500);
    await expect(checkoutSchema.validate({ ...validForm, address: longAddress }))
      .resolves.toMatchObject({ address: longAddress.trim() });
  });

  it('still requires a non-empty delivery address', async () => {
    await expect(checkoutSchema.validate({ ...validForm, address: '   ' }))
      .rejects.toThrow('bắt buộc');
  });

  it('only accepts payment methods shown on the page', async () => {
    await expect(checkoutSchema.validate({ ...validForm, payment: 'momo' }))
      .rejects.toThrow('không hợp lệ');
  });
});
