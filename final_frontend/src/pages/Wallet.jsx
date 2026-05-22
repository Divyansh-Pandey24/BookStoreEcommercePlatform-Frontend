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
        API.get("/wallet"),
        API.get("/wallet/transactions"),
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
      const res = await API.post("/wallet/razorpay/create-order", { amount: parsed });

      const { razorpayOrderId, amount: orderAmount, currency, keyId } = res.data;

      await loadRazorpayScript();

      const options = {
        key: keyId,

        // Convert amount from rupees to paise for Razorpay checkout
        amount: Math.round(orderAmount * 100),

        currency: currency || "INR",
        name: "BookNest",
        description: "Add Money to Wallet",
        order_id: razorpayOrderId,

        handler: async function (response) {
          try {
            // Map callback keys from snake_case to camelCase to match backend DTO schema
            await API.post("/wallet/razorpay/verify", {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
              amount: orderAmount,
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

        // Configure payment methods enabling UPI collect flows for sandboxed testing
        method: {
          card: true,
          netbanking: true,
          wallet: false,
          upi: true,
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
                    flows: ["collect"],
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
            // Reset adding state on dismiss
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
    // Reset state on modal dismiss instead of finally block because checkout is async
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

          {Array.isArray(transactions) && transactions.length === 0 ? (
            <p className="no-transactions">No transactions yet.</p>
          ) : (
            <div className="transactions-list">
              {Array.isArray(transactions) && transactions.map((txn) => (
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