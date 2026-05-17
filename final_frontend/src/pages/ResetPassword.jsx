import { useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import API from "../utils/api";
import toast from "react-hot-toast";
import "./Login.css";

function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const token = searchParams.get("token");

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [done, setDone] = useState(false);

  // If no token in URL, show error immediately
  useEffect(() => {
    if (!token) {
      toast.error("Invalid or missing reset token");
    }
  }, [token]);

  async function handleSubmit(e) {
    e.preventDefault();

    if (!token) {
      toast.error("Invalid or missing reset token. Please request a new link.");
      return;
    }

    if (!newPassword || !confirmPassword) {
      toast.error("Please fill in all fields");
      return;
    }

    if (newPassword.length < 8) {
      toast.error("Password must be at least 8 characters");
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error("Passwords do not match");
      return;
    }

    try {
      setLoading(true);
      await API.post("/auth/reset-password", {
        token,
        newPassword,
      });
      setDone(true);
      toast.success("Password reset successfully! Please login.");
      setTimeout(() => navigate("/login"), 2500);
    } catch (error) {
      const msg = error.response?.data?.message || error.response?.data || "Failed to reset password";
      toast.error(typeof msg === "string" ? msg : "Token expired or invalid. Please request a new link.");
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-header">
            <span className="auth-logo">⚠️</span>
            <h1 className="auth-title">Invalid Link</h1>
            <p className="auth-subtitle">This password reset link is invalid or has expired.</p>
          </div>
          <div style={{ textAlign: "center", marginTop: "16px" }}>
            <Link to="/forgot-password" className="auth-submit-btn" style={{ display: "inline-block", textDecoration: "none", padding: "14px 24px" }}>
              Request New Link
            </Link>
          </div>
          <div className="auth-footer">
            <p>
              Remember your password?{" "}
              <Link to="/login" className="auth-switch-link">Login</Link>
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <span className="auth-logo">🔑</span>
          <h1 className="auth-title">Reset Password</h1>
          <p className="auth-subtitle">
            {done ? "Your password has been reset!" : "Enter your new password below"}
          </p>
        </div>

        {done ? (
          <div style={{ textAlign: "center", padding: "20px 0", color: "#28a745", fontSize: "1rem" }}>
            ✅ Password reset successfully!<br />
            <span style={{ color: "#888", fontSize: "0.9rem" }}>Redirecting to login...</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="auth-form">
            {/* New Password */}
            <div className="form-group">
              <label className="form-label">New Password</label>
              <div className="input-wrapper">
                <input
                  type={showNew ? "text" : "password"}
                  className="form-input"
                  placeholder="At least 8 characters"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
                <button
                  type="button"
                  className="eye-toggle"
                  onClick={() => setShowNew((v) => !v)}
                  tabIndex={-1}
                >
                  {showNew ? "🙈" : "👁️"}
                </button>
              </div>
            </div>

            {/* Confirm Password */}
            <div className="form-group">
              <label className="form-label">Confirm New Password</label>
              <div className="input-wrapper">
                <input
                  type={showConfirm ? "text" : "password"}
                  className="form-input"
                  placeholder="Repeat your new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
                <button
                  type="button"
                  className="eye-toggle"
                  onClick={() => setShowConfirm((v) => !v)}
                  tabIndex={-1}
                >
                  {showConfirm ? "🙈" : "👁️"}
                </button>
              </div>
              {confirmPassword && newPassword !== confirmPassword && (
                <span style={{ color: "#e74c3c", fontSize: "0.82rem", marginTop: "2px" }}>
                  Passwords do not match
                </span>
              )}
            </div>

            <button type="submit" className="auth-submit-btn" disabled={loading}>
              {loading ? "Resetting..." : "Reset Password"}
            </button>
          </form>
        )}

        <div className="auth-footer">
          <p>
            Remember your password?{" "}
            <Link to="/login" className="auth-switch-link">Login</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default ResetPassword;
