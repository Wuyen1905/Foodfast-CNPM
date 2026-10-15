import React, { createContext, useContext, useState, ReactNode, useEffect } from "react";
import {
  getOrders,
  createOrder,
  cancelOrder as cancelOrderApi,
  CancellationActor as OrderApiCancellationActor,
  CancellationReasonCode,
  CancelOrderRequest,
  updateOrderStatus as updateOrderStatusApi,
} from "@/api/orderApi";
import { realtimeSocket } from "@/realtime/socket";
import {
  normalizeOrderStatusValue,
  normalizePaymentStatusValue,
  type OrderStatus,
  type PaymentStatus,
} from '@/types/status';
import { useAuth } from "./AuthContext";

export type { OrderStatus } from '@/types/status';

export type Order = {
  id: string;
  orderGroupId?: string;
  groupPosition?: number;
  name: string;
  phone: string;
  customerEmail?: string;
  address: string;
  items: {
    id?: string;
    productId?: string;
    name: string;
    qty: number;
    price: number;
    productImage?: string;
    selectedOptionIds?: string[];
    itemNote?: string;
    selectedOptions?: Array<{ groupName: string; optionName: string; priceDelta: number }>;
    originalProductId?: string;
    originalProductName?: string;
    substitutionProposalId?: number;
  }[];
  total: number;
  status: OrderStatus;
  paymentMethod?: 'visa' | 'momo' | 'zalopay' | 'cod' | 'vnpay';
  paymentStatus?: PaymentStatus;
  vnpayTransactionId?: string;
  restaurantId?: string;
  userId?: string;
  createdAt?: number;
  updatedAt?: number;
  confirmedAt?: number;
  paidAt?: number;
  preparingAt?: number;
  readyAt?: number;
  deliveringAt?: number;
  deliveredAt?: number;
  cancelledAt?: number;
  internalNotes?: string;
  confirmedBy?: string;
  cancellationActor?: OrderApiCancellationActor;
  cancellationActorId?: string;
  cancellationReasonCode?: CancellationReasonCode;
  cancellationReasonDetail?: string;
  subtotal?: number;
  shippingFee?: number;
  taxAmount?: number;
  discountAmount?: number;
  voucherCode?: string;
  voucherName?: string;
  partialRefundAmount?: number;
};

type OrderContextType = {
  orders: Order[];
  loading: boolean;
  addOrder: (order: Order, idempotencyKey: string) => Promise<Order>;
  getOrdersByPhone: (phone: string) => Order[];
  getOrdersByRestaurantId: (restaurantId: string) => Order[];
  updateOrderStatus: (id: string, status: OrderStatus) => void;
  confirmOrder: (id: string, confirmedBy?: string) => void;
  cancelOrder: (id: string, request: CancelOrderRequest) => Promise<Order>;
  addOrderNote: (id: string, note: string) => void;
};

const OrderContext = createContext<OrderContextType | undefined>(undefined);

