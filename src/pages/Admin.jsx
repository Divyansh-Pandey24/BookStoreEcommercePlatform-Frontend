import { useState, useEffect } from "react";
import API, { getImageUrl } from "../utils/api";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import "./Admin.css";

const EMPTY_BOOK = {
  title: "", author: "", genre: "", description: "",
  price: "", stock: "", isbn: "",
  publisher: "", language: "English", featured: false,
};

const ORDER_STATUSES = ["PLACED", "CONFIRMED", "DISPATCHED", "DELIVERED", "CANCELLED"];

const STATUS_META = {
  PLACED:     { color: "#856404", bg: "#fff3cd", label: "Placed"     },
  CONFIRMED:  { color: "#004085", bg: "#cce5ff", label: "Confirmed"  },
  DISPATCHED: { color: "#6f42c1", bg: "#e2d9f3", label: "Dispatched" },
  DELIVERED:  { color: "#155724", bg: "#d4edda", label: "Delivered"  },
  CANCELLED:  { color: "#721c24", bg: "#f8d7da", label: "Cancelled"  },
};

function StatusBadge({ status }) {
  const meta = STATUS_META[status] || { color: "#555", bg: "#eee", label: status };
  return (
    <span style={{
      display: "inline-block", padding: "3px 12px", borderRadius: 20,
      fontSize: "0.73rem", fontWeight: 700, letterSpacing: "0.5px",
      textTransform: "uppercase", color: meta.color, backgroundColor: meta.bg,
    }}>
      {meta.label}
    </span>
  );
}

function StatCard({ icon, label, value, sub }) {
  return (
    <div className="stat-card">
      <div className="stat-icon">{icon}</div>
      <div className="stat-body">
        <div className="stat-value">{value}</div>
        <div className="stat-label">{label}</div>
        {sub && <div className="stat-sub">{sub}</div>}
      </div>
    </div>
  );
}

export default function Admin() {
  const { isAdmin } = useAuth();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState("dashboard");
  const [books, setBooks] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);

  // Book form
  const [showBookForm, setShowBookForm] = useState(false);
  const [editingBook, setEditingBook] = useState(null);
  const [bookForm, setBookForm] = useState(EMPTY_BOOK);
  const [savingBook, setSavingBook] = useState(false);

  // Stock modal
  const [stockBook, setStockBook] = useState(null);
  const [stockQty, setStockQty] = useState("");
  const [savingStock, setSavingStock] = useState(false);

  // Order detail modal
  const [selectedOrder, setSelectedOrder] = useState(null);

  // Order filter
  const [statusFilter, setStatusFilter] = useState("ALL");

  useEffect(() => {
    if (!isAdmin) { toast.error("Access denied. Admins only."); navigate("/"); }
  }, [isAdmin]);

  // Load data whenever tab changes
  useEffect(() => {
    if (activeTab === "books" || activeTab === "dashboard") loadBooks();
    if (activeTab === "orders" || activeTab === "dashboard") loadOrders();
  }, [activeTab]);

  async function loadBooks() {
    try {
      setLoading(true);
      const res = await API.get("/api/books");
      setBooks(res.data);
    } catch { toast.error("Failed to load books"); }
    finally { setLoading(false); }
  }

  async function loadOrders() {
    try {
      setLoading(true);
      const res = await API.get("/api/orders/admin/all");
      setOrders(res.data);
    } catch { toast.error("Failed to load orders"); }
    finally { setLoading(false); }
  }

  // ─── Book actions ────────────────────────────────────
  function openAddBook() { setEditingBook(null); setBookForm(EMPTY_BOOK); setShowBookForm(true); }
  function openEditBook(book) {
    setEditingBook(book);
    setBookForm({
      title: book.title || "", author: book.author || "", genre: book.genre || "",
      description: book.description || "", price: book.price || "",
      stock: book.stock || "", isbn: book.isbn || "",
      publisher: book.publisher || "", language: book.language || "English",
      featured: book.featured || false,
    });
    setShowBookForm(true);
  }

  function handleFormChange(e) {
    const { name, value, type, checked } = e.target;
    setBookForm(prev => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
  }

  async function handleSaveBook(e) {
    e.preventDefault();
    if (!bookForm.title || !bookForm.author || !bookForm.price) {
      toast.error("Title, Author and Price are required"); return;
    }
    try {
      setSavingBook(true);
      const payload = { ...bookForm, price: parseFloat(bookForm.price), stock: parseInt(bookForm.stock) || 0 };
      if (editingBook) { await API.put(`/api/books/${editingBook.bookId}`, payload); toast.success("Book updated!"); }
      else             { await API.post("/api/books", payload); toast.success("Book added!"); }
      setShowBookForm(false); loadBooks();
    } catch (err) { toast.error(err.response?.data?.message || "Failed to save book"); }
    finally { setSavingBook(false); }
  }

  async function handleDeleteBook(bookId, title) {
    if (!window.confirm(`Delete "${title}"? This cannot be undone.`)) return;
    try { await API.delete(`/api/books/${bookId}`); toast.success("Book deleted"); loadBooks(); }
    catch { toast.error("Failed to delete book"); }
  }

  async function handleToggleFeatured(bookId) {
    try { await API.patch(`/api/books/${bookId}/featured`); toast.success("Featured updated"); loadBooks(); }
    catch { toast.error("Failed to update"); }
  }

  function openStockModal(book) { setStockBook(book); setStockQty(book.stock ?? ""); }

  async function handleUpdateStock() {
    if (stockQty === "" || isNaN(stockQty) || parseInt(stockQty) < 0) {
      toast.error("Enter a valid stock quantity"); return;
    }
    try {
      setSavingStock(true);
      await API.patch(`/api/books/${stockBook.bookId}/stock`, null, { params: { quantity: parseInt(stockQty) } });
      toast.success("Stock updated!");
      setStockBook(null);
      loadBooks();
    } catch { toast.error("Failed to update stock"); }
    finally { setSavingStock(false); }
  }

  // ─── Order actions ────────────────────────────────────
  async function handleUpdateOrderStatus(orderId, newStatus) {
    try {
      await API.patch(`/api/orders/admin/${orderId}/status`, { status: newStatus });
      toast.success(`Order #${orderId} → ${newStatus}`);
      loadOrders();
      if (selectedOrder?.orderId === orderId) {
        setSelectedOrder(prev => ({ ...prev, orderStatus: newStatus }));
      }
    } catch { toast.error("Failed to update order status"); }
  }

  if (!isAdmin) return null;

  // ─── Derived stats ────────────────────────────────────
  const totalRevenue = orders
    .filter(o => o.orderStatus !== "CANCELLED")
    .reduce((s, o) => s + (o.totalAmount || 0), 0);
  const outOfStock = books.filter(b => (b.stock ?? 0) === 0).length;
  const pendingOrders = orders.filter(o => o.orderStatus === "PLACED").length;

  const filteredOrders = statusFilter === "ALL"
    ? orders
    : orders.filter(o => o.orderStatus === statusFilter);

  // ─── Format date ─────────────────────────────────────
  function fmtDate(dt) {
    if (!dt) return "—";
    return new Date(dt).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
  }

  return (
    <div className="admin-page">

      {/* ── Sidebar ── */}
      <aside className="admin-sidebar">
        <div className="sidebar-brand">
          <span className="sidebar-logo">⚙</span>
          <span className="sidebar-name">Admin</span>
        </div>
        <nav className="sidebar-nav">
          {[
            { id: "dashboard", icon: "◈", label: "Dashboard" },
            { id: "books",     icon: "📚", label: "Books"    },
            { id: "orders",    icon: "📦", label: "Orders"   },
          ].map(tab => (
            <button
              key={tab.id}
              className={`sidebar-link ${activeTab === tab.id ? "active" : ""}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <span className="sidebar-link-icon">{tab.icon}</span>
              {tab.label}
            </button>
          ))}
        </nav>
        <button className="sidebar-back" onClick={() => navigate("/")}>← Back to Site</button>
      </aside>

      {/* ── Main content ── */}
      <main className="admin-main">

        {/* ══════════════ DASHBOARD ══════════════ */}
        {activeTab === "dashboard" && (
          <div className="admin-section fade-in">
            <h1 className="admin-heading">Dashboard</h1>
            <div className="stats-grid">
              <StatCard icon="📚" label="Total Books"    value={books.length}              sub={`${outOfStock} out of stock`} />
              <StatCard icon="📦" label="Total Orders"   value={orders.length}             sub={`${pendingOrders} pending`} />
              <StatCard icon="💰" label="Total Revenue"  value={`₹${totalRevenue.toFixed(0)}`} sub="Excluding cancelled" />
              <StatCard icon="🚚" label="Dispatched"     value={orders.filter(o=>o.orderStatus==="DISPATCHED").length} sub="In transit" />
            </div>

            <div className="dash-row">
              <div className="dash-panel">
                <h2 className="dash-panel-title">Orders by Status</h2>
                <div className="status-breakdown">
                  {ORDER_STATUSES.map(s => {
                    const cnt = orders.filter(o => o.orderStatus === s).length;
                    const pct = orders.length ? Math.round((cnt / orders.length) * 100) : 0;
                    const meta = STATUS_META[s];
                    return (
                      <div key={s} className="status-row">
                        <span className="status-row-label">{meta.label}</span>
                        <div className="status-row-bar-wrap">
                          <div className="status-row-bar" style={{ width: `${pct}%`, background: meta.color, opacity: 0.7 }} />
                        </div>
                        <span className="status-row-count" style={{ color: meta.color }}>{cnt}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="dash-panel">
                <h2 className="dash-panel-title">Recent Orders</h2>
                <div className="recent-orders-list">
                  {orders.slice(0, 5).map(o => (
                    <div key={o.orderId} className="recent-order-row" onClick={() => setSelectedOrder(o)}>
                      <div>
                        <div className="ro-id">#{o.orderId}</div>
                        <div className="ro-date">{fmtDate(o.placedAt)}</div>
                      </div>
                      <StatusBadge status={o.orderStatus} />
                      <div className="ro-amount">₹{o.totalAmount?.toFixed(0)}</div>
                    </div>
                  ))}
                  {orders.length === 0 && <p className="admin-empty">No orders yet.</p>}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ══════════════ BOOKS ══════════════ */}
        {activeTab === "books" && (
          <div className="admin-section fade-in">
            <div className="section-header">
              <h1 className="admin-heading">Manage Books</h1>
              <button className="btn-primary" onClick={openAddBook}>+ Add New Book</button>
            </div>

            {loading ? <p className="admin-loading">Loading books…</p> : (
              <div className="admin-table-wrapper">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>ID</th><th>Title</th><th>Author</th><th>Genre</th>
                      <th>Price</th><th>Stock</th><th>Featured</th><th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {books.map(book => {
                      const stock = book.stock ?? 0;
                      return (
                        <tr key={book.bookId}>
                          <td className="td-id">#{book.bookId}</td>
                          <td className="td-title">{book.title}</td>
                          <td>{book.author}</td>
                          <td><span className="genre-tag">{book.genre}</span></td>
                          <td>₹{book.price?.toFixed(2)}</td>
                          <td>
                            <button className={`stock-btn ${stock === 0 ? "stock-zero" : "stock-ok"}`}
                              onClick={() => openStockModal(book)} title="Click to update stock">
                              {stock} ✎
                            </button>
                          </td>
                          <td>
                            <button className={`featured-toggle ${book.featured ? "yes" : "no"}`}
                              onClick={() => handleToggleFeatured(book.bookId)}>
                              {book.featured ? "★ Yes" : "☆ No"}
                            </button>
                          </td>
                          <td className="td-actions">
                            <button className="edit-btn" onClick={() => openEditBook(book)}>Edit</button>
                            <button className="delete-btn" onClick={() => handleDeleteBook(book.bookId, book.title)}>Delete</button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
                {books.length === 0 && <p className="admin-empty">No books found.</p>}
              </div>
            )}
          </div>
        )}

        {/* ══════════════ ORDERS ══════════════ */}
        {activeTab === "orders" && (
          <div className="admin-section fade-in">
            <div className="section-header">
              <h1 className="admin-heading">All Orders</h1>
              <div className="filter-chip-group">
                {["ALL", ...ORDER_STATUSES].map(s => (
                  <button
                    key={s}
                    className={`filter-chip ${statusFilter === s ? "active" : ""}`}
                    onClick={() => setStatusFilter(s)}
                  >
                    {s === "ALL" ? "All" : STATUS_META[s].label}
                    <span className="chip-count">
                      {s === "ALL" ? orders.length : orders.filter(o => o.orderStatus === s).length}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            {loading ? <p className="admin-loading">Loading orders…</p> : (
              <div className="admin-table-wrapper">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>Order ID</th><th>Customer</th><th>Items</th>
                      <th>Amount</th><th>Payment</th><th>Date</th>
                      <th>Status</th><th>Update Status</th><th>Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredOrders.map(order => (
                      <tr key={order.orderId}>
                        <td className="td-id">#{order.orderId}</td>
                        <td>
                          <div className="td-customer-name">{order.deliveryName || `User #${order.userId}`}</div>
                          <div className="td-customer-sub">{order.deliveryCity}, {order.deliveryState}</div>
                        </td>
                        <td>{order.items?.length ?? 0} book{order.items?.length !== 1 ? "s" : ""}</td>
                        <td className="td-amount">₹{order.totalAmount?.toFixed(2)}</td>
                        <td>
                          <span className={`payment-badge ${order.paymentMode?.toLowerCase()}`}>
                            {order.paymentMode}
                          </span>
                        </td>
                        <td>{fmtDate(order.placedAt)}</td>
                        <td><StatusBadge status={order.orderStatus} /></td>
                        <td>
                          <select
                            className="status-select"
                            value={order.orderStatus}
                            onChange={e => handleUpdateOrderStatus(order.orderId, e.target.value)}
                            disabled={order.orderStatus === "DELIVERED"}
                          >
                            {ORDER_STATUSES.map(s => (
                              <option key={s} value={s}>{s}</option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <button className="detail-btn" onClick={() => setSelectedOrder(order)}>
                            View ›
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {filteredOrders.length === 0 && <p className="admin-empty">No orders for this filter.</p>}
              </div>
            )}
          </div>
        )}
      </main>

      {/* ══════════════ BOOK FORM MODAL ══════════════ */}
      {showBookForm && (
        <div className="modal-overlay" onClick={() => setShowBookForm(false)}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingBook ? "Edit Book" : "Add New Book"}</h3>
              <button className="modal-close" onClick={() => setShowBookForm(false)}>✕</button>
            </div>
            <form onSubmit={handleSaveBook} className="book-form">
              <div className="form-grid">
                {[
                  { name: "title",     label: "Title *",        placeholder: "Book title",           required: true  },
                  { name: "author",    label: "Author *",       placeholder: "Author name",          required: true  },
                  { name: "genre",     label: "Genre *",        placeholder: "e.g. Fiction",         required: true  },
                  { name: "price",     label: "Price (₹) *",    placeholder: "299", type: "number",  required: true  },
                  { name: "stock",     label: "Stock",          placeholder: "50",  type: "number"                   },
                  { name: "isbn",      label: "ISBN",           placeholder: "978-3-16-148410-0"                      },
                  { name: "publisher", label: "Publisher",      placeholder: "Publisher name"                         },
                  { name: "language",  label: "Language",       placeholder: "English"                                },
                ].map(f => (
                  <div className="form-group" key={f.name}>
                    <label>{f.label}</label>
                    <input
                      type={f.type || "text"} name={f.name}
                      value={bookForm[f.name]} onChange={handleFormChange}
                      placeholder={f.placeholder} required={f.required}
                      min={f.type === "number" ? 0 : undefined}
                      step={f.name === "price" ? "0.01" : undefined}
                    />
                  </div>
                ))}
              </div>
              <div className="form-group full-width">
                <label>Description</label>
                <textarea name="description" rows={3} value={bookForm.description}
                  onChange={handleFormChange} placeholder="Book description…" />
              </div>
              <div className="form-checkbox">
                <input type="checkbox" name="featured" id="featured"
                  checked={bookForm.featured} onChange={handleFormChange} />
                <label htmlFor="featured">Mark as Featured</label>
              </div>
              <div className="form-actions">
                <button type="button" className="btn-ghost" onClick={() => setShowBookForm(false)}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={savingBook}>
                  {savingBook ? "Saving…" : editingBook ? "Update Book" : "Add Book"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ══════════════ STOCK MODAL ══════════════ */}
      {stockBook && (
        <div className="modal-overlay" onClick={() => setStockBook(null)}>
          <div className="modal-box modal-sm" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Update Stock</h3>
              <button className="modal-close" onClick={() => setStockBook(null)}>✕</button>
            </div>
            <p className="stock-book-title">"{stockBook.title}"</p>
            <div className="form-group">
              <label>New Stock Quantity</label>
              <input type="number" min="0" value={stockQty}
                onChange={e => setStockQty(e.target.value)}
                placeholder="Enter quantity" autoFocus />
            </div>
            <div className="form-actions" style={{ marginTop: 20 }}>
              <button className="btn-ghost" onClick={() => setStockBook(null)}>Cancel</button>
              <button className="btn-primary" onClick={handleUpdateStock} disabled={savingStock}>
                {savingStock ? "Saving…" : "Update Stock"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ══════════════ ORDER DETAIL MODAL ══════════════ */}
      {selectedOrder && (
        <div className="modal-overlay" onClick={() => setSelectedOrder(null)}>
          <div className="modal-box modal-lg" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Order #{selectedOrder.orderId}</h3>
              <button className="modal-close" onClick={() => setSelectedOrder(null)}>✕</button>
            </div>

            <div className="order-detail-grid">
              {/* Left: Delivery + Payment */}
              <div className="order-detail-col">
                <h4 className="detail-section-title">Delivery Info</h4>
                <div className="detail-info-block">
                  <div className="detail-row"><span>Name</span><strong>{selectedOrder.deliveryName}</strong></div>
                  <div className="detail-row"><span>Mobile</span><strong>{selectedOrder.deliveryMobile}</strong></div>
                  <div className="detail-row"><span>Address</span><strong>{selectedOrder.deliveryAddress}</strong></div>
                  <div className="detail-row"><span>City</span><strong>{selectedOrder.deliveryCity}</strong></div>
                  <div className="detail-row"><span>State</span><strong>{selectedOrder.deliveryState}</strong></div>
                  <div className="detail-row"><span>Pincode</span><strong>{selectedOrder.deliveryPincode}</strong></div>
                </div>

                <h4 className="detail-section-title" style={{ marginTop: 20 }}>Payment & Status</h4>
                <div className="detail-info-block">
                  <div className="detail-row"><span>Amount</span><strong>₹{selectedOrder.totalAmount?.toFixed(2)}</strong></div>
                  <div className="detail-row"><span>Mode</span>
                    <span className={`payment-badge ${selectedOrder.paymentMode?.toLowerCase()}`}>
                      {selectedOrder.paymentMode}
                    </span>
                  </div>
                  <div className="detail-row"><span>Placed</span><strong>{fmtDate(selectedOrder.placedAt)}</strong></div>
                  <div className="detail-row"><span>Status</span><StatusBadge status={selectedOrder.orderStatus} /></div>
                  <div className="detail-row">
                    <span>Update</span>
                    <select
                      className="status-select"
                      value={selectedOrder.orderStatus}
                      disabled={selectedOrder.orderStatus === "DELIVERED"}
                      onChange={e => handleUpdateOrderStatus(selectedOrder.orderId, e.target.value)}
                    >
                      {ORDER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                </div>
              </div>

              {/* Right: Items */}
              <div className="order-detail-col">
                <h4 className="detail-section-title">Items ({selectedOrder.items?.length})</h4>
                <div className="order-items-list">
                  {selectedOrder.items?.map(item => (
                    <div key={item.orderItemId} className="order-item-row">
                      {item.coverImageUrl && (
                        <img className="order-item-img" src={getImageUrl(item.coverImageUrl)}
                          alt={item.bookTitle} onError={e => e.target.style.display = "none"} />
                      )}
                      <div className="order-item-info">
                        <div className="order-item-title">{item.bookTitle}</div>
                        <div className="order-item-meta">Qty: {item.quantity} × ₹{item.price?.toFixed(2)}</div>
                      </div>
                      <div className="order-item-subtotal">₹{item.subtotal?.toFixed(2)}</div>
                    </div>
                  ))}
                </div>
                <div className="order-total-row">
                  <span>Total</span>
                  <strong>₹{selectedOrder.totalAmount?.toFixed(2)}</strong>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}