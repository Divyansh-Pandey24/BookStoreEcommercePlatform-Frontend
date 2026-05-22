import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import API from "../utils/api";
import BookCard from "../components/BookCard";
import "./Home.css";

// Predefined list of book genres available for quick browsing and filtering
const GENRES = ["Fiction", "Non-Fiction", "Mystery", "Fantasy", "Science Fiction", "Biography", "Romance", "Thriller", "History", "Self-Help"];

function Home() {
  // State array storing list of books designated as featured
  const [featuredBooks, setFeaturedBooks] = useState([]);
  // State array storing all active books retrieved from server
  const [allBooks, setAllBooks] = useState([]);
  // State indicating active fetch processes in progress
  const [loading, setLoading] = useState(true);
  // State container for active search text input
  const [searchInput, setSearchInput] = useState("");
  // Hook to programmatically route the user
  const navigate = useNavigate();

  // Fetch featured and catalog book data asynchronously on component load
  useEffect(() => {
    console.log("🏠 Home component rendered, starting data fetch...");
    async function loadData() {
      try {
        console.log("📡 Sending API requests to /books/featured and /books...");
        // Execute concurrent API calls for both featured and normal book lists
        const [featuredRes, allRes] = await Promise.all([
          API.get("/books/featured"),
          API.get("/books"),
        ]);
        console.log("📡 Raw Featured Response:", featuredRes.data);
        console.log("📡 Raw All Books Response:", allRes.data);
        // Safely retrieve array formats from responses or content properties
        const featuredData = Array.isArray(featuredRes.data) ? featuredRes.data : (featuredRes.data?.content || []);
        const allData = Array.isArray(allRes.data) ? allRes.data : (allRes.data?.content || []);
        console.log("✅ Parsed Data! Featured:", featuredData.length, "Total:", allData.length);
        setFeaturedBooks(featuredData);
        setAllBooks(allData);
      } catch (error) {
        console.error("❌ API Error:", error);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  // Route user to browse view with search criteria query string parameter
  function handleSearch(e) {
    e.preventDefault();
    if (searchInput.trim()) {
      navigate(`/books?search=${encodeURIComponent(searchInput.trim())}`);
    }
  }

  // Route user to browse view filtered specifically by selected genre
  function handleGenreClick(genre) {
    navigate(`/books?genre=${encodeURIComponent(genre)}`);
  }

  return (
    <div className="home">
      <section className="hero">
        <div className="hero-content">
          <h1 className="hero-title">
            Find Your Next <span className="hero-highlight">Great Read</span>
          </h1>
          <p className="hero-subtitle">
            Search our collection and find the perfect book for your next adventure.
          </p>

          <form className="hero-search" onSubmit={handleSearch}>
            <input
              type="text"
              placeholder="Search books, authors, genres..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="hero-search-input"
            />
            <button type="submit" className="hero-search-btn">
              Search
            </button>
          </form>

          <Link to="/books" className="hero-browse-btn">
            Browse All Books →
          </Link>
        </div>

        <div className="hero-decoration">
          {/* Decorative elements removed as per user request */}
        </div>
      </section>

      <section className="genre-section">
        <div className="section-container">
          <h2 className="section-title">Browse by Genre</h2>
          <div className="genre-grid">
            {GENRES.map((genre) => (
              <button
                key={genre}
                className="genre-btn"
                onClick={() => handleGenreClick(genre)}
              >
                {genre}
              </button>
            ))}
          </div>
        </div>
      </section>

      {featuredBooks.length > 0 && (
        <section className="books-section">
          <div className="section-container">
            <div className="section-header">
              <h2 className="section-title">Featured Books</h2>
              <Link to="/books" className="see-all-link">See All →</Link>
            </div>

            {loading ? (
              <p className="loading-text">Loading...</p>
            ) : (
              <div className="books-grid">
                {Array.isArray(featuredBooks) && featuredBooks.slice(0, 8).map((book) => (
                  <BookCard key={book.bookId} book={book} />
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      <section className="books-section alt-bg">
        <div className="section-container">
          <div className="section-header">
            <h2 className="section-title">All Books</h2>
            <Link to="/books" className="see-all-link">See All →</Link>
          </div>

          {loading ? (
            <p className="loading-text">Loading...</p>
          ) : allBooks.length === 0 ? (
            <p className="empty-text">No books available at this time.</p>
          ) : (
            <div className="books-grid">
              {Array.isArray(allBooks) && allBooks
                .filter(b => Array.isArray(featuredBooks) && !featuredBooks.some(f => f.bookId === b.bookId))
                .slice(0, 8)
                .map((book) => (
                  <BookCard key={book.bookId} book={book} />
                ))
              }
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

export default Home;