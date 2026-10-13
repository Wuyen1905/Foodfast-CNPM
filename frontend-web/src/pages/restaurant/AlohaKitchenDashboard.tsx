import React, { useEffect } from 'react';
import RestaurantDashboardLayout from '@/components/restaurant/RestaurantDashboardLayout';
import OrderTracking from '@/components/restaurant/OrderTracking';
import CourierManagement from '@/components/restaurant/CourierManagement';
import SupportTicketInbox from '@/components/SupportTicketInbox';
import ComplaintInbox from '@/components/ComplaintInbox';
import { useAuth } from '@/context';

// Aloha Kitchen Asian Fusion Theme (Warm Orange-Yellow)
const ALOHA_THEME = {
  primary: '#ffcc70',
  secondary: '#ff9671',
  accent: '#ffc75f',
  background: '#FFF8F0',
  light: '#FFFEF8'
};

const RESTAURANT_ID = 'restaurant_2';
const RESTAURANT_NAME = '🌺 Nhà hàng Aloha';

const AlohaKitchenDashboard: React.FC = () => {
  const { user } = useAuth();
  
  useEffect(() => {
    console.log("🌺 [AlohaKitchenDashboard] Component mounted!");
    console.log("👤 [AlohaKitchenDashboard] Current user:", user);
    return () => {
      console.log("🌺 [AlohaKitchenDashboard] Component unmounting");
    };
  }, [user]);

  return (
    <RestaurantDashboardLayout
      theme={ALOHA_THEME}
      restaurantName={RESTAURANT_NAME}
    >
      {(activeTab) => (
        <>
          {activeTab === 'orders' && (
            <OrderTracking
              restaurantId={RESTAURANT_ID}
              theme={ALOHA_THEME}
            />
          )}
          {activeTab === 'couriers' && (
            <CourierManagement
              restaurantId={RESTAURANT_ID}
              theme={ALOHA_THEME}
            />
          )}
          {activeTab === 'support' && (
            <>
              <SupportTicketInbox target="RESTAURANT" restaurantId={RESTAURANT_ID} />
              <ComplaintInbox role="RESTAURANT" viewerId={RESTAURANT_ID} />
            </>
          )}
        </>
      )}
    </RestaurantDashboardLayout>
  );
};

export default AlohaKitchenDashboard;
