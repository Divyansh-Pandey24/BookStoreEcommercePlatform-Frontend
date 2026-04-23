import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import toast from "react-hot-toast";

/**
 * OAuth2Success — handles the redirect back from Google OAuth.
 *
 * Flow:
 *   1. User clicks "Continue with Google" on Login page
 *   2. Browser navigates to http://localhost:8081/oauth2/authorization/google
 *   3. Spring handles the Google OAuth dance
 *   4. On success, Spring (GoogleOAuthSuccessHandler) redirects the browser to:
 *      http://localhost:5173/oauth2/success?accessToken=...&userId=...&...
 *   5. THIS component reads those URL params, stores them in AuthContext,
 *      and navigates to the home page.
 *
 * WHY useSearchParams instead of window.location.search:
 *   React Router's <BrowserRouter> manages the URL — using useSearchParams
 *   ensures we read the params from the router's view of the URL, which is
 *   consistent with how navigate() and other hooks work. window.location.search
 *   can occasionally lag behind in SPAs on the first render.
 */
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
    // profilePicture is optional — Google URL may not always be present
    const profilePicture = searchParams.get("profilePicture") || null;
    const error          = searchParams.get("error");

    // Backend explicitly sent an error (e.g. Google email not available)
    if (error) {
      toast.error("Google login failed. Please try again.");
      navigate("/login", { replace: true });
      return;
    }

    // Both accessToken and userId are required — if either is missing,
    // the redirect URL was corrupted or incomplete
    if (!accessToken || !userId) {
      toast.error("Google login response was incomplete. Please try again.");
      navigate("/login", { replace: true });
      return;
    }

    // All good — store the user and tokens exactly like a normal login
    login(
      { userId: Number(userId), role, fullName, email, profilePicture },
      accessToken,
      refreshToken
    );

    toast.success(`Welcome, ${fullName?.split(" ")[0] || "User"}! 👋`);

    // replace: true so the /oauth2/success URL is removed from browser history
    // (pressing Back after login won't re-run this page)
    navigate("/", { replace: true });
  }, [searchParams]); // re-run if searchParams change (they won't, but good practice)

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
