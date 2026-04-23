import { useState, useEffect } from "react";
import API from "../utils/api";
import { useAuth } from "../context/AuthContext";
import toast from "react-hot-toast";
import "./Profile.css";

function Profile() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  // Forgot password section
  const [showResetForm, setShowResetForm] = useState(false);
  const [resetEmail, setResetEmail] = useState("");
  const [resetSent, setResetSent] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    try {
      setLoading(true);
      const res = await API.get("/api/auth/profile");
      setProfile(res.data);
    } catch (error) {
      toast.error("Failed to load profile");
    } finally {
      setLoading(false);
    }
  }

  async function handleSendReset() {
    if (!resetEmail.trim()) {
      toast.error("Please enter your email");
      return;
    }
    try {
      await API.post("/api/auth/forgot-password", { email: resetEmail });
      setResetSent(true);
      toast.success("Password reset link sent to your email!");
    } catch (error) {
      toast.error("Failed to send reset email");
    }
  }

  if (loading) return <div className="profile-loading">Loading profile...</div>;

  const displayData = profile || user || {};

  return (
    <div className="profile-page">
      <div className="profile-container">
        <h1 className="profile-title">👤 My Profile</h1>

        {/* Profile Card */}
        <div className="profile-card">
          {/* Avatar */}
          <div className="profile-avatar">
            <span className="avatar-letter">
              {(displayData.fullName || displayData.email || "U")[0].toUpperCase()}
            </span>
          </div>

          {/* Info */}
          <div className="profile-info">
            <div className="info-row">
              <span className="info-label">Full Name</span>
              <span className="info-value">{displayData.fullName || "—"}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Email Address</span>
              <span className="info-value">{displayData.email || "—"}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Mobile Number</span>
              <span className="info-value">{displayData.mobile || "—"}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Account Role</span>
              <span className={`role-badge ${displayData.role === "ADMIN" ? "admin" : "customer"}`}>
                {displayData.role || "CUSTOMER"}
              </span>
            </div>
          </div>
        </div>

        {/* Change Password Section */}
        <div className="profile-section-card">
          <div className="section-header-row">
            <h2 className="section-card-title">🔒 Change Password</h2>
            <button
              className="toggle-reset-btn"
              onClick={() => setShowResetForm(!showResetForm)}
            >
              {showResetForm ? "Cancel" : "Change Password"}
            </button>
          </div>

          {showResetForm && (
            <div className="reset-form">
              {resetSent ? (
                <p className="reset-success">
                  ✅ A password reset link has been sent to your email. Check your inbox.
                </p>
              ) : (
                <>
                  <p className="reset-info">
                    We will send a password reset link to your email address.
                  </p>
                  <div className="reset-input-row">
                    <input
                      type="email"
                      className="reset-email-input"
                      placeholder="Enter your email address"
                      value={resetEmail}
                      onChange={(e) => setResetEmail(e.target.value)}
                    />
                    <button className="send-reset-btn" onClick={handleSendReset}>
                      Send Reset Link
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>

        {/* Account Stats */}
        <div className="profile-section-card">
          <h2 className="section-card-title">📊 Quick Links</h2>
          <div className="quick-links">
            <a href="/orders" className="quick-link-item">
              <span className="quick-link-icon">📦</span>
              <span>My Orders</span>
            </a>
            <a href="/cart" className="quick-link-item">
              <span className="quick-link-icon">🛒</span>
              <span>My Cart</span>
            </a>
            <a href="/wallet" className="quick-link-item">
              <span className="quick-link-icon">💳</span>
              <span>My Wallet</span>
            </a>
            <a href="/notifications" className="quick-link-item">
              <span className="quick-link-icon">🔔</span>
              <span>Notifications</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Profile;
