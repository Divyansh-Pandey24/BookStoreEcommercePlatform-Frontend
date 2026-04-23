import { createContext, useContext, useState } from "react";

// This context holds the logged-in user info and auth functions
// Any component can use useAuth() to get this data
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Try to load user from localStorage on first render
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem("user");
    return stored ? JSON.parse(stored) : null;
  });

  // Called after successful login or register
  function login(userData, accessToken, refreshToken) {
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
    localStorage.setItem("user", JSON.stringify(userData));
    setUser(userData);
  }

  // Called when user logs out
  function logout() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    setUser(null);
  }

  // Check if logged-in user is an admin
  const isAdmin = user?.role === "ADMIN";

  return (
    <AuthContext.Provider value={{ user, login, logout, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

// Custom hook - use this in any component instead of useContext(AuthContext)
export function useAuth() {
  return useContext(AuthContext);
}
