import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./Navbar.css";

function Navbar() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo">
          <span className="logo-text">BookNest</span>
        </Link>

        <div className="navbar-links">
          <div className="nav-main-links">
            <Link to="/" className="nav-link">Home</Link>
            <Link to="/books" className="nav-link">Browse Books</Link>

            {user && (
              <>
                <Link to="/cart" className="nav-link">Cart</Link>
                <Link to="/orders" className="nav-link">Orders</Link>
                <Link to="/wallet" className="nav-link">Wallet</Link>
                <Link to="/notifications" className="nav-link">Notifications</Link>
              </>
            )}

            {isAdmin && (
              <Link to="/admin" className="nav-link admin-link">Admin</Link>
            )}
          </div>

          <div className="navbar-auth">
            {user ? (
              <div className="user-section">
                <Link to="/profile" className="user-name">
                  {user.fullName || user.username || "Profile"}
                </Link>
                <button onClick={handleLogout} className="btn-logout">
                  Logout
                </button>
              </div>
            ) : (
              <div className="auth-buttons">
                <Link to="/login" className="btn-login">Login</Link>
                <Link to="/register" className="btn-register">Register</Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
