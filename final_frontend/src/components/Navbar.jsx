import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./Navbar.css";

function Navbar() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const toggleMenu = () => setIsMenuOpen(!isMenuOpen);
  const closeMenu = () => setIsMenuOpen(false);

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo" onClick={closeMenu}>
          <span className="logo-text">BookNest</span>
        </Link>

        {/* Mobile Toggle Button */}
        <button className="navbar-toggle" onClick={toggleMenu} aria-label="Toggle navigation">
          <span className={`hamburger ${isMenuOpen ? "open" : ""}`}></span>
        </button>

        <div className={`navbar-links ${isMenuOpen ? "active" : ""}`}>
          <div className="nav-main-links">
            <Link to="/" className="nav-link" onClick={closeMenu}>Home</Link>
            <Link to="/books" className="nav-link" onClick={closeMenu}>Browse Books</Link>
            <Link to="/ebooks" className="nav-link" onClick={closeMenu}>EBooks</Link>

            {user && (
              <>
                <Link to="/cart" className="nav-link" onClick={closeMenu}>Cart</Link>
                <Link to="/orders" className="nav-link" onClick={closeMenu}>Orders</Link>
                <Link to="/wallet" className="nav-link" onClick={closeMenu}>Wallet</Link>
                <Link to="/notifications" className="nav-link" onClick={closeMenu}>Notifications</Link>
              </>
            )}

            {isAdmin && (
              <Link to="/admin" className="nav-link admin-link" onClick={closeMenu}>Admin</Link>
            )}
          </div>

          <div className="navbar-auth">
            {user ? (
              <div className="user-section">
                <Link to="/profile" className="user-name" onClick={closeMenu}>
                  {user.fullName || user.username || "Profile"}
                </Link>
                <button onClick={() => { handleLogout(); closeMenu(); }} className="btn-logout">
                  Logout
                </button>
              </div>
            ) : (
              <div className="auth-buttons">
                <Link to="/login" className="btn-login" onClick={closeMenu}>Login</Link>
                <Link to="/register" className="btn-register" onClick={closeMenu}>Register</Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
