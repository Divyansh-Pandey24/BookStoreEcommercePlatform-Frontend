import axios from "axios";

const API_BASE_URL = window.location.hostname === "localhost" 
  ? "http://localhost:8080/api" 
  : "/api";

// Base URL for the gateway WITHOUT /api — used for static file paths like /ebook-uploads/** and /uploads/**
export const GATEWAY_BASE_URL = window.location.hostname === "localhost"
  ? "http://localhost:8080"
  : "";

const IMAGE_BASE_URL = API_BASE_URL;

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

  // EBook local uploads — served at /ebook-uploads/** (no /api prefix)
  const ebookUploadsIndex = normalised.indexOf("ebook-uploads/");
  if (ebookUploadsIndex !== -1) {
    return `${GATEWAY_BASE_URL}/${normalised.substring(ebookUploadsIndex)}`;
  }

  // Book cover images — served at /uploads/** (no /api prefix)
  const uploadsIndex = normalised.indexOf("uploads/");
  if (uploadsIndex !== -1) {
    return `${GATEWAY_BASE_URL}/${normalised.substring(uploadsIndex)}`;
  }

  // Fallback: prepend the full API base URL
  return `${IMAGE_BASE_URL}/${normalised}`;
};

// EBooks API
export const getActiveEBooks = () => API.get("/ebooks/public");
export const getEBooksAdmin = () => API.get("/ebooks/admin");
export const uploadEBookAdmin = (formData) => API.post("/ebooks/admin", formData, {
  headers: { "Content-Type": "multipart/form-data" }
});
export const updateEBookAdmin = (id, formData) => API.put(`/ebooks/admin/${id}`, formData, {
  headers: { "Content-Type": "multipart/form-data" }
});
export const deleteEBookAdmin = (id) => API.delete(`/ebooks/admin/${id}`);
export const getEBookById = (id) => API.get(`/ebooks/user/${id}`);
export const purchaseEBook = (ebookId) => API.post(`/ebooks/user/${ebookId}/purchase`);
export const readEBook = (ebookId) => API.get(`/ebooks/user/${ebookId}/read`);
export const getMyEBookPurchases = () => API.get("/ebooks/user/purchases");
export const getAllEBookPurchasesAdmin = () => API.get("/ebooks/admin/purchases");

// Admin User Management
export const adminGetAllUsers = () => API.get("/auth/admin/users");
export const adminChangeRole = (userId, role) => API.put(`/auth/admin/users/${userId}/role`, { role });
export const adminSuspendUser = (userId, suspended) => API.put(`/auth/admin/users/${userId}/suspend`, { suspended });
export const adminDeleteUser = (userId) => API.delete(`/auth/admin/users/${userId}`);

export default API;