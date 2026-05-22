import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import API from "../utils/api";
import toast from "react-hot-toast";
import "./Orders.css";

// Backend fields: orderId, userId, paymentMode, totalAmount, orderStatus,
// deliveryName, deliveryMobile, deliveryAddress, deliveryCity, deliveryPincode,
// deliveryState, placedAt, updatedAt, items[]
// items: orderItemId, bookId, bookTitle, coverImageUrl, price, quantity, subtotal

const STATUS_COLORS = {
  PLACED:    "#f0a500",
  CONFIRMED: "#1a7fe0",
  SHIPPED:   "#9b59b6",
  DELIVERED: "#28a745",
  CANCELLED: "#e74c3c",
};

const STEPS = ["PLACED", "CONFIRMED", "SHIPPED", "DELIVERED"];

function Tracker({ status }) {
  if (status === "CANCELLED") return <div className="cancelled-banner">❌ Order Cancelled</div>;
  const cur = STEPS.indexOf(status);
  return (
    <div className="tracker">
      {STEPS.map((s, i) => (
        <div key={s} className="tracker-step">
          <div className={`tracker-circle ${i <= cur ? "done" : ""} ${i === cur ? "current" : ""}`}>
            {i < cur ? "✓" : i === cur ? "●" : "○"}
          </div>
          <span className={`tracker-label ${i <= cur ? "done" : ""}`}>{s}</span>
          {i < STEPS.length - 1 && <div className={`tracker-line ${i < cur ? "done" : ""}`} />}
        </div>
      ))}
    </div>
  );
}

function Orders() {
  const [orders, setOrders]     = useState([]);
  const [loading, setLoading]   = useState(true);
  const [expanded, setExpanded] = useState(null);
  const [cancelling, setCancelling] = useState(null);

  useEffect(() => { load(); }, []);

  async function load() {
    try {
      setLoading(true);
      const r = await API.get("/orders/my");
      setOrders(r.data);
    } catch { toast.error("Failed to load orders"); }
    finally { setLoading(false); }
  }

  async function cancel(orderId) {
    if (!window.confirm("Cancel this order?")) return;
    setCancelling(orderId);
    try {
      await API.delete(`/orders/${orderId}/cancel`);
      toast.success("Order cancelled");
      load();
    } catch (e) { toast.error(e.response?.data?.message || "Cannot cancel this order"); }
    finally { setCancelling(null); }
  }

  if (loading) return <div className="orders-loading">Loading your orders...</div>;

  return (
    <div className="orders-page">
      <div className="orders-container">
        <h1 className="orders-title">📦 My Orders</h1>

        {orders.length === 0 ? (
          <div className="orders-empty">
            <span>📦</span>
            <h2>No orders yet</h2>
            <p>Place your first order to see it here.</p>
          </div>
        ) : (
          <div className="orders-list">
            {orders.map(order => {
              const isOpen = expanded === order.orderId;
              // FIXED: use order.orderStatus (not order.status)
              const statusColor = STATUS_COLORS[order.orderStatus] || "#888";
              return (
                <div key={order.orderId} className="order-card">
                  <div className="order-header" onClick={() => setExpanded(isOpen ? null : order.orderId)}>
                    <div className="order-header-left">
                      <span className="order-id">Order #{order.orderId}</span>
                      <span className="order-status" style={{ backgroundColor: statusColor }}>
                        {/* FIXED: order.orderStatus */}
                        {order.orderStatus}
                      </span>
                      <span className="pay-mode-badge">{order.paymentMode}</span>
                    </div>
                    <div className="order-header-right">
                      <span className="order-amount">₹{order.totalAmount?.toFixed(2)}</span>
                      <span className="order-date">
                        {/* FIXED: order.placedAt (not order.createdAt) */}
                        {order.placedAt ? new Date(order.placedAt).toLocaleDateString("en-IN", {
                          day:"numeric", month:"short", year:"numeric"
                        }) : "—"}
                      </span>
                      <span className="expand-icon">{isOpen ? "▲" : "▼"}</span>
                    </div>
                  </div>

                  {isOpen && (
                    <div className="order-details">
                      {/* FIXED: use order.orderStatus for tracker */}
                      <Tracker status={order.orderStatus} />

                      {/* FIXED: items use order.items (not order.orderItems) */}
                      <div className="order-items">
                        {(order.items || []).map(item => (
                          <div key={item.orderItemId} className="order-item">
                            <div className="order-item-info">
                              <Link to={`/books/${item.bookId}`} className="order-item-title">{item.bookTitle}</Link>
                              <span className="order-item-qty">Qty: {item.quantity} × ₹{item.price?.toFixed(2)}</span>
                            </div>
                            <span className="order-item-price">₹{item.subtotal?.toFixed(2)}</span>
                          </div>
                        ))}
                      </div>

                      {/* FIXED: full delivery address with all 5 fields */}
                      <div className="order-address">
                        <p className="addr-header">📍 Delivery Details</p>
                        <p><strong>{order.deliveryName}</strong> · {order.deliveryMobile}</p>
                        <p>{order.deliveryAddress}</p>
                        <p>{order.deliveryCity}, {order.deliveryState} — {order.deliveryPincode}</p>
                      </div>

                      <div className="order-total-row">
                        <span>Total Paid</span>
                        <span className="order-total-amount">₹{order.totalAmount?.toFixed(2)}</span>
                      </div>

                      {/* FIXED: check order.orderStatus (not order.status) */}
                      {(order.orderStatus === "PLACED" || order.orderStatus === "CONFIRMED") && (
                        <button className="cancel-order-btn"
                          onClick={() => cancel(order.orderId)}
                          disabled={cancelling === order.orderId}>
                          {cancelling === order.orderId ? "Cancelling..." : "Cancel Order"}
                        </button>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default Orders;
