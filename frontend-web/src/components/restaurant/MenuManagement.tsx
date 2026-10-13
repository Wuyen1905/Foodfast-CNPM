import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import toast from 'react-hot-toast';
import { useMenu } from '@/context/MenuContext';
import { Product } from '@/data/products';
import ProductOptionManager from './ProductOptionManager';
import { 
  searchProductsByRestaurant,
  getCategoriesByRestaurant,
  getRestaurantMenuStats
} from '@/services/menuService';

interface MenuManagementProps {
  restaurantId: string;
  theme: {
    primary: string;
    secondary: string;
    background: string;
  };
}

const restaurantNameFor = (restaurantId: string): "SweetDreams" | "Aloha" => {
  const normalized = restaurantId.trim().toLowerCase();
  return normalized === 'sweetdreams' || normalized === 'rest_2'
    ? 'SweetDreams'
    : 'Aloha';
};

const MenuManagement: React.FC<MenuManagementProps> = ({ restaurantId, theme }) => {
  const { 
    getRestaurantProducts, 
    addProduct, 
    updateProduct, 
    deleteProduct, 
    toggleAvailability,
    loading: contextLoading 
  } = useMenu();
  
  const [dishes, setDishes] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('Tất cả');
  const [statusFilter, setStatusFilter] = useState('Tất cả');
  const [categories, setCategories] = useState<string[]>([]);
  const [stats, setStats] = useState({
    totalDishes: 0,
    availableDishes: 0,
    outOfStockDishes: 0,
    categories: 0
  });
  
  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingDish, setEditingDish] = useState<Product | null>(null);
  const [optionDish, setOptionDish] = useState<Product | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    category: '',
    price: '',
    available: true,
    dailyLimit: '',
    description: '',
    ingredients: '',
    preparationTime: ''
  });

  // Load data
  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const restaurant = restaurantNameFor(restaurantId);
        const [dishesData, categoriesData, statsData] = await Promise.all([
          getRestaurantProducts(restaurant),
          getCategoriesByRestaurant(restaurant),
          getRestaurantMenuStats(restaurant)
        ]);
        
        setDishes(dishesData);
        setCategories(categoriesData);
        setStats(statsData);
      } catch (error) {
        console.error('Error loading menu data:', error);
        toast.error('Không thể tải dữ liệu thực đơn');
      } finally {
        setLoading(false);
      }
    };
    
    loadData();
  }, [restaurantId, getRestaurantProducts]);

  // Search and filter dishes
  useEffect(() => {
    const searchDishes = async () => {
      try {
        const restaurant = restaurantNameFor(restaurantId);
        const availableOnly = statusFilter === 'Còn hàng';
        const unavailableOnly = statusFilter === 'Hết hàng';
        
        const filteredDishes = await searchProductsByRestaurant(
          restaurant,
          searchQuery,
          categoryFilter === 'Tất cả' ? undefined : categoryFilter,
          availableOnly
        );
        
        let result = filteredDishes;
        if (unavailableOnly) {
          result = filteredDishes.filter(dish => !dish.available);
        } else if (statusFilter === 'Còn hàng') {
          result = filteredDishes.filter(dish => dish.available);
        }
        
        setDishes(result);
      } catch (error) {
        console.error('Error searching dishes:', error);
      }
    };
    
    searchDishes();
  }, [searchQuery, categoryFilter, statusFilter, restaurantId]);

  const handleAddDish = () => {
    setEditingDish(null);
    setFormData({
      name: '',
      category: '',
      price: '',
      available: true,
      dailyLimit: '',
      description: '',
      ingredients: '',
      preparationTime: ''
    });
    setIsModalOpen(true);
  };

  const handleEditDish = (dish: Product) => {
    setEditingDish(dish);
    setFormData({
      name: dish.name,
      category: dish.category,
      price: dish.price.toString(),
      available: dish.manuallyAvailable ?? dish.available,
      dailyLimit: dish.dailyLimit?.toString() || '',
      description: dish.description || '',
      ingredients: dish.ingredients?.join(', ') || '',
      preparationTime: dish.preparationTime?.toString() || ''
    });
    setIsModalOpen(true);
  };

  const handleDeleteDish = async (dishId: string) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa món ăn này?')) return;
    
    try {
      const restaurant = restaurantNameFor(restaurantId);
      const success = await deleteProduct(dishId, restaurant);
      if (success) {
        // Reload dishes from service to ensure consistency
        const restaurantProducts = await getRestaurantProducts(restaurant);
        setDishes(restaurantProducts);
        // Refresh stats
        const statsData = await getRestaurantMenuStats(restaurant);
        setStats(statsData);
      }
    } catch (error) {
      console.error('Error deleting dish:', error);
      toast.error('Không thể xóa món ăn');
    }
  };

  const handleToggleAvailability = async (dish: Product) => {
    const restaurant = restaurantNameFor(restaurantId);
    const updated = await toggleAvailability(dish.id, restaurant);
    if (!updated) return;
    setDishes((current) => current.map((item) => item.id === dish.id ? updated : item));
    setStats(await getRestaurantMenuStats(restaurant));
  };

  const handleSaveDish = async () => {
    if (!formData.name || !formData.category || !formData.price) {
      toast.error('Vui lòng nhập đầy đủ thông tin!');
        return;
      }

    if (formData.dailyLimit && parseInt(formData.dailyLimit, 10) < 1) {
      toast.error('Giới hạn bán mỗi ngày phải lớn hơn 0');
      return;
    }

    try {
      const restaurant = restaurantNameFor(restaurantId);
      const dishData = {
        name: formData.name,
        category: formData.category,
        price: parseInt(formData.price),
        available: formData.available,
        dailyLimit: formData.dailyLimit ? parseInt(formData.dailyLimit, 10) : null,
        description: formData.description,
        ingredients: formData.ingredients ? formData.ingredients.split(',').map(i => i.trim()) : [],
        preparationTime: formData.preparationTime ? parseInt(formData.preparationTime) : undefined,
        image: '/images/default-dish.jpg', // Default image
        restaurant: restaurant
      };

      if (editingDish) {
        const updatedDish = await updateProduct(editingDish.id, dishData, restaurant);
        if (updatedDish) {
          // Reload dishes from service to ensure consistency
          const restaurantProducts = await getRestaurantProducts(restaurant);
          setDishes(restaurantProducts);
        }
      } else {
        const newDish = await addProduct(dishData);
        if (newDish) {
          // Reload dishes from service to ensure consistency
          const restaurantProducts = await getRestaurantProducts(restaurant);
          setDishes(restaurantProducts);
        }
      }
      
      // Refresh stats
      const statsData = await getRestaurantMenuStats(restaurant);
      setStats(statsData);
      
      setIsModalOpen(false);
    } catch (error) {
      console.error('Error saving dish:', error);
      toast.error('Không thể lưu món ăn');
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        padding: '40px',
        fontSize: '16px',
        color: '#666'
      }}>
        Đang tải dữ liệu menu...
      </div>
    );
  }

  return (
    <div style={{ 
      padding: '24px', 
      background: theme.background || '#f8f9fa',
      minHeight: '100vh'
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '24px',
        flexWrap: 'wrap',
        gap: '16px'
      }}>
        <h1 style={{
          fontSize: '28px',
          fontWeight: '700',
          color: theme.primary || '#333',
          margin: 0,
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          🍳 Quản lý món ăn
        </h1>
        
        <div style={{
          display: 'flex',
          gap: '16px',
          alignItems: 'center',
          flexWrap: 'wrap'
        }}>
          <input
            type="text"
            placeholder="Tìm kiếm món ăn..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              padding: '12px 16px',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              fontSize: '14px',
              minWidth: '200px',
              transition: 'border-color 0.2s'
            }}
          />
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            style={{
              padding: '12px 16px',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              fontSize: '14px',
              background: 'white',
              cursor: 'pointer'
            }}
          >
            {categories.map(cat => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{
              padding: '12px 16px',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              fontSize: '14px',
              background: 'white',
              cursor: 'pointer'
            }}
          >
            <option value="Tất cả">Tất cả</option>
            <option value="Còn hàng">Còn hàng</option>
            <option value="Hết hàng">Hết hàng</option>
          </select>
          <button
            onClick={handleAddDish}
            style={{
              padding: '12px 24px',
              border: 'none',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: '600',
              cursor: 'pointer',
              background: theme.primary || '#007bff',
              color: 'white',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              transition: 'all 0.2s'
            }}
          >
            ➕ Thêm món mới
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '16px',
        marginBottom: '24px'
      }}>
        <div style={{
          background: 'white',
          padding: '20px',
          borderRadius: '12px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          borderLeft: `4px solid ${theme.primary || '#007bff'}`
        }}>
          <div style={{
            fontSize: '24px',
            fontWeight: '700',
            color: theme.primary || '#007bff',
            marginBottom: '4px'
          }}>
            {stats.totalDishes}
          </div>
          <div style={{
            fontSize: '14px',
            color: '#666',
            fontWeight: '500'
          }}>
            Tổng số món
          </div>
        </div>
        
        <div style={{
          background: 'white',
          padding: '20px',
          borderRadius: '12px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          borderLeft: `4px solid ${theme.primary || '#007bff'}`
        }}>
          <div style={{
            fontSize: '24px',
            fontWeight: '700',
            color: theme.primary || '#007bff',
            marginBottom: '4px'
          }}>
            {stats.availableDishes}
          </div>
          <div style={{
            fontSize: '14px',
            color: '#666',
            fontWeight: '500'
          }}>
            Còn hàng
          </div>
        </div>
        
        <div style={{
          background: 'white',
          padding: '20px',
          borderRadius: '12px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          borderLeft: `4px solid ${theme.primary || '#007bff'}`
        }}>
          <div style={{
            fontSize: '24px',
            fontWeight: '700',
            color: theme.primary || '#007bff',
            marginBottom: '4px'
          }}>
            {stats.outOfStockDishes}
          </div>
          <div style={{
            fontSize: '14px',
            color: '#666',
            fontWeight: '500'
          }}>
            Hết hàng
          </div>
        </div>
        
        <div style={{
          background: 'white',
          padding: '20px',
          borderRadius: '12px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          borderLeft: `4px solid ${theme.primary || '#007bff'}`
        }}>
          <div style={{
            fontSize: '24px',
            fontWeight: '700',
            color: theme.primary || '#007bff',
            marginBottom: '4px'
          }}>
            {stats.categories}
          </div>
          <div style={{
            fontSize: '14px',
            color: '#666',
            fontWeight: '500'
          }}>
            Loại món
          </div>
        </div>
      </div>

      {/* Table */}
      <div style={{
        background: 'white',
        borderRadius: '12px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        overflow: 'hidden'
      }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead style={{
            background: theme.primary || '#007bff',
            color: 'white'
          }}>
            <tr>
              <th style={{
                padding: '16px',
                textAlign: 'left',
                fontWeight: '600',
                fontSize: '14px'
              }}>
                Tên món
              </th>
              <th style={{
                padding: '16px',
                textAlign: 'left',
                fontWeight: '600',
                fontSize: '14px'
              }}>
                Loại
              </th>
              <th style={{
                padding: '16px',
                textAlign: 'left',
                fontWeight: '600',
                fontSize: '14px'
              }}>
                Giá (₫)
              </th>
              <th style={{
                padding: '16px',
                textAlign: 'left',
                fontWeight: '600',
                fontSize: '14px'
              }}>
                Trạng thái
              </th>
              <th style={{
                padding: '16px',
                textAlign: 'left',
                fontWeight: '600',
                fontSize: '14px'
              }}>
                Bán hôm nay
              </th>
              <th style={{
                padding: '16px',
                textAlign: 'left',
                fontWeight: '600',
                fontSize: '14px'
              }}>
                Hành động
              </th>
            </tr>
          </thead>
          <tbody>
            {dishes.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '40px' }}>
                  <div style={{ color: '#666' }}>
                    Không có món ăn nào
                  </div>
                </td>
              </tr>
            ) : (
              dishes.map((dish, index) => (
                <tr key={dish.id} style={{
                  backgroundColor: index % 2 === 0 ? '#f8f9fa' : 'white'
                }}>
                  <td style={{ padding: '16px', borderBottom: '1px solid #e0e0e0' }}>
                    <div>
                      <div style={{ fontWeight: '600', marginBottom: '4px' }}>
                        {dish.name}
                      </div>
                      {dish.description && (
                        <div style={{ fontSize: '12px', color: '#666' }}>
                          {dish.description}
                        </div>
                      )}
                    </div>
                  </td>
                  <td style={{ padding: '16px', borderBottom: '1px solid #e0e0e0' }}>
                    {dish.category}
                  </td>
                  <td style={{ padding: '16px', borderBottom: '1px solid #e0e0e0' }}>
                    {formatCurrency(dish.price)}
                  </td>
                  <td style={{ padding: '16px', borderBottom: '1px solid #e0e0e0' }}>
                    <span style={{
                      padding: '4px 12px',
                      borderRadius: '20px',
                      fontSize: '12px',
                      fontWeight: '600',
                       background: dish.available ? '#d4edda' : dish.soldOutToday ? '#fff3cd' : '#f8d7da',
                       color: dish.available ? '#155724' : dish.soldOutToday ? '#856404' : '#721c24'
                     }}>
                       {dish.available ? 'Đang bán' : dish.soldOutToday ? 'Hết hôm nay' : 'Tạm ngừng'}
                     </span>
                   </td>
                  <td style={{ padding: '16px', borderBottom: '1px solid #e0e0e0' }}>
                    {dish.dailyLimit
                      ? `${dish.soldToday ?? 0}/${dish.dailyLimit} (còn ${dish.remainingToday ?? 0})`
                      : 'Không giới hạn'}
                  </td>
                  <td style={{ padding: '16px', borderBottom: '1px solid #e0e0e0' }}>
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                      <button
                        onClick={() => handleToggleAvailability(dish)}
                        style={{
                          padding: '8px 12px', fontSize: '12px', border: 'none', borderRadius: '8px',
                          background: (dish.manuallyAvailable ?? dish.available) ? '#f59e0b' : '#16a34a',
                          color: 'white', cursor: 'pointer'
                        }}
                      >
                        {(dish.manuallyAvailable ?? dish.available) ? '⏸ Tạm ngừng' : '▶ Mở bán'}
                      </button>
                      <button
                        onClick={() => setOptionDish(dish)}
                        style={{
                          padding: '8px 12px', fontSize: '12px', border: 'none', borderRadius: '8px',
                          background: theme.primary, color: 'white', cursor: 'pointer',
                        }}
                      >
                        🧩 Topping
                      </button>
                      <button
                        onClick={() => handleEditDish(dish)}
                        style={{
                          padding: '8px 12px',
                          fontSize: '12px',
                          border: 'none',
                          borderRadius: '8px',
                          background: '#6c757d',
                          color: 'white',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '4px'
                        }}
                      >
                        ✏️ Sửa món
                      </button>
                      <button
                        onClick={() => handleDeleteDish(dish.id)}
                        style={{
                          padding: '8px 12px',
                          fontSize: '12px',
                          border: 'none',
                          borderRadius: '8px',
                          background: '#dc3545',
                          color: 'white',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '4px'
                        }}
                      >
                        🗑️ Xóa món
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            background: 'white',
            borderRadius: '12px',
            padding: '24px',
            maxWidth: '500px',
            width: '90%',
            maxHeight: '80vh',
            overflowY: 'auto'
          }}>
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '20px'
            }}>
              <h3 style={{
                margin: 0,
                fontSize: '20px',
                fontWeight: '600',
                color: theme.primary || '#333'
              }}>
                {editingDish ? 'Sửa món ăn' : 'Thêm món mới'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                  color: '#666'
                }}
              >
                ×
              </button>
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontWeight: '500',
                color: '#333'
              }}>
                Giới hạn bán mỗi ngày
              </label>
              <input
                type="number"
                min="1"
                value={formData.dailyLimit}
                onChange={(e) => setFormData({ ...formData, dailyLimit: e.target.value })}
                placeholder="Để trống nếu không giới hạn"
                style={{
                  width: '100%',
                  padding: '12px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px'
                }}
              />
              <div style={{ marginTop: '6px', fontSize: '12px', color: '#666' }}>
                Hệ thống tự tính số đã bán hôm nay và tự mở lại vào ngày mới.
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontWeight: '500',
                color: '#333'
              }}>
                Tên món ăn *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="Nhập tên món ăn"
                style={{
                  width: '100%',
                  padding: '12px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px'
                }}
              />
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontWeight: '500',
                color: '#333'
              }}>
                Loại món *
              </label>
              <input
                type="text"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                placeholder="Ví dụ: Món chính, Tráng miệng, Đồ uống"
                style={{
                  width: '100%',
                  padding: '12px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px'
                }}
              />
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontWeight: '500',
                color: '#333'
              }}>
                Giá (VND) *
              </label>
              <input
                type="number"
                value={formData.price}
                onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                placeholder="Nhập giá món ăn"
                style={{
                  width: '100%',
                  padding: '12px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px'
                }}
              />
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontWeight: '500',
                color: '#333'
              }}>
                Trạng thái
              </label>
              <select
                value={formData.available ? 'true' : 'false'}
                onChange={(e) => setFormData({ ...formData, available: e.target.value === 'true' })}
                style={{
                  width: '100%',
                  padding: '12px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px',
                  background: 'white'
                }}
              >
                <option value="true">Còn hàng</option>
                <option value="false">Hết hàng</option>
              </select>
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontWeight: '500',
                color: '#333'
              }}>
                Mô tả
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Mô tả món ăn"
                style={{
                  width: '100%',
                  padding: '12px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px',
                  minHeight: '80px',
                  resize: 'vertical'
                }}
              />
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontWeight: '500',
                color: '#333'
              }}>
                Nguyên liệu
              </label>
              <input
                type="text"
                value={formData.ingredients}
                onChange={(e) => setFormData({ ...formData, ingredients: e.target.value })}
                placeholder="Nguyên liệu (cách nhau bởi dấu phẩy)"
                style={{
                  width: '100%',
                  padding: '12px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px'
                }}
              />
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontWeight: '500',
                color: '#333'
              }}>
                Thời gian chuẩn bị (phút)
              </label>
              <input
                type="number"
                value={formData.preparationTime}
                onChange={(e) => setFormData({ ...formData, preparationTime: e.target.value })}
                placeholder="Thời gian chuẩn bị"
                style={{
                  width: '100%',
                  padding: '12px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px'
                }}
              />
            </div>
            
            <div style={{
              display: 'flex',
              gap: '12px',
              justifyContent: 'flex-end',
              marginTop: '24px'
            }}>
              <button
                onClick={() => setIsModalOpen(false)}
                style={{
                  padding: '12px 24px',
                  border: 'none',
                  borderRadius: '8px',
                  fontSize: '14px',
                  fontWeight: '600',
                  cursor: 'pointer',
                  background: '#6c757d',
                  color: 'white'
                }}
              >
                Hủy
              </button>
              <button
                onClick={handleSaveDish}
                style={{
                  padding: '12px 24px',
                  border: 'none',
                  borderRadius: '8px',
                  fontSize: '14px',
                  fontWeight: '600',
                  cursor: 'pointer',
                  background: theme.primary || '#007bff',
                  color: 'white'
                }}
              >
                {editingDish ? 'Cập nhật' : 'Thêm món'}
              </button>
            </div>
          </div>
        </div>
      )}
      {optionDish && (
        <ProductOptionManager
          product={optionDish}
          primary={theme.primary}
          onClose={() => setOptionDish(null)}
          onSaved={() => {
            setDishes((current) => current.map((dish) => (
              dish.id === optionDish.id ? { ...dish, customizable: true } : dish
            )));
          }}
        />
      )}
    </div>
  );
};

export default MenuManagement;
