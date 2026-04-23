import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import API from "../utils/api";
import BookCard from "../components/BookCard";
import "./Home.css";

const GENRES = ["Fiction", "Non-Fiction", "Mystery", "Fantasy", "Science Fiction", "Biography", "Romance", "Thriller", "History", "Self-Help"];

function Home() {
  const [featuredBooks, setFeaturedBooks] = useState([]);
  const [allBooks, setAllBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchInput, setSearchInput] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    async function loadData() {
      try {
        const [featuredRes, allRes] = await Promise.all([
          API.get("/api/books/featured"),
          API.get("/api/books"),
        ]);
        setFeaturedBooks(featuredRes.data);
        setAllBooks(allRes.data);
      } catch (error) {
        console.error("Failed to load books", error);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  function handleSearch(e) {
    e.preventDefault();
    if (searchInput.trim()) {
      navigate(`/books?search=${encodeURIComponent(searchInput.trim())}`);
    }
  }

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
                {featuredBooks.slice(0, 8).map((book) => (
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
              {allBooks
                .filter(b => !featuredBooks.some(f => f.bookId === b.bookId))
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