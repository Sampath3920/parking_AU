import { useEffect, useState } from "react";
import api from "../services/api";
import { getUser } from "../services/auth";

function Payments() {
    const user = getUser();
    const userId = user?.id;
    const isAdmin = user?.role === "ADMIN";

    const [bookings, setBookings] = useState([]);
    const [payments, setPayments] = useState([]);
    const [selectedBooking, setSelectedBooking] = useState("");
    const [amount, setAmount] = useState(50);
    const [paymentMethod, setPaymentMethod] = useState("UPI");
    const [transactionReference, setTransactionReference] = useState("");
    const [acknowledged, setAcknowledged] = useState(false);
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("info");
    const [submitting, setSubmitting] = useState(false);
    const [receipt, setReceipt] = useState(null);

    const paymentListUrl = isAdmin ? "/payments" : `/payments/user/${userId}`;

    useEffect(() => {
        let cancelled = false;
        const requests = [
            api.get("/payments/manual-amount"),
            api.get(paymentListUrl)
        ];
        if (!isAdmin) {
            requests.push(api.get(`/bookings/user/${userId}`));
        }

        Promise.all(requests)
            .then(([amountResponse, paymentResponse, bookingResponse]) => {
                if (cancelled) {
                    return;
                }
                setAmount(amountResponse.data.amount);
                setPayments(paymentResponse.data);
                if (bookingResponse) {
                    setBookings(bookingResponse.data);
                }
            })
            .catch((error) => {
                if (!cancelled) {
                    setMessage(
                        error.response?.data?.message || "Failed to load payment information"
                    );
                    setMessageType("danger");
                }
            });

        return () => {
            cancelled = true;
        };
    }, [isAdmin, paymentListUrl, userId]);

    const refreshPayments = async () => {
        const response = await api.get(paymentListUrl);
        setPayments(response.data);
    };

    const submitPaymentDetails = async (event) => {
        event.preventDefault();
        if (!selectedBooking) {
            setMessage("Please select a booking");
            setMessageType("warning");
            return;
        }
        if (!transactionReference.trim()) {
            setMessage("Enter the transaction/reference ID from your payment");
            setMessageType("warning");
            return;
        }
        if (!acknowledged) {
            setMessage("Confirm that the payment reference you submitted is correct");
            setMessageType("warning");
            return;
        }

        setSubmitting(true);
        try {
            const response = await api.post(`/payments/create/${selectedBooking}`, {
                paymentMethod,
                transactionReference: transactionReference.trim(),
                acknowledged
            });
            setMessage(
                `Payment details submitted for verification (payment #${response.data.id}). This is not a payment confirmation.`
            );
            setMessageType("success");
            setSelectedBooking("");
            setTransactionReference("");
            setAcknowledged(false);
            await refreshPayments();
        } catch (error) {
            setMessage(
                error.response?.data?.message || "Could not submit payment details"
            );
            setMessageType("danger");
        } finally {
            setSubmitting(false);
        }
    };

    const reviewPayment = async (payment, status) => {
        const reason = status === "FAILED"
            ? window.prompt("Enter the reason for rejecting this payment:")?.trim()
            : "";
        if (status === "FAILED" && !reason) {
            setMessage("A rejection reason is required");
            setMessageType("warning");
            return;
        }

        try {
            await api.put(`/payments/${payment.id}/verify`, { status, reason });
            setMessage(
                status === "SUCCESS" ? "Payment verified" : "Payment rejected"
            );
            setMessageType("success");
            await refreshPayments();
        } catch (error) {
            setMessage(
                error.response?.data?.message || "Could not review payment"
            );
            setMessageType("danger");
        }
    };

    const submittedBookingIds = new Set(
        payments.map((payment) => payment.booking?.id)
    );
    const eligibleBookings = bookings.filter(
        (booking) =>
            !submittedBookingIds.has(booking.id) &&
            !["CANCELLED", "COMPLETED", "EXPIRED", "REJECTED"].includes(booking.status)
    );

    return (
        <div className="container mt-4 mb-5">
            <h2 className="text-center mb-4">
                {isAdmin ? "Payment Verification" : "My Payments"}
            </h2>

            <div className="alert alert-warning" role="note">
                Manual/demo payment verification only. This system does not process
                money or connect to a bank. Submitting a reference does not mean a
                payment was received or approved.
            </div>

            {message && (
                <div className={`alert alert-${messageType}`} role="status">
                    {message}
                </div>
            )}

            {!isAdmin && (
                <form className="card shadow mb-4" onSubmit={submitPaymentDetails}>
                    <div className="card-body">
                        <h5 className="mb-3">Submit Payment Details</h5>
                        <p className="mb-3">
                            Manual demo amount: <strong>₹{amount}</strong>. Pay using
                            your institution&apos;s approved channel before entering
                            the transaction reference.
                        </p>

                        <label className="form-label" htmlFor="paymentBooking">
                            Booking
                        </label>
                        <select
                            id="paymentBooking"
                            className="form-select mb-3"
                            value={selectedBooking}
                            onChange={(event) => setSelectedBooking(event.target.value)}
                            required
                        >
                            <option value="">Select booking</option>
                            {eligibleBookings.map((booking) => (
                                <option key={booking.id} value={booking.id}>
                                    Booking #{booking.id} —{" "}
                                    {booking.vehicle?.vehicleNumber || "vehicle"} —{" "}
                                    {booking.parkingSlot?.slotNumber || "slot"}
                                </option>
                            ))}
                        </select>
                        {eligibleBookings.length === 0 && (
                            <p className="text-muted">
                                No bookings are currently eligible for payment submission.
                            </p>
                        )}

                        <label className="form-label" htmlFor="paymentMethod">
                            Payment method
                        </label>
                        <select
                            id="paymentMethod"
                            className="form-select mb-3"
                            value={paymentMethod}
                            onChange={(event) => setPaymentMethod(event.target.value)}
                        >
                            <option value="UPI">UPI</option>
                            <option value="BANK_TRANSFER">Bank transfer</option>
                            <option value="CASH">Cash at parking counter</option>
                        </select>

                        <label className="form-label" htmlFor="transactionReference">
                            Transaction/reference ID
                        </label>
                        <input
                            id="transactionReference"
                            className="form-control mb-3"
                            maxLength={100}
                            value={transactionReference}
                            onChange={(event) =>
                                setTransactionReference(event.target.value)
                            }
                            required
                        />

                        <div className="form-check mb-3">
                            <input
                                id="paymentAcknowledgement"
                                className="form-check-input"
                                type="checkbox"
                                checked={acknowledged}
                                onChange={(event) =>
                                    setAcknowledged(event.target.checked)
                                }
                            />
                            <label
                                className="form-check-label"
                                htmlFor="paymentAcknowledgement"
                            >
                                I confirm the payment reference details I submitted are correct.
                            </label>
                        </div>
                        <button
                            className="btn btn-primary"
                            type="submit"
                            disabled={submitting || eligibleBookings.length === 0}
                        >
                            {submitting ? "Submitting..." : "Submit for verification"}
                        </button>
                    </div>
                </form>
            )}

            {receipt && (
                <section className="card shadow-sm mb-4 print-area" aria-live="polite">
                    <div className="card-body">
                        <h4>Payment receipt #{receipt.id}</h4>
                        <p className="text-muted small">
                            Smart College Parking. Manual / demo payment, verified by the administrator.
                        </p>
                        <table className="table table-sm mb-3">
                            <tbody>
                                <tr><th scope="row">Name</th><td>{receipt.booking?.user?.name ?? "-"}</td></tr>
                                <tr><th scope="row">Booking</th><td>#{receipt.booking?.id ?? "-"}</td></tr>
                                <tr><th scope="row">Vehicle</th><td>{receipt.booking?.vehicle?.vehicleNumber ?? "-"}</td></tr>
                                <tr><th scope="row">Slot</th><td>{receipt.booking?.parkingSlot?.slotNumber ?? "-"}</td></tr>
                                <tr><th scope="row">Amount</th><td>₹{receipt.amount}</td></tr>
                                <tr><th scope="row">Payment method</th><td>{receipt.paymentMethod}</td></tr>
                                <tr><th scope="row">Reference</th><td>{receipt.transactionReference}</td></tr>
                                <tr>
                                    <th scope="row">Date</th>
                                    <td>{receipt.paymentTime ? new Date(receipt.paymentTime).toLocaleString() : "-"}</td>
                                </tr>
                                <tr><th scope="row">Status</th><td>{receipt.status}</td></tr>
                            </tbody>
                        </table>
                        <div className="d-print-none">
                            <button type="button" className="btn btn-primary me-2" onClick={() => window.print()}>
                                Print / save as PDF
                            </button>
                            <button type="button" className="btn btn-outline-secondary" onClick={() => setReceipt(null)}>
                                Close
                            </button>
                        </div>
                    </div>
                </section>
            )}

            <section>
                <h4>{isAdmin ? "All payment submissions" : "Payment history"}</h4>
                {payments.length === 0 ? (
                    <div className="alert alert-info">No payment records found.</div>
                ) : (
                    <div className="table-responsive">
                        <table className="table table-striped table-bordered align-middle">
                            <thead>
                                <tr>
                                    {isAdmin && <th>User</th>}
                                    <th>Payment ID</th>
                                    <th>Booking</th>
                                    <th>Amount</th>
                                    <th>Method</th>
                                    <th>Reference</th>
                                    <th>Submitted</th>
                                    <th>Status</th>
                                    <th>Review note</th>
                                    {isAdmin && <th>Actions</th>}
                                    {!isAdmin && <th>Receipt</th>}
                                </tr>
                            </thead>
                            <tbody>
                                {payments.map((payment) => (
                                    <tr key={payment.id}>
                                        {isAdmin && (
                                            <td>{payment.booking?.user?.name || "Unknown"}</td>
                                        )}
                                        <td>{payment.id}</td>
                                        <td>{payment.booking?.id || "—"}</td>
                                        <td>₹{payment.amount}</td>
                                        <td>{payment.paymentMethod}</td>
                                        <td>{payment.transactionReference}</td>
                                        <td>
                                            {payment.paymentTime
                                                ? new Date(payment.paymentTime).toLocaleString()
                                                : "—"}
                                        </td>
                                        <td>{payment.status}</td>
                                        <td>{payment.reviewReason || "—"}</td>
                                        {!isAdmin && (
                                            <td>
                                                {payment.status === "SUCCESS" ? (
                                                    <button
                                                        type="button"
                                                        className="btn btn-sm btn-outline-primary"
                                                        onClick={() => setReceipt(payment)}
                                                    >
                                                        View receipt
                                                    </button>
                                                ) : (
                                                    "—"
                                                )}
                                            </td>
                                        )}
                                        {isAdmin && (
                                            <td>
                                                {payment.status === "PENDING_VERIFICATION" && (
                                                    <div className="d-flex gap-2">
                                                        <button
                                                            className="btn btn-sm btn-success"
                                                            type="button"
                                                            onClick={() =>
                                                                reviewPayment(payment, "SUCCESS")
                                                            }
                                                        >
                                                            Verify
                                                        </button>
                                                        <button
                                                            className="btn btn-sm btn-outline-danger"
                                                            type="button"
                                                            onClick={() =>
                                                                reviewPayment(payment, "FAILED")
                                                            }
                                                        >
                                                            Reject
                                                        </button>
                                                    </div>
                                                )}
                                            </td>
                                        )}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </div>
    );
}

export default Payments;
