import Swal from 'sweetalert2';

const restaurantName = (restaurantId?: string): string => {
  if (restaurantId === 'rest_2' || restaurantId === 'sweetdreams') {
    return 'SweetDreams';
  }
  if (restaurantId === 'restaurant_2' || restaurantId === 'aloha') {
    return 'Aloha';
  }
  return 'nhà hàng khác';
};

export const isRestaurantCartConflict = (error: any): boolean =>
  error?.response?.status === 409
  && error?.response?.data?.code === 'CART_RESTAURANT_CONFLICT';

export const askToSwitchCartRestaurant = async (error: any): Promise<boolean | null> => {
  if (!isRestaurantCartConflict(error)) {
    return null;
  }

  const data = error.response.data;
  const currentRestaurant = restaurantName(data.currentRestaurantId);
  const requestedRestaurant = restaurantName(data.requestedRestaurantId);
  const result = await Swal.fire({
    title: 'Bạn đang chọn món ở nhà hàng khác',
    html: `Giỏ hàng hiện có món của <strong>${currentRestaurant}</strong>.<br/>`
      + `Để chọn món của <strong>${requestedRestaurant}</strong>, giỏ hàng hiện tại phải được xóa.`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: 'Xóa giỏ và chuyển nhà hàng',
    cancelButtonText: 'Giữ giỏ hiện tại',
    confirmButtonColor: '#ff6600',
    cancelButtonColor: '#6c757d',
    reverseButtons: true,
  });

  return result.isConfirmed;
};

export const getCartErrorMessage = (error: any): string =>
  error?.response?.data?.message
  || error?.message
  || 'Không thể cập nhật giỏ hàng';
