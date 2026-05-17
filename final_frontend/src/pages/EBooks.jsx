import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getActiveEBooks, getMyEBookPurchases, getImageUrl } from "../utils/api";
import { useAuth } from "../context/AuthContext";
import "./EBooks.css";

const EBooks = () => {
  const [ebooks, setEbooks] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    fetchEBooks();
    if (user) {
      fetchPurchases();
    }
  }, [user]);

  const fetchEBooks = async () => {
    try {
      const res = await getActiveEBooks();
      setEbooks(res.data);
    } catch (error) {
      console.error("Failed to fetch eBooks", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchPurchases = async () => {
    try {
      const res = await getMyEBookPurchases();
      setPurchases(res.data.map((p) => p.ebook.id));
    } catch (error) {
      console.error("Failed to fetch purchases", error);
    }
  };

  const handleAction = (ebookId) => {
    if (!user) {
      navigate("/login");
      return;
    }
    // Navigate to reader page (handles both purchase and reading)
    navigate(`/ebooks/read/${ebookId}`);
  };

  if (loading) return <div className="loading">Loading eBooks...</div>;

  return (
    <div className="ebooks-container">
      <div className="ebooks-header">
        <h1>Digital Library</h1>
        <p>Purchase and read eBooks instantly</p>
      </div>

      {ebooks.length === 0 ? (
        <div className="no-ebooks">No eBooks available right now.</div>
      ) : (
        <div className="ebooks-grid">
          {ebooks.map((ebook) => {
            const isPurchased = purchases.includes(ebook.id);
            return (
              <div key={ebook.id} className="ebook-card">
                <div className="ebook-image">
                  <img
                    src={getImageUrl(ebook.coverImageUrl) || "https://via.placeholder.com/300x450?text=No+Cover"}
                    alt={ebook.title}
                  />
                </div>
                <div className="ebook-info">
                  <h3>{ebook.title}</h3>
                  <p className="author">by {ebook.author}</p>
                  <p className="price">₹{ebook.price}</p>
                  <button
                    className={`btn ${isPurchased ? "btn-read" : "btn-buy"}`}
                    onClick={() => handleAction(ebook.id)}
                  >
                    {isPurchased ? "Read Now" : "Buy Now"}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default EBooks;
