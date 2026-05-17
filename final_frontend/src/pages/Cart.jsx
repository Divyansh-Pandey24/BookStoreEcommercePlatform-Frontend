import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import API, { getImageUrl } from "../utils/api";   // ← Updated import
import { useAuth } from "../context/AuthContext";
import toast from "react-hot-toast";
import "./Cart.css";

const EMPTY_ADDR = { 
  deliveryName: "", 
  deliveryMobile: "", 
  deliveryAddress: "", 
  deliveryCity: "", 
  deliveryPincode: "", 
  deliveryState: "" 
};

function Cart() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showCheckout, setShowCheckout] = useState(false);
  const [paymentMode, setPaymentMode] = useState("COD");
  const [addr, setAddr] = useState(EMPTY_ADDR);
  const [placing, setPlacing] = useState(false);

  useEffect(() => { loadCart(); }, []);

  async function loadCart() {
    try {
      setLoading(true);
      const res = await API.get("/cart");
      setCart(res.data);
    } catch { 
      toast.error("Failed to load cart"); 
    } finally { 
      setLoading(false); 
    }
  }

  async function handleRemove(itemId) {
    try {
      const res = await API.delete(`/cart/item/${itemId}`);
      setCart(res.data); 
      toast.success("Item removed");
    } catch { 
      toast.error("Failed to remove item"); 
    }
  }

  async function handleQty(itemId, newQty) {
    if (newQty < 1) return;
    try {
      const res = await API.patch(`/cart/item/${itemId}?quantity=${newQty}`);
      setCart(res.data);
    } catch (e) { 
      toast.error(e.response?.data?.message || "Failed"); 
    }
  }

  async function handleClear() {
    try { 
      await API.delete("/cart/clear"); 
      toast.success("Cart cleared"); 
      loadCart(); 
    } catch { 
      toast.error("Failed to clear cart"); 
    }
  }

  async function handlePlaceOrder() {
    const missing = Object.entries(addr).find(([k, v]) => !v.trim());
    if (missing) { 
      toast.error(`Please fill in ${missing[0].replace("delivery","")}`); 
      return; 
    }
    try {
      setPlacing(true);
      await API.post("/orders/place", { ...addr, paymentMode });
      toast.success("Order placed successfully! 🎉");
      setShowCheckout(false);
      navigate("/orders");
    } catch (e) {
      toast.error(e.response?.data?.error || e.response?.data?.message || "Failed to place order");
    } finally { 
      setPlacing(false); 
    }
  }

  if (loading) return <div className="cart-loading">Loading your cart...</div>;

  const items = cart?.items || [];
  const total = items.reduce((s, i) => s + i.price * i.quantity, 0);

  return (
    <div className="cart-page">
      <div className="cart-container">
        <h1 className="cart-title">🛒 Your Cart</h1>

        {items.length === 0 ? (
          <div className="cart-empty">
            <span className="empty-icon">🛒</span>
            <h2>Your cart is empty</h2>
            <p>Add some books to get started!</p>
            <Link to="/books" className="browse-btn">Browse Books</Link>
          </div>
        ) : (
          <div className="cart-layout">
            <div className="cart-items">
              <div className="cart-items-header">
                <span>{items.length} item(s) in your cart</span>
                <button className="clear-cart-btn" onClick={handleClear}>Clear All</button>
              </div>

              {items.map(item => (
                <div key={item.itemId} className="cart-item">
                  <div className="item-image">
                    {item.coverImageUrl ? (
                      <img 
                        src={getImageUrl(item.coverImageUrl)} 
                        alt={item.bookTitle}
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = "https://via.placeholder.com/300x400/cccccc/666666?text=No+Image";
                        }}
                      />
                    ) : (
                      <div className="item-image-placeholder">📖</div>
                    )}
                  </div>
                  <div className="item-details">
                    <h3 className="item-title">{item.bookTitle}</h3>
                    <p className="item-unit-price">₹{item.price?.toFixed(2)} each</p>
                  </div>
                  <div className="item-qty-control">
                    <button className="qty-btn" onClick={() => handleQty(item.itemId, item.quantity - 1)}>−</button>
                    <span className="qty-display">{item.quantity}</span>
                    <button className="qty-btn" onClick={() => handleQty(item.itemId, item.quantity + 1)}>+</button>
                  </div>
                  <div className="item-subtotal">₹{(item.price * item.quantity).toFixed(2)}</div>
                  <button className="remove-item-btn" onClick={() => handleRemove(item.itemId)}>✕</button>
                </div>
              ))}
            </div>

            <div className="order-summary">
              <h2 className="summary-title">Order Summary</h2>
              <div className="summary-row">
                <span>Subtotal ({items.length} items)</span>
                <span>₹{total.toFixed(2)}</span>
              </div>
              <div className="summary-row">
                <span>Delivery</span>
                <span className="free-delivery">Free</span>
              </div>
              <div className="summary-total">
                <span>Total</span>
                <span>₹{total.toFixed(2)}</span>
              </div>

              <button className="place-order-btn" onClick={() => setShowCheckout(true)}>
                Proceed to Checkout
              </button>
              <Link to="/books" className="continue-shopping">← Continue Shopping</Link>
            </div>
          </div>
        )}
      </div>

      {/* Checkout Modal */}
      {showCheckout && (
        <div className="checkout-overlay" onClick={() => setShowCheckout(false)}>
          <div className="checkout-modal" onClick={e => e.stopPropagation()}>
            <button className="modal-close" onClick={() => setShowCheckout(false)}>✕</button>
            <h2>Complete Your Order</h2>
            <p className="modal-total">Total: ₹{total.toFixed(2)}</p>

            <div className="payment-mode">
              <label className="pm-label">Payment Mode</label>
              <div className="pm-options">
                <label className={`pm-opt ${paymentMode==="COD"?"active":""}`}>
                  <input 
                    type="radio" 
                    value="COD" 
                    checked={paymentMode==="COD"} 
                    onChange={() => setPaymentMode("COD")} 
                  />
                  💵 Cash on Delivery
                </label>
                <label className={`pm-opt ${paymentMode==="WALLET"?"active":""}`}>
                  <input 
                    type="radio" 
                    value="WALLET" 
                    checked={paymentMode==="WALLET"} 
                    onChange={() => setPaymentMode("WALLET")} 
                  />
                  💳 Wallet
                </label>
              </div>
            </div>

            <div className="addr-form">
              <label>Delivery Name *</label>
              <input 
                placeholder="Full name" 
                value={addr.deliveryName} 
                onChange={e => setAddr({...addr, deliveryName: e.target.value})} 
              />
              <label>Mobile *</label>
              <input 
                placeholder="10-digit mobile" 
                value={addr.deliveryMobile} 
                onChange={e => setAddr({...addr, deliveryMobile: e.target.value})} 
              />
              <label>Address *</label>
              <textarea 
                rows={2} 
                placeholder="House no, Street, Area" 
                value={addr.deliveryAddress} 
                onChange={e => setAddr({...addr, deliveryAddress: e.target.value})} 
              />
              <label>City *</label>
              <input 
                placeholder="City" 
                value={addr.deliveryCity} 
                onChange={e => setAddr({...addr, deliveryCity: e.target.value})} 
              />
              <div className="addr-row">
                <div>
                  <label>State *</label>
                  <input 
                    placeholder="State" 
                    value={addr.deliveryState} 
                    onChange={e => setAddr({...addr, deliveryState: e.target.value})} 
                  />
                </div>
                <div>
                  <label>Pincode *</label>
                  <input 
                    placeholder="6-digit" 
                    value={addr.deliveryPincode} 
                    onChange={e => setAddr({...addr, deliveryPincode: e.target.value})} 
                  />
                </div>
              </div>
            </div>

            <button 
              className="place-order-btn" 
              onClick={handlePlaceOrder} 
              disabled={placing}
            >
              {placing ? "Placing Order..." : `Place Order — ₹${total.toFixed(2)}`}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Cart;