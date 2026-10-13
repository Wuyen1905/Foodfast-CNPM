import React, { lazy, Suspense, useMemo, useEffect, useState } from "react";
import ReactDOM from "react-dom/client";
import { ThemeProvider } from "styled-components";
import { GlobalStyle } from "./theme";
import { GlobalStyle as ResponsiveGlobalStyle, enhancedTheme } from "./globalStyles";
import "./global.css";
import { CartProvider } from "@/context/CartContext";
import { WishlistProvider } from "@/context/WishlistContext";
import { ThemeProvider as CustomThemeProvider } from "@/context/ThemeContext";
import { AuthProvider } from "@/context/AuthContext";
import { OrderProvider } from "@/context/OrderContext";
import { getAllProducts, Product } from "./data/products";
import { Toaster } from "react-hot-toast";
import ErrorBoundary from "./components/common/ErrorBoundary";

const App = lazy(() => import("./pages/App"));
const AdminApp = lazy(() => import("./admin/AdminApp"));

const Root = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadProducts = async () => {
      try {
        const fetchedProducts = await getAllProducts();
        setProducts(fetchedProducts);
      } catch (error) {
        console.error('Error loading products:', error);
      } finally {
        setLoading(false);
      }
    };
    loadProducts();
  }, []);

  const priceMap = useMemo(
    () => Object.fromEntries(products.map((p) => [p.id, p.price])),
    [products]
  );

  return (
    <CustomThemeProvider>
      <ThemeProvider theme={enhancedTheme}>
        <GlobalStyle />
        <ResponsiveGlobalStyle />
        <AuthProvider>
          <OrderProvider>
            <CartProvider priceMap={priceMap}>
              <WishlistProvider>
                <Toaster position="top-right" />
                <App />
              </WishlistProvider>
            </CartProvider>
          </OrderProvider>
        </AuthProvider>
      </ThemeProvider>
    </CustomThemeProvider>
  );
};

const root = ReactDOM.createRoot(document.getElementById("root")!);

// Cache-busting attribute for design token system
document.documentElement.setAttribute('data-skin-version', 'v1');

root.render(
  <React.StrictMode>
    <ErrorBoundary>
      <Suspense fallback={<div role="status" style={{ padding: 48, textAlign: "center" }}>Đang khởi động FoodFast...</div>}>
        {window.location.pathname.startsWith("/admin") ? <AdminApp /> : <Root />}
      </Suspense>
    </ErrorBoundary>
  </React.StrictMode>
);
