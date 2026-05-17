import React, { useState, useEffect } from "react";
import toast from "react-hot-toast";
import { getEBooksAdmin, uploadEBookAdmin, updateEBookAdmin, deleteEBookAdmin, getAllEBookPurchasesAdmin, getImageUrl, GATEWAY_BASE_URL } from "../utils/api";

export default function EBookAdmin() {
  const [ebooks, setEbooks] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editingEbook, setEditingEbook] = useState(null); // null = create mode, object = edit mode
  const [deleteConfirmId, setDeleteConfirmId] = useState(null);

  // Form State
  const [title, setTitle] = useState("");
  const [author, setAuthor] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [pdfFile, setPdfFile] = useState(null);
  const [coverImage, setCoverImage] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [ebookRes, purchaseRes] = await Promise.all([
        getEBooksAdmin(),
        getAllEBookPurchasesAdmin()
      ]);
      setEbooks(ebookRes.data);
      setPurchases(purchaseRes.data);
    } catch (err) {
      toast.error("Failed to load EBooks data");
    } finally {
      setLoading(false);
    }
  };

  const openCreateForm = () => {
    setEditingEbook(null);
    resetForm();
    setShowForm(true);
  };

  const openEditForm = (ebook) => {
    setEditingEbook(ebook);
    setTitle(ebook.title);
    setAuthor(ebook.author);
    setDescription(ebook.description || "");
    setPrice(ebook.price);
    setPdfFile(null);
    setCoverImage(null);
    setShowForm(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();

    const isEditing = !!editingEbook;

    if (!isEditing && (!title || !author || !price || !pdfFile)) {
      toast.error("Please fill required fields (Title, Author, Price, PDF)");
      return;
    }

    setSaving(true);
    const formData = new FormData();
    if (title) formData.append("title", title);
    if (author) formData.append("author", author);
    if (description) formData.append("description", description);
    if (price) formData.append("price", price);
    if (pdfFile) formData.append("pdfFile", pdfFile);
    if (coverImage) formData.append("coverImage", coverImage);

    try {
      toast.loading(isEditing ? "Saving changes..." : "Uploading EBook...", { id: "ebook-op" });
      if (isEditing) {
        await updateEBookAdmin(editingEbook.id, formData);
        toast.success("EBook updated successfully!", { id: "ebook-op" });
      } else {
        await uploadEBookAdmin(formData);
        toast.success("EBook uploaded successfully!", { id: "ebook-op" });
      }
      setShowForm(false);
      resetForm();
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || "Operation failed", { id: "ebook-op" });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      toast.loading("Deleting EBook...", { id: "delete-op" });
      await deleteEBookAdmin(id);
      toast.success("EBook deleted successfully!", { id: "delete-op" });
      setDeleteConfirmId(null);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to delete EBook", { id: "delete-op" });
    }
  };

  const resetForm = () => {
    setTitle("");
    setAuthor("");
    setDescription("");
    setPrice("");
    setPdfFile(null);
    setCoverImage(null);
  };

  const totalRevenue = purchases.reduce((sum, p) => sum + p.amountPaid, 0);

  return (
    <div className="admin-section fade-in">
      <div className="section-header">
        <h1 className="admin-heading">Manage EBooks</h1>
        <button className="btn-primary" onClick={openCreateForm}>+ Upload EBook</button>
      </div>

      <div className="stats-grid" style={{ marginBottom: '2rem' }}>
        <div className="stat-card">
          <div className="stat-icon">📱</div>
          <div className="stat-body">
            <div className="stat-value">{ebooks.length}</div>
            <div className="stat-label">Total EBooks</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">💰</div>
          <div className="stat-body">
            <div className="stat-value">₹{totalRevenue.toFixed(0)}</div>
            <div className="stat-label">Total Revenue</div>
            <div className="stat-sub">{purchases.length} Sales</div>
          </div>
        </div>
      </div>

      {loading ? <p className="admin-loading">Loading EBooks…</p> : (
        <div className="admin-table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Cover</th><th>ID</th><th>Title</th><th>Author</th><th>Price</th><th>Sales</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {ebooks.map(ebook => {
                const sales = purchases.filter(p => p.ebook.id === ebook.id).length;
                const coverSrc = getImageUrl(ebook.coverImageUrl);
                return (
                  <tr key={ebook.id}>
                    <td>
                      {coverSrc
                        ? <img src={coverSrc} alt={ebook.title} style={{ width: 40, height: 55, objectFit: 'cover', borderRadius: 4 }} />
                        : <div style={{ width: 40, height: 55, background: '#333', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, color: '#999' }}>No img</div>
                      }
                    </td>
                    <td className="td-id">#{ebook.id}</td>
                    <td className="td-title">{ebook.title}</td>
                    <td>{ebook.author}</td>
                    <td>₹{ebook.price.toFixed(2)}</td>
                    <td>{sales}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <a
                          href={ebook.pdfUrl.startsWith("http") ? ebook.pdfUrl : `${GATEWAY_BASE_URL}${ebook.pdfUrl}`}
                          target="_blank"
                          rel="noreferrer"
                          style={{ color: '#4dabf7', fontSize: '0.82rem', textDecoration: 'none', padding: '4px 8px', border: '1px solid #4dabf7', borderRadius: 4 }}
                        >
                          View PDF
                        </a>
                        <button
                          onClick={() => openEditForm(ebook)}
                          style={{ background: '#fab005', color: '#000', border: 'none', borderRadius: 4, padding: '4px 10px', cursor: 'pointer', fontSize: '0.82rem', fontWeight: 600 }}
                        >
                          Edit
                        </button>
                        {deleteConfirmId === ebook.id ? (
                          <>
                            <button
                              onClick={() => handleDelete(ebook.id)}
                              style={{ background: '#fa5252', color: '#fff', border: 'none', borderRadius: 4, padding: '4px 10px', cursor: 'pointer', fontSize: '0.82rem', fontWeight: 600 }}
                            >
                              Confirm
                            </button>
                            <button
                              onClick={() => setDeleteConfirmId(null)}
                              style={{ background: '#555', color: '#fff', border: 'none', borderRadius: 4, padding: '4px 10px', cursor: 'pointer', fontSize: '0.82rem' }}
                            >
                              Cancel
                            </button>
                          </>
                        ) : (
                          <button
                            onClick={() => setDeleteConfirmId(ebook.id)}
                            style={{ background: '#fa5252', color: '#fff', border: 'none', borderRadius: 4, padding: '4px 10px', cursor: 'pointer', fontSize: '0.82rem', fontWeight: 600 }}
                          >
                            Delete
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {ebooks.length === 0 && <p className="admin-empty">No EBooks found.</p>}
        </div>
      )}

      {showForm && (
        <div className="modal-overlay" onClick={() => setShowForm(false)}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingEbook ? `Edit: ${editingEbook.title}` : "Upload New EBook"}</h3>
              <button className="modal-close" onClick={() => setShowForm(false)}>✕</button>
            </div>
            <form onSubmit={handleSave} className="book-form">
              <div className="form-grid">
                <div className="form-group">
                  <label>Title {!editingEbook && '*'}</label>
                  <input value={title} onChange={e => setTitle(e.target.value)} required={!editingEbook} />
                </div>
                <div className="form-group">
                  <label>Author {!editingEbook && '*'}</label>
                  <input value={author} onChange={e => setAuthor(e.target.value)} required={!editingEbook} />
                </div>
                <div className="form-group">
                  <label>Price (₹) {!editingEbook && '*'}</label>
                  <input type="number" step="0.01" min="0" value={price} onChange={e => setPrice(e.target.value)} required={!editingEbook} />
                </div>
              </div>
              <div className="form-group full-width">
                <label>Description</label>
                <textarea rows={3} value={description} onChange={e => setDescription(e.target.value)} />
              </div>
              <div className="form-grid">
                <div className="form-group">
                  <label>PDF File {!editingEbook ? '*' : '(leave blank to keep existing)'}</label>
                  <input type="file" accept="application/pdf" onChange={e => setPdfFile(e.target.files[0])} required={!editingEbook} />
                  {editingEbook && <small style={{ color: '#888', fontSize: '0.75rem' }}>Current: {editingEbook.pdfUrl?.split('/').pop()}</small>}
                </div>
                <div className="form-group">
                  <label>Cover Image (leave blank to keep existing)</label>
                  <input type="file" accept="image/*" onChange={e => setCoverImage(e.target.files[0])} />
                  {editingEbook && editingEbook.coverImageUrl && (
                    <div style={{ marginTop: 6 }}>
                      <img
                        src={getImageUrl(editingEbook.coverImageUrl)}
                        alt="Current cover"
                        style={{ width: 48, height: 64, objectFit: 'cover', borderRadius: 4, border: '2px solid #555' }}
                      />
                      <small style={{ display: 'block', color: '#888', fontSize: '0.75rem' }}>Current cover</small>
                    </div>
                  )}
                </div>
              </div>
              <div className="form-actions" style={{ marginTop: '1rem' }}>
                <button type="button" className="btn-ghost" onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? "Saving..." : (editingEbook ? "Save Changes" : "Upload EBook")}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