export const OrderProvider = ({ children }: { children: ReactNode }) => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  // Load orders from backend on mount
  useEffect(() => {
    const loadOrders = async () => {
      if (!user?.id) {
        setOrders([]);
        setLoading(false);
        return;
      }
      setLoading(true);
      try {
        const data = await getOrders();
        // Transform to Order format
        const transformed = data.map((o: any) => ({
          id: o.id,
          orderGroupId: o.orderGroupId,
          groupPosition: o.groupPosition,
          name: o.name || o.customerName,
          phone: o.phone || o.customerPhone,
          customerEmail: o.customerEmail,
          address: o.address,
          items: o.items?.map((item: any) => ({
            id: item.id,
            productId: item.productId,
            name: item.name,
            qty: item.qty || item.quantity,
            price: item.price,
            productImage: item.productImage,
            itemNote: item.itemNote,
            selectedOptions: item.selectedOptions,
            originalProductId: item.originalProductId,
            originalProductName: item.originalProductName,
            substitutionProposalId: item.substitutionProposalId,
          })) || [],
          total: o.total,
          status: normalizeOrderStatusValue(o.status),
          paymentMethod: o.paymentMethod,
          paymentStatus: normalizePaymentStatusValue(o.paymentStatus),
          vnpayTransactionId: o.vnpayTransactionId,
          restaurantId: o.restaurantId || o.restaurant,
          userId: o.userId,
          createdAt: o.createdAt,
          updatedAt: o.updatedAt,
          confirmedAt: o.confirmedAt,
          paidAt: o.paidAt,
          preparingAt: o.preparingAt,
          readyAt: o.readyAt,
          deliveringAt: o.deliveringAt,
          deliveredAt: o.deliveredAt,
          cancelledAt: o.cancelledAt,
          internalNotes: o.internalNotes,
          confirmedBy: o.confirmedBy,
          cancellationActor: o.cancellationActor,
          cancellationActorId: o.cancellationActorId,
          cancellationReasonCode: o.cancellationReasonCode,
          cancellationReasonDetail: o.cancellationReasonDetail,
          subtotal: o.subtotal,
          shippingFee: o.shippingFee,
          taxAmount: o.taxAmount,
          discountAmount: o.discountAmount,
          voucherCode: o.voucherCode,
          voucherName: o.voucherName,
          partialRefundAmount: o.partialRefundAmount,
        }));
        setOrders(transformed);
      } catch (error) {
        console.error('Failed to load orders:', error);
        setOrders([]);
      } finally {
        setLoading(false);
      }
    };
    loadOrders();
  }, [user?.id, user?.restaurantId, user?.role]);

  useEffect(() => {
    if (!user?.id) {
      setOrders([]);
      return;
    }
    const unsub = realtimeSocket.onOrderUpdate((order) => {
        setOrders(prev => {
            const exists = prev.find(o => o.id === order.id);
            if (exists) {
                return prev.map(o => o.id === order.id ? { ...o, ...order } : o);
            }
            return [...prev, order];
        });
    });

    return unsub;
  }, [user?.id, user?.restaurantId, user?.role]);

  const addOrder = async (order: Order, idempotencyKey: string) => {
    try {
      const response = await createOrder({
        customerPhone: order.phone,
        address: order.address,
        restaurantId: order.restaurantId || undefined,
        items: order.items.map(item => ({
          productId: item.productId,
          name: item.name,
          quantity: item.qty,
          price: item.price,
          productImage: item.productImage,
          selectedOptionIds: item.selectedOptionIds,
          itemNote: item.itemNote,
        })),
        paymentMethod: order.paymentMethod,
        note: order.internalNotes,
        voucherCode: order.voucherCode,
      }, idempotencyKey);
      
      // Transform response to Order format and add to state
      const newOrder: Order = {
        id: response.id,
        orderGroupId: response.orderGroupId,
        groupPosition: response.groupPosition,
        name: response.name || response.customerName || '',
        phone: response.phone || response.customerPhone || '',
        customerEmail: response.customerEmail,
        address: response.address,
        items: response.items?.map((item: any) => ({
          id: item.id,
          productId: item.productId,
          name: item.name,
          qty: item.qty || item.quantity,
          price: item.price,
          itemNote: item.itemNote,
          selectedOptions: item.selectedOptions,
          originalProductId: item.originalProductId,
          originalProductName: item.originalProductName,
          substitutionProposalId: item.substitutionProposalId,
        })) || [],
        total: response.total,
        status: normalizeOrderStatusValue(response.status),
        paymentMethod: response.paymentMethod as Order['paymentMethod'],
        paymentStatus: normalizePaymentStatusValue(response.paymentStatus),
        vnpayTransactionId: response.vnpayTransactionId,
        restaurantId: response.restaurantId || response.restaurant,
        userId: response.userId,
        createdAt: response.createdAt,
        updatedAt: response.updatedAt,
        confirmedAt: response.confirmedAt,
        paidAt: response.paidAt,
        preparingAt: response.preparingAt,
        readyAt: response.readyAt,
        deliveringAt: response.deliveringAt,
        deliveredAt: response.deliveredAt,
        cancelledAt: response.cancelledAt,
        subtotal: response.subtotal,
        shippingFee: response.shippingFee,
        taxAmount: response.taxAmount,
        discountAmount: response.discountAmount,
        voucherCode: response.voucherCode,
        voucherName: response.voucherName,
        partialRefundAmount: response.partialRefundAmount,
      };

      setOrders((prev) => prev.some((item) => item.id === newOrder.id)
        ? prev.map((item) => item.id === newOrder.id ? newOrder : item)
        : [...prev, newOrder]);
      return newOrder;
    } catch (error) {
      console.error('Failed to create order:', error);
      throw error;
    }
  };

  const getOrdersByPhone = (phone: string) => {
    return orders.filter((o) => o.phone === phone);
  };

  const getOrdersByRestaurantId = (restaurantId: string) => {
    return orders.filter((o) => o.restaurantId === restaurantId);
  };

  const updateOrderStatus = async (id: string, status: OrderStatus) => {
    try {
      await updateOrderStatusApi(id, status);
      // Update local state after successful API call
      setOrders((prev) => prev.map((o) => (o.id === id ? { 
        ...o, 
        status,
        updatedAt: Date.now()
      } : o)));
    } catch (error) {
      console.error('Failed to update order status:', error);
      throw error;
    }
  };

  const confirmOrder = async (id: string, confirmedBy?: string) => {
    try {
      await updateOrderStatusApi(id, 'Confirmed');
      // Update local state after successful API call
      setOrders((prev) => prev.map((o) => (o.id === id ? { 
        ...o, 
        status: "Confirmed" as OrderStatus,
        confirmedAt: Date.now(),
        confirmedBy: confirmedBy || o.confirmedBy,
        updatedAt: Date.now()
      } : o)));
    } catch (error) {
      console.error('Failed to confirm order:', error);
      throw error;
    }
  };

  const cancelOrder = async (id: string, request: CancelOrderRequest): Promise<Order> => {
    try {
      const response = await cancelOrderApi(id, request);
      let result: Order | undefined;
      setOrders((prev) => prev.map((o) => {
        if (o.id !== id) return o;
        result = {
          ...o,
          status: 'Cancelled',
          paymentStatus: response.paymentStatus as Order['paymentStatus'],
          cancelledAt: response.cancelledAt,
          cancellationActor: response.cancellationActor,
          cancellationActorId: response.cancellationActorId,
          cancellationReasonCode: response.cancellationReasonCode,
          cancellationReasonDetail: response.cancellationReasonDetail,
          updatedAt: response.updatedAt,
        };
        return result;
      }));
      return result || {
        ...(response as unknown as Order),
        name: response.name || response.customerName || '',
        phone: response.phone || response.customerPhone || '',
        status: 'Cancelled',
        items: response.items.map(item => ({
          productId: item.productId,
          name: item.name,
          qty: item.qty || item.quantity || 0,
          price: item.price,
        })),
      };
    } catch (error) {
      console.error('Failed to cancel order:', error);
      throw error;
    }
  };

  const addOrderNote = async (id: string, note: string) => {
    try {
      // Add note via backend API (using restaurantOrderService)
      const { addOrderNote: addOrderNoteApi } = await import('@/services/restaurantOrderService');
      await addOrderNoteApi(id, note);
      // Update local state after successful API call
      setOrders((prev) => prev.map((o) => (o.id === id ? { 
        ...o, 
        internalNotes: o.internalNotes ? `${o.internalNotes}\n${note}` : note,
        updatedAt: Date.now()
      } : o)));
    } catch (error) {
      console.error('Failed to add order note:', error);
      throw error;
    }
  };

  return (
    <OrderContext.Provider value={{
      orders,
      loading,
      addOrder,
      getOrdersByPhone, 
      getOrdersByRestaurantId,
      updateOrderStatus, 
      confirmOrder,
      cancelOrder,
      addOrderNote
    }}>
      {children}
    </OrderContext.Provider>
  );
};

export const useOrders = () => {
  const ctx = useContext(OrderContext);
  if (!ctx) throw new Error("useOrders must be used inside OrderProvider");
  return ctx;
};
