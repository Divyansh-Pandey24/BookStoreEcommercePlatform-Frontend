import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import API, { getImageUrl } from "../utils/api";
import { useAuth } from "../context/AuthContext";
import toast from "react-hot-toast";
import "./BookDetail.css";

function BookDetail() {
  const { bookId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [book, setBook]       = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false);
  const [rating, setRating]   = useState(5);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const [bookRes, reviewRes] = await Promise.all([
          API.get(`/books/${bookId}`),
          API.get(`/reviews/book/${bookId}`),
        ]);
        setBook(bookRes.data);
        setReviews(reviewRes.data);
      } catch {
        toast.error("Book not found");
      } finally { setLoading(false); }
    }
    load();
  }, [bookId]);

  async function handleAddToCart() {
    if (!user) { toast.error("Please login to add to cart"); navigate("/login"); return; }
    try {
      setAddingToCart(true);
      await API.post("/cart/add", { bookId: parseInt(bookId), quantity });
      toast.success("Added to cart! 🛒");
    } catch (e) {
      toast.error(e.response?.data?.message || "Failed to add to cart");
    } finally { setAddingToCart(false); }
  }

  async function handleSubmitReview(e) {
    e.preventDefault();
    if (!user) { toast.error("Please login to review"); navigate("/login"); return; }
    if (!comment.trim()) { toast.error("Please write a comment"); return; }
    try {
      setSubmitting(true);
      await API.post("/reviews", { bookId: parseInt(bookId), rating, comment });
      toast.success("Review submitted!");
      setComment(""); setRating(5);
      const res = await API.get(`/reviews/book/${bookId}`);
      setReviews(res.data);
    } catch (e) {
      toast.error(e.response?.data?.message || "Failed to submit review");
    } finally { setSubmitting(false); }
  }

  function renderStars(r) {
    return Array.from({ length:5 }, (_, i) => (
      <span key={i} className={i < r ? "star filled" : "star"}>★</span>
    ));
  }

  if (loading) return <div className="detail-loading">Loading book details...</div>;
  if (!book)   return <div className="detail-loading">Book not found.</div>;

  // ✅ FIXED: use book.rating and book.stock (not averageRating/stockQuantity)
  const bookRating   = book.rating || 0;
  const bookStock    = book.stock  || 0;
  const inStock      = book.inStock && bookStock > 0;

  return (
    <div className="book-detail-page">
      <div className="detail-container">
        <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>

        <div className="detail-top">
          <div className="detail-cover">
            {book.coverImageUrl
              ? <img src={getImageUrl(book.coverImageUrl)} alt={book.title} />
              : <div className="detail-cover-placeholder">📖</div>
            }
          </div>

          <div className="detail-info">
            <span className="detail-genre">{book.genre}</span>
            <h1 className="detail-title">{book.title}</h1>
            <p className="detail-author">by <strong>{book.author}</strong></p>

            <div className="detail-rating">
              {renderStars(Math.round(bookRating))}
              <span className="rating-text">{bookRating.toFixed(1)} out of 5</span>
            </div>

            <p className="detail-price">₹{book.price?.toFixed(2)}</p>

            {/* ✅ FIXED: use book.stock */}
            <p className="detail-stock">
              {inStock ? `✅ In Stock (${bookStock} available)` : "❌ Out of Stock"}
            </p>

            {book.description && <p className="detail-description">{book.description}</p>}

            {inStock && (
              <div className="detail-cart-section">
                <div className="quantity-control">
                  <button className="qty-btn" onClick={() => setQuantity(q => Math.max(1, q - 1))}>−</button>
                  <span className="qty-value">{quantity}</span>
                  <button className="qty-btn" onClick={() => setQuantity(q => Math.min(bookStock, q + 1))}>+</button>
                </div>
                <button className="add-to-cart-btn" onClick={handleAddToCart} disabled={addingToCart}>
                  {addingToCart ? "Adding..." : "🛒 Add to Cart"}
                </button>
              </div>
            )}

            <div className="detail-meta">
              {book.isbn      && <p><span>ISBN:</span> {book.isbn}</p>}
              {book.publisher && <p><span>Publisher:</span> {book.publisher}</p>}
            </div>
          </div>
        </div>

        {/* Reviews */}
        <div className="reviews-section">
          <h2 className="reviews-title">Customer Reviews ({reviews.length})</h2>

          {user && (
            <div className="review-form-box">
              <h3>Write a Review</h3>
              <form onSubmit={handleSubmitReview} className="review-form">
                <div className="rating-selector">
                  <label>Your Rating:</label>
                  <div className="star-selector">
                    {[1,2,3,4,5].map(s => (
                      <span key={s} className={s <= rating ? "sel-star filled" : "sel-star"} onClick={() => setRating(s)}>★</span>
                    ))}
                  </div>
                </div>
                <textarea placeholder="Share your thoughts about this book..."
                  value={comment} onChange={e => setComment(e.target.value)}
                  className="review-textarea" rows={4} />
                <button type="submit" className="submit-review-btn" disabled={submitting}>
                  {submitting ? "Submitting..." : "Submit Review"}
                </button>
              </form>
            </div>
          )}

          {reviews.length === 0
            ? <p className="no-reviews">No reviews yet. Be the first to review!</p>
            : <div className="reviews-list">
                {reviews.map(review => (
                  <div key={review.reviewId} className="review-card">
                    <div className="review-header">
                      <div className="reviewer-name">
                        {/* ✅ FIXED: use review.reviewerName (not userFullName) */}
                        👤 <strong>{review.reviewerName || "Anonymous"}</strong>
                      </div>
                      <div className="review-stars">{renderStars(review.rating)}</div>
                      <span className="review-date">{new Date(review.createdAt).toLocaleDateString()}</span>
                    </div>
                    <p className="review-comment">{review.comment}</p>
                  </div>
                ))}
              </div>
          }
        </div>
      </div>
    </div>
  );
}

export default BookDetail;