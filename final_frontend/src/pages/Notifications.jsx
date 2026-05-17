import { useState, useEffect } from "react";
import API from "../utils/api";
import toast from "react-hot-toast";
import "./Notifications.css";

function getIcon(type) {
  if (!type) return "🔔";
  const t = type.toUpperCase();
  if (t.includes("ORDER"))   return "📦";
  if (t.includes("PAYMENT") || t.includes("WALLET")) return "💳";
  if (t.includes("CANCEL"))  return "❌";
  if (t.includes("DELIVER")) return "🚚";
  return "🔔";
}

function Notifications() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading]             = useState(true);

  useEffect(() => { load(); }, []);

  async function load() {
    try {
      setLoading(true);
      const res = await API.get("/notifications");
      setNotifications(res.data);
    } catch { toast.error("Failed to load notifications"); }
    finally { setLoading(false); }
  }

  async function markRead(id) {
    try {
      await API.patch(`/notifications/${id}/read`);
      setNotifications(prev => prev.map(n => n.notificationId === id ? { ...n, isRead: true } : n));
    } catch { toast.error("Failed to mark as read"); }
  }

  async function markAllRead() {
    try {
      await API.patch("/notifications/read-all");
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      toast.success("All notifications marked as read");
    } catch { toast.error("Failed"); }
  }

  async function deleteNotif(id) {
    try {
      await API.delete(`/notifications/${id}`);
      setNotifications(prev => prev.filter(n => n.notificationId !== id));
    } catch { toast.error("Failed to delete"); }
  }

  const unreadCount = notifications.filter(n => !n.isRead).length;

  if (loading) return <div className="notif-loading">Loading notifications...</div>;

  return (
    <div className="notif-page">
      <div className="notif-container">
        <div className="notif-header-row">
          <div>
            <h1 className="notif-title">🔔 Notifications</h1>
            {unreadCount > 0 && <span className="unread-badge">{unreadCount} unread</span>}
          </div>
          {unreadCount > 0 && (
            <button className="mark-all-btn" onClick={markAllRead}>Mark all as read ✓</button>
          )}
        </div>

        {notifications.length === 0 ? (
          <div className="notif-empty">
            <span>🔔</span>
            <h2>No notifications</h2>
            <p>You're all caught up! Notifications about orders and payments will appear here.</p>
          </div>
        ) : (
          <div className="notif-list">
            {notifications.map(notif => (
              <div key={notif.notificationId} className={`notif-item ${!notif.isRead ? "unread" : ""}`}>
                <div className="notif-icon">{getIcon(notif.type)}</div>
                <div className="notif-body">
                  <p className="notif-message">{notif.message}</p>
                  <span className="notif-time">{new Date(notif.createdAt).toLocaleString()}</span>
                </div>
                <div className="notif-actions">
                  <span className="notif-type-badge">{notif.type || "INFO"}</span>
                  {!notif.isRead && (
                    <button className="read-btn" onClick={() => markRead(notif.notificationId)} title="Mark as read">✓</button>
                  )}
                  <button className="del-btn" onClick={() => deleteNotif(notif.notificationId)} title="Delete">✕</button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default Notifications;
