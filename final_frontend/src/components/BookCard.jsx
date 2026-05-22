import { Link } from "react-router-dom";
import { getImageUrl } from "../utils/api";
import "./BookCard.css";

function BookCard({ book }) {
  // Extract raw cover image URL from book object
  const rawUrl = book.coverImageUrl;
  // Convert raw URL to full absolute URL if needed
  const imageSrc = getImageUrl(rawUrl);

  // Generate JSX elements representing star ratings
  function renderStars(rating) {
    const stars = [];
    // Round down the rating to determine filled stars count
    const fullStars = Math.floor(rating || 0);
    // Populate star array up to maximum of 5 stars
    for (let i = 1; i <= 5; i++) {
      stars.push(
        <span key={i} className={i <= fullStars ? "star filled" : "star"}>
          ★
        </span>
      );
    }
    return stars;
  }

  return (
    <Link to={`/books/${book.bookId}`} className="book-card">
      <div className="book-cover">
        {imageSrc ? (
          <img 
            src={imageSrc} 
            alt={book.title} 
            loading="lazy"
            onError={(e) => {
              e.target.onerror = null;
              // Inline SVG fallback works offline, no external dependency
              e.target.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='400' viewBox='0 0 300 400'%3E%3Crect width='300' height='400' fill='%23cccccc'/%3E%3Ctext x='50%25' y='48%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='16' fill='%23666666'%3ENo Cover%3C/text%3E%3C/svg%3E";
            }}
          />
        ) : (
          <div className="book-cover-placeholder">
            <span>📖</span>
          </div>
        )}
        {book.featured && <span className="featured-badge">Featured</span>}
      </div>

      <div className="book-info">
        <h3 className="book-title">{book.title}</h3>
        <p className="book-author">by {book.author}</p>
        <span className="book-genre">{book.genre}</span>

        <div className="book-rating">
          {renderStars(book.averageRating)}
          <span className="rating-number">
            ({book.averageRating ? book.averageRating.toFixed(1) : "0.0"})
          </span>
        </div>

        <p className="book-price">₹{book.price?.toFixed(2)}</p>
      </div>
    </Link>
  );
}

export default BookCard;