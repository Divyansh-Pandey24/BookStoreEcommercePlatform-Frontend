import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { readEBook, purchaseEBook, getMyEBookPurchases, getActiveEBooks, getEBookById, GATEWAY_BASE_URL } from "../utils/api";
import { useAuth } from "../context/AuthContext";
import "./EBookReader.css";

const EBookReader = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [ebook, setEbook] = useState(null);
  const [pdfUrl, setPdfUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [purchasing, setPurchasing] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!user) {
      navigate("/login");
      return;
    }
    checkAccessAndFetch();
  }, [id, user]);

  const checkAccessAndFetch = async () => {
    setLoading(true);
    try {
      // 1. Fetch ebook details first (always works if book exists)
      const ebookRes = await getEBookById(id);
      setEbook(ebookRes.data);

      // 2. Try to read (if we have access, we get the PDF URL)
      const readRes = await readEBook(id);
      setPdfUrl(readRes.data.pdfUrl);
    } catch (err) {
      // If 403 Forbidden, we haven't purchased it yet. 
      // The ebook details were already fetched if it reached here,
      // but if the FIRST call (getEBookById) failed, it would catch here.
      if (err.response?.status === 403 && ebook) {
        // Just stay on purchase prompt (ebook is already set)
      } else if (err.response?.status === 403) {
         // This means we might have failed at getEBookById due to 403 or readEBook due to 403
         // Try to ensure ebook details are loaded even if read fails
         try {
            const ebookRes = await getEBookById(id);
            setEbook(ebookRes.data);
         } catch (e) {
            setError("EBook not found or access denied.");
         }
      } else {
        setError(err.response?.data?.message || "Failed to load eBook.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handlePurchase = async () => {
    setPurchasing(true);
    setError(null);
    try {
      await purchaseEBook(id);
      // Purchase successful, now fetch the PDF
      const readRes = await readEBook(id);
      setPdfUrl(readRes.data.pdfUrl);
    } catch (err) {
      setError(err.response?.data?.message || "Purchase failed. Please check your wallet balance.");
    } finally {
      setPurchasing(false);
    }
  };

  if (loading) return <div className="loading">Loading...</div>;

  // If user has the URL, show the reader
  if (pdfUrl) {
    return (
      <div className="reader-container">
        <div className="reader-header">
          <button onClick={() => navigate("/ebooks")} className="btn-back">← Back to Library</button>
          <h2>Reading: {ebook?.title || "EBook"}</h2>
        </div>
        <div className="pdf-viewer">
          {/* Use native viewer for local files (Google Docs can't see localhost), and Google Docs for external files */}
          <iframe 
            src={pdfUrl.startsWith("http") 
              ? `https://docs.google.com/viewer?url=${encodeURIComponent(pdfUrl)}&embedded=true`
              : `${GATEWAY_BASE_URL}${pdfUrl}#toolbar=0`
            } 
            title="PDF Reader"
            width="100%" 
            height="100%" 
            style={{ border: "none" }}
          />
        </div>
      </div>
    );
  }

  // If user hasn't purchased it, show purchase prompt
  if (ebook) {
    return (
      <div className="purchase-container">
        <div className="purchase-card">
          <h2>Purchase Required</h2>
          <p>You need to purchase <strong>{ebook.title}</strong> to read it.</p>
          <div className="price-tag">₹{ebook.price}</div>
          {error && <div className="error-alert">{error}</div>}
          <div className="purchase-actions">
            <button 
              className="btn btn-buy" 
              onClick={handlePurchase}
              disabled={purchasing}
            >
              {purchasing ? "Processing..." : `Pay ₹${ebook.price} from Wallet`}
            </button>
            <button className="btn btn-cancel" onClick={() => navigate("/ebooks")}>Cancel</button>
          </div>
          <p className="wallet-hint">
            <small>If you don't have enough balance, <a href="/wallet">top-up your wallet here</a>.</small>
          </p>
        </div>
      </div>
    );
  }

  return <div className="error-container">{error || "Something went wrong."}</div>;
};

export default EBookReader;
