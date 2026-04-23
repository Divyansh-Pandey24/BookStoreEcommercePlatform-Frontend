import axios from "axios";

const API_BASE_URL = "http://localhost:8080";

// Images also go through the gateway (8080).
// Route 9 in api-gateway application.properties correctly
// forwards /uploads/** → BOOK-SERVICE, so no need for direct port 8082.
const IMAGE_BASE_URL = "http://localhost:8080";

const API = axios.create({
  baseURL: API_BASE_URL,
});

// Attach JWT token
API.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 unauthorized
const NO_REDIRECT_PATHS = [
  "/api/auth/forgot-password",
  "/api/auth/reset-password",
];

API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || "";
      const isPublic = NO_REDIRECT_PATHS.some((p) => requestUrl.includes(p));
      if (!isPublic) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

/**
 * Convert a stored coverImageUrl to a full, browser-loadable URL.
 *
 * The backend may store:
 *   1. A relative path  →  "uploads/books/book_1.jpg"
 *   2. A legacy absolute Windows path  →  "D:/BookNest/uploads/books/book_1.jpg"
 *
 * In both cases we extract the "uploads/books/..." portion and prepend
 * the gateway base URL so the browser requests:
 *   http://localhost:8080/uploads/books/book_1.jpg
 *
 * The gateway (Route 9) forwards /uploads/** to the book-service, which
 * serves the file via its WebMvcConfigurer resource handler.
 */
export const getImageUrl = (coverImageUrl) => {
  if (!coverImageUrl) return null;

  // Already a full URL — return as-is (handles http / https)
  if (coverImageUrl.startsWith("http://") || coverImageUrl.startsWith("https://")) {
    return coverImageUrl;
  }

  // Normalise Windows backslashes to forward slashes
  const normalised = coverImageUrl.replace(/\\/g, "/");

  // Extract the "uploads/..." segment whether the stored value is
  // absolute ("D:/BookNest/uploads/books/f.jpg") or relative ("uploads/books/f.jpg")
  const uploadsIndex = normalised.indexOf("uploads/");
  if (uploadsIndex !== -1) {
    return `${IMAGE_BASE_URL}/${normalised.substring(uploadsIndex)}`;
  }

  // Fallback: just prepend the base URL
  return `${IMAGE_BASE_URL}/${normalised}`;
};

export default API;