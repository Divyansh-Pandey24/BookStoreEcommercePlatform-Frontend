import React, { useState, useEffect } from "react";
import toast from "react-hot-toast";
import { adminGetAllUsers, adminChangeRole, adminSuspendUser, adminDeleteUser } from "../utils/api";

const SUPER_ADMIN_EMAIL = "divyanshpandey996@gmail.com";

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [deleteConfirmId, setDeleteConfirmId] = useState(null);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const res = await adminGetAllUsers();
      setUsers(res.data);
    } catch {
      toast.error("Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  const handleRoleChange = async (user) => {
    const newRole = user.role === "ADMIN" ? "CUSTOMER" : "ADMIN";
    const action = newRole === "ADMIN" ? "promote to Admin" : "demote to Customer";
    if (!window.confirm(`Are you sure you want to ${action} "${user.fullName}"?`)) return;
    try {
      await adminChangeRole(user.userId, newRole);
      toast.success(`${user.fullName} is now ${newRole}`);
      loadUsers();
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to change role");
    }
  };

  const handleSuspend = async (user) => {
    const action = user.suspended ? "reactivate" : "suspend";
    if (!window.confirm(`Are you sure you want to ${action} "${user.fullName}"?`)) return;
    try {
      await adminSuspendUser(user.userId, !user.suspended);
      toast.success(`Account ${user.suspended ? "reactivated" : "suspended"} successfully`);
      loadUsers();
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to update account status");
    }
  };

  const handleDelete = async (userId) => {
    try {
      await adminDeleteUser(userId);
      toast.success("User permanently deleted");
      setDeleteConfirmId(null);
      loadUsers();
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to delete user");
    }
  };

  const filtered = users.filter(u =>
    u.fullName?.toLowerCase().includes(search.toLowerCase()) ||
    u.email?.toLowerCase().includes(search.toLowerCase())
  );

  const adminCount = users.filter(u => u.role === "ADMIN").length;
  const suspendedCount = users.filter(u => u.suspended).length;

  return (
    <div className="admin-section fade-in">
      <div className="section-header">
        <h1 className="admin-heading">User Management</h1>
        <input
          type="text"
          placeholder="Search by name or email..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{
            padding: "0.5rem 1rem", borderRadius: 8, border: "1px solid #444",
            background: "#1a1a2e", color: "#fff", minWidth: 260, fontSize: "0.9rem"
          }}
        />
      </div>

      {/* Stats */}
      <div className="stats-grid" style={{ marginBottom: "2rem" }}>
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-body">
            <div className="stat-value">{users.length}</div>
            <div className="stat-label">Total Users</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">🛡️</div>
          <div className="stat-body">
            <div className="stat-value">{adminCount}</div>
            <div className="stat-label">Admins</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">🚫</div>
          <div className="stat-body">
            <div className="stat-value">{suspendedCount}</div>
            <div className="stat-label">Suspended</div>
          </div>
        </div>
      </div>

      {loading ? (
        <p className="admin-loading">Loading users…</p>
      ) : (
        <div className="admin-table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Provider</th>
                <th>Role</th>
                <th>Status</th>
                <th>Joined</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(user => {
                const isSuperAdmin = user.email === SUPER_ADMIN_EMAIL;
                const isSuspended = user.suspended;
                return (
                  <tr key={user.userId} style={{ opacity: isSuspended ? 0.6 : 1 }}>
                    <td className="td-id">#{user.userId}</td>
                    <td className="td-title">
                      {user.fullName}
                      {isSuperAdmin && (
                        <span style={{ marginLeft: 6, fontSize: "0.7rem", background: "#ffd43b", color: "#000", borderRadius: 4, padding: "1px 6px", fontWeight: 700 }}>
                          SUPER ADMIN
                        </span>
                      )}
                    </td>
                    <td style={{ fontSize: "0.85rem" }}>{user.email}</td>
                    <td>
                      <span style={{
                        fontSize: "0.75rem", padding: "2px 8px", borderRadius: 4,
                        background: user.provider === "GOOGLE" ? "#4285f4" : "#555", color: "#fff"
                      }}>
                        {user.provider}
                      </span>
                    </td>
                    <td>
                      <span style={{
                        fontSize: "0.75rem", padding: "2px 8px", borderRadius: 4,
                        background: user.role === "ADMIN" ? "#862e9c" : "#2b8a3e", color: "#fff", fontWeight: 600
                      }}>
                        {user.role}
                      </span>
                    </td>
                    <td>
                      <span style={{
                        fontSize: "0.75rem", padding: "2px 8px", borderRadius: 4,
                        background: isSuspended ? "#c92a2a" : "#2b8a3e", color: "#fff"
                      }}>
                        {isSuspended ? "Suspended" : "Active"}
                      </span>
                    </td>
                    <td style={{ fontSize: "0.8rem" }}>
                      {user.createdAt ? new Date(user.createdAt).toLocaleDateString("en-IN") : "—"}
                    </td>
                    <td>
                      {isSuperAdmin ? (
                        <span style={{ color: "#888", fontSize: "0.8rem" }}>Protected</span>
                      ) : (
                        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap" }}>
                          {/* Make Admin / Remove Admin */}
                          <button
                            onClick={() => handleRoleChange(user)}
                            style={{
                              background: user.role === "ADMIN" ? "#5c5f66" : "#862e9c",
                              color: "#fff", border: "none", borderRadius: 4,
                              padding: "4px 8px", cursor: "pointer", fontSize: "0.78rem", fontWeight: 600
                            }}
                          >
                            {user.role === "ADMIN" ? "Revoke Admin" : "Make Admin"}
                          </button>

                          {/* Suspend / Reactivate */}
                          <button
                            onClick={() => handleSuspend(user)}
                            style={{
                              background: isSuspended ? "#2b8a3e" : "#e67700",
                              color: "#fff", border: "none", borderRadius: 4,
                              padding: "4px 8px", cursor: "pointer", fontSize: "0.78rem", fontWeight: 600
                            }}
                          >
                            {isSuspended ? "Reactivate" : "Suspend"}
                          </button>

                          {/* Hard Delete */}
                          {deleteConfirmId === user.userId ? (
                            <>
                              <button
                                onClick={() => handleDelete(user.userId)}
                                style={{ background: "#c92a2a", color: "#fff", border: "none", borderRadius: 4, padding: "4px 8px", cursor: "pointer", fontSize: "0.78rem", fontWeight: 600 }}
                              >
                                Confirm Delete
                              </button>
                              <button
                                onClick={() => setDeleteConfirmId(null)}
                                style={{ background: "#555", color: "#fff", border: "none", borderRadius: 4, padding: "4px 8px", cursor: "pointer", fontSize: "0.78rem" }}
                              >
                                Cancel
                              </button>
                            </>
                          ) : (
                            <button
                              onClick={() => setDeleteConfirmId(user.userId)}
                              style={{ background: "#c92a2a", color: "#fff", border: "none", borderRadius: 4, padding: "4px 8px", cursor: "pointer", fontSize: "0.78rem", fontWeight: 600 }}
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {filtered.length === 0 && <p className="admin-empty">No users found.</p>}
        </div>
      )}
    </div>
  );
}
