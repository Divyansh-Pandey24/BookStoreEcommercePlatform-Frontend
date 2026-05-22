import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import toast from "react-hot-toast";

// Handles redirect callback from Google OAuth authentication
function OAuth2Success() {
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const accessToken    = searchParams.get("accessToken");
    const refreshToken   = searchParams.get("refreshToken");
    const role           = searchParams.get("role");
    const userId         = searchParams.get("userId");
    const email          = searchParams.get("email");
    const fullName       = searchParams.get("fullName");
    // profilePicture parameter is optional
    const profilePicture = searchParams.get("profilePicture") || null;
    const error          = searchParams.get("error");

    // Backend explicitly sent an error (e.g. Google email not available)
    if (error) {
      toast.error("Google login failed. Please try again.");
      navigate("/login", { replace: true });
      return;
    }

    // Verify that access token and user ID parameters are populated
    if (!accessToken || !userId) {
      toast.error("Google login response was incomplete. Please try again.");
      navigate("/login", { replace: true });
      return;
    }

    // Save session state exactly like a normal login
    login(
      { userId: Number(userId), role, fullName, email, profilePicture },
      accessToken,
      refreshToken
    );

    toast.success(`Welcome, ${fullName?.split(" ")[0] || "User"}! 👋`);

    // Replace browser history state to avoid back button issues
    navigate("/", { replace: true });
  }, [searchParams]);

  return (
    <div style={{
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      height: "60vh",
      flexDirection: "column",
      gap: 16,
    }}>
      <div style={{
        width: 48,
        height: 48,
        border: "4px solid #e8e8e8",
        borderTopColor: "#c9a84c",
        borderRadius: "50%",
        animation: "spin 0.8s linear infinite",
      }} />
      <p style={{ color: "#666", fontWeight: 700, fontSize: "1rem" }}>
        Completing Google login…
      </p>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

export default OAuth2Success;
