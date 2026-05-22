import { createContext, useContext, useState } from "react";

// Context to store logged-in user information and active authentication helper functions
const AuthContext = createContext(null);

// Global provider exposing active user, login function, logout function, and isAdmin status
export function AuthProvider({ children }) {
  // State container initialized with user data parsed from localStorage if it exists
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem("user");
    return stored ? JSON.parse(stored) : null;
  });

  // Save session credentials and active user profile inside localStorage and update state
  function login(userData, accessToken, refreshToken) {
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
    localStorage.setItem("user", JSON.stringify(userData));
    setUser(userData);
  }

  // Clear all session tokens and user data from localStorage and reset user state
  function logout() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    setUser(null);
  }

  // Compute a boolean flag indicating if the current user has administrator privileges
  const isAdmin = user?.role === "ADMIN";

  return (
    <AuthContext.Provider value={{ user, login, logout, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

// Custom hook allowing any consumer component to easily access the AuthContext
export function useAuth() {
  return useContext(AuthContext);
}
