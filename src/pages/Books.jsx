import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import API from "../utils/api";
import BookCard from "../components/BookCard";
import "./Books.css";

const GENRES = ["All", "Fiction", "Non-Fiction", "Mystery", "Fantasy", "Science Fiction", "Biography", "Romance", "Thriller", "History", "Self-Help"];

function Books() {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedGenre, setSelectedGenre] = useState("All");
  const [searchInput, setSearchInput] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");

  // Read URL params (from home page clicks)
  const [searchParams] = useSearchParams();

  useEffect(() => {
    // If user came from home page with a search or genre, apply it
    const urlSearch = searchParams.get("search");
    const urlGenre = searchParams.get("genre");

    if (urlSearch) {
      setSearchInput(urlSearch);
      searchByKeyword(urlSearch);
    } else if (urlGenre) {
      setSelectedGenre(urlGenre);
      filterByGenre(urlGenre);
    } else {
      loadAllBooks();
    }
  }, []);

  async function loadAllBooks() {
    try {
      setLoading(true);
      const res = await API.get("/api/books");
      setBooks(res.data);
    } catch (error) {
      console.error("Failed to load books", error);
    } finally {
      setLoading(false);
    }
  }

  async function searchByKeyword(keyword) {
    try {
      setLoading(true);
      const res = await API.get(`/api/books/search?keyword=${encodeURIComponent(keyword)}`);
      setBooks(res.data);
    } catch (error) {
      console.error("Search failed", error);
    } finally {
      setLoading(false);
    }
  }

  async function filterByGenre(genre) {
    try {
      setLoading(true);
      if (genre === "All") {
        await loadAllBooks();
        return;
      }
      const res = await API.get(`/api/books/genre/${encodeURIComponent(genre)}`);
      setBooks(res.data);
    } catch (error) {
      console.error("Genre filter failed", error);
    } finally {
      setLoading(false);
    }
  }

  async function filterByPrice() {
    if (!minPrice || !maxPrice) {
      alert("Please enter both min and max price");
      return;
    }
    try {
      setLoading(true);
      const res = await API.get(`/api/books/price-range?min=${minPrice}&max=${maxPrice}`);
      setBooks(res.data);
    } catch (error) {
      console.error("Price filter failed", error);
    } finally {
      setLoading(false);
    }
  }

  function handleSearch(e) {
    e.preventDefault();
    if (searchInput.trim()) {
      setSelectedGenre("All");
      searchByKeyword(searchInput.trim());
    }
  }

  function handleGenreChange(genre) {
    setSelectedGenre(genre);
    setSearchInput("");
    setMinPrice("");
    setMaxPrice("");
    filterByGenre(genre);
  }

  function handleReset() {
    setSelectedGenre("All");
    setSearchInput("");
    setMinPrice("");
    setMaxPrice("");
    loadAllBooks();
  }

  return (
    <div className="books-page">
      <div className="books-page-container">
        {/* Page Title */}
        <div className="books-header">
          <h1 className="books-page-title">📚 Browse Books</h1>
          <p className="books-page-subtitle">
            {loading ? "Loading..." : `${books.length} books found`}
          </p>
        </div>

        {/* Search Bar */}
        <form className="search-bar" onSubmit={handleSearch}>
          <input
            type="text"
            placeholder="Search by title, author, or keyword..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="search-input"
          />
          <button type="submit" className="search-btn">Search</button>
          <button type="button" className="reset-btn" onClick={handleReset}>Reset</button>
        </form>

        <div className="books-layout">
          {/* Sidebar Filters */}
          <aside className="books-sidebar">
            {/* Genre Filter */}
            <div className="filter-card">
              <h3 className="filter-title">Genre</h3>
              <ul className="genre-list">
                {GENRES.map((genre) => (
                  <li key={genre}>
                    <button
                      className={`genre-item ${selectedGenre === genre ? "active" : ""}`}
                      onClick={() => handleGenreChange(genre)}
                    >
                      {genre}
                    </button>
                  </li>
                ))}
              </ul>
            </div>

            {/* Price Filter */}
            <div className="filter-card">
              <h3 className="filter-title">Price Range (₹)</h3>
              <div className="price-inputs">
                <input
                  type="number"
                  placeholder="Min"
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                  className="price-input"
                />
                <span className="price-separator">—</span>
                <input
                  type="number"
                  placeholder="Max"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                  className="price-input"
                />
              </div>
              <button className="apply-price-btn" onClick={filterByPrice}>
                Apply Price Filter
              </button>
            </div>
          </aside>

          {/* Books Grid */}
          <main className="books-main">
            {loading ? (
              <div className="books-loading">
                <p>Loading books...</p>
              </div>
            ) : books.length === 0 ? (
              <div className="books-empty">
                <p>😕 No books found. Try a different search or filter.</p>
                <button className="reset-btn" onClick={handleReset}>
                  Reset Filters
                </button>
              </div>
            ) : (
              <div className="books-grid">
                {books.map((book) => (
                  <BookCard key={book.bookId} book={book} />
                ))}
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}

export default Books;
