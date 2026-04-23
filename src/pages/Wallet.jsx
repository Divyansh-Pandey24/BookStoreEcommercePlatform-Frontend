import { useState, useEffect } from "react";
import API from "../utils/api";
import { useAuth } from "../context/AuthContext";
import toast from "react-hot-toast";
import "./Wallet.css";

function Wallet() {
  const { user } = useAuth();
  const [balance, setBalance] = useState(null);
  const [transactions, setTxns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState("");
  const [addingMoney, setAddingMoney] = useState(false);

  useEffect(() => { loadWallet(); }, []);

  async function loadWallet() {
    try {
      setLoading(true);

      // FIX: Use Promise.allSettled so one failure doesn't block the other
      const [walletRes, txnRes] = await Promise.allSettled([
        API.get("/api/wallet"),
        API.get("/api/wallet/transactions"),
      ]);

      if (walletRes.status === "fulfilled") {
        setBalance(walletRes.value.data.currentBalance ?? 0);
      } else {
        toast.error("Failed to load wallet balance");
        setBalance(0);
      }

      if (txnRes.status === "fulfilled") {
        setTxns(txnRes.value.data || []);
      } else {
        // Transactions failing silently is acceptable
        setTxns([]);
      }
    } catch {
      toast.error("Failed to load wallet");
    } finally {
      setLoading(false);
    }
  }

  async function handleAddMoney() {
    const parsed = parseFloat(amount);
    if (!parsed || parsed <= 0) {
      toast.error("Please enter a valid amount");
      return;
    }

    try {
      setAddingMoney(true);

      // Step 1: Create Razorpay order on backend
      const res = await API.post("/api/wallet/razorpay/create-order", { amount: parsed });

      const { razorpayOrderId, amount: orderAmount, currency, keyId } = res.data;

      await loadRazorpayScript();

      const options = {
        key: keyId,

        // FIX ✅: Backend returns amount in RUPEES (e.g. 500.0)
        // Razorpay checkout expects PAISE (50000)
        // Without this, checkout amount won't match the order → payment rejected
        amount: Math.round(orderAmount * 100),

        currency: currency || "INR",
        name: "BookNest",
        description: "Add Money to Wallet",
        order_id: razorpayOrderId,

        handler: async function (response) {
          try {
            // CRITICAL FIX: Backend DTO fields are camelCase (razorpayOrderId, etc.)
            // Razorpay callback gives snake_case (razorpay_order_id, etc.)
            // Jackson does NOT auto-convert snake_case → camelCase, so they
            // arrived as null → signature check always failed → "verification failed" toast.
            // Solution: map to camelCase keys that match PaymentVerifyRequest.java
            await API.post("/api/wallet/razorpay/verify", {
              razorpayOrderId: response.razorpay_order_id,       // ← camelCase!
              razorpayPaymentId: response.razorpay_payment_id,   // ← camelCase!
              razorpaySignature: response.razorpay_signature,     // ← camelCase!
              amount: orderAmount,   // ← in rupees (backend uses this directly)
            });

            toast.success("Money added to wallet successfully! 🎉");
            setAmount("");
            loadWallet();
          } catch {
            toast.error("Payment verification failed. Please contact support.");
          }
        },

        prefill: {
          name: user?.fullName || "BookNest User",
          email: user?.email || "",
          contact: user?.mobile || "",
        },

        // FIX ✅: Simplified method config for reliable UPI ID support
        // The complex config.display.blocks can break in test mode
        // Use method flags + one config block for UPI ID text input
        method: {
          card: true,
          netbanking: true,
          wallet: false,
          upi: true,       // enables UPI section
          emi: false,
        },

        config: {
          display: {
            blocks: {
              upiBlock: {
                name: "Pay via UPI ID",
                instruments: [
                  {
                    method: "upi",
                    flows: ["collect"],   // "collect" = UPI ID text input (no apps)
                  },
                ],
              },
              cardBlock: {
                name: "Debit / Credit Cards",
                instruments: [{ method: "card" }],
              },
              netbankingBlock: {
                name: "Net Banking",
                instruments: [{ method: "netbanking" }],
              },
            },
            sequence: ["block.upiBlock", "block.cardBlock", "block.netbankingBlock"],
            preferences: {
              show_default_blocks: false,
            },
          },
        },

        theme: { color: "#c9a84c" },
        modal: {
          ondismiss: () => {
            // User closed the popup — reset state cleanly
            setAddingMoney(false);
          },
        },
      };

      const rzp = new window.Razorpay(options);

      rzp.on("payment.failed", (resp) => {
        toast.error(`Payment failed: ${resp.error.description || "Please try again"}`);
        setAddingMoney(false);
      });

      rzp.open();
    } catch (e) {
      toast.error(e.response?.data?.message || "Failed to start payment. Please try again.");
      setAddingMoney(false);
    }
    // NOTE: don't put setAddingMoney(false) in finally here
    // because rzp.open() is async — modal.ondismiss handles it
  }

  function loadRazorpayScript() {
    return new Promise((resolve, reject) => {
      if (window.Razorpay) return resolve();
      const script = document.createElement("script");
      script.src = "https://checkout.razorpay.com/v1/checkout.js";
      script.onload = resolve;
      script.onerror = () => reject(new Error("Failed to load Razorpay SDK"));
      document.body.appendChild(script);
    });
  }

  if (loading) return <div className="wallet-loading">Loading wallet...</div>;

  return (
    <div className="wallet-page">
      <div className="wallet-container">
        <h1 className="wallet-title">💳 My Wallet</h1>

        <div className="balance-card">
          <p className="balance-label">CURRENT BALANCE</p>
          <p className="balance-amount">₹{balance?.toFixed(2) ?? "0.00"}</p>
          <p className="balance-sub">BookNest Wallet</p>
        </div>

        <div className="add-money-card">
          <h2 className="add-money-title">Add Money</h2>

          <div className="quick-amounts">
            {[100, 200, 500, 1000, 2000].map((val) => (
              <button key={val} className="quick-amt-btn" onClick={() => setAmount(val.toString())}>
                ₹{val}
              </button>
            ))}
          </div>

          <div className="add-money-input-row">
            <input
              type="number"
              className="add-money-input"
              placeholder="Enter amount (₹)"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              min="1"
            />
          </div>

          <button
            className="add-money-btn"
            onClick={handleAddMoney}
            disabled={addingMoney || !amount}
          >
            {addingMoney ? "Opening Payment..." : "Proceed to Payment"}
          </button>

          <p className="razorpay-note">
            🔒 Secure payment via Razorpay • UPI ID, Cards, Netbanking supported
          </p>
        </div>

        <div className="transactions-card">
          <h2 className="transactions-title">Transaction History</h2>

          {transactions.length === 0 ? (
            <p className="no-transactions">No transactions yet.</p>
          ) : (
            <div className="transactions-list">
              {transactions.map((txn) => (
                <div key={txn.id} className="transaction-item">
                  <div className="txn-info">
                    <span className="txn-type">{txn.type}</span>
                    <span className="txn-date">
                      {new Date(txn.createdAt).toLocaleDateString("en-IN")}
                    </span>
                  </div>

                  <span className={`txn-amount ${txn.type === "CREDIT" ? "credit" : "debit"}`}>
                    {txn.type === "CREDIT" ? "+" : "−"}₹{txn.amount?.toFixed(2)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Wallet;