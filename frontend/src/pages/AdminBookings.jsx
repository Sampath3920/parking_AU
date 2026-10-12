import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../services/api";

const PAGE_SIZE = 10;
const FILTERS = [
    { value: "PENDING", label: "Pending" },
    { value: "BOOKED", label: "Approved" },
    { value: "REJECTED", label: "Rejected" },
    { value: "CANCELLED", label: "Cancelled" },
    { value: "COMPLETED", label: "Completed" },
    { value: "EXPIRED", label: "Expired" },
    { value: "", label: "All" }
];

function statusLabel(status) {
    return status === "BOOKED" ? "APPROVED" : status;
}

function badgeClass(status) {
    if (status === "BOOKED" || status === "COMPLETED") return "bg-success";
    if (status === "REJECTED" || status === "CANCELLED") return "bg-danger";
    if (status === "EXPIRED") return "bg-secondary";
    return "bg-warning text-dark";
}

function formatWindow(item) {
    if (!item.startTime || !item.endTime) return "-";
    return `${new Date(item.startTime).toLocaleString()} - ${new Date(item.endTime).toLocaleTimeString()}`;
}

function AdminBookings() {
    const [bookings, setBookings] = useState([]);
    const [payments, setPayments] = useState([]);
    // The dashboard cards open this page as /admin/bookings?status=BOOKED (or PENDING, COMPLETED ...).
    const [params] = useSearchParams();
    const [status, setStatus] = useState(params.get("status") ?? "PENDING");
    const [search, setSearch] = useState("");
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    const [isError, setIsError] = useState(false);

    // { kind: "booking" | "payment", item }
    const [rejecting, setRejecting] = useState(null);
    const [reason, setReason] = useState("");

    const show = (text, error = false) => {
        setMessage(text);
        setIsError(error);
    };

    // The loading flag is switched on by the handler that changes the filter (not inside this
    // function), so that the effect below does not set state synchronously.
    const load = async () => {
        try {
            const [bookingResponse, paymentResponse] = await Promise.all([
                api.get("/bookings", { params: { status } }),
                api.get("/payments")
            ]);
            const sorted = [...bookingResponse.data].sort((a, b) => b.id - a.id);
            setBookings(sorted);
            setPayments(paymentResponse.data);
            setPage(0);
        } catch (error) {
            show(error.response?.data?.message || "Failed to load requests", true);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        const run = async () => {
            await load();
        };
        void run();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [status]);

    const paymentFor = (bookingId) =>
        payments.find((p) => p.booking?.id === bookingId);

    const approve = async (booking) => {
        try {
            await api.put(`/bookings/${booking.id}/approve`);
            show(`Request #${booking.id} approved. The user was notified and the QR code is ready.`);
            await load();
        } catch (error) {
            show(error.response?.data?.message || "Could not approve the request", true);
        }
    };

    const verifyPayment = async (payment) => {
        try {
            await api.put(`/payments/${payment.id}/verify`, { status: "SUCCESS", reason: "" });
            show(`Payment #${payment.id} verified`);
            await load();
        } catch (error) {
            show(error.response?.data?.message || "Could not verify the payment", true);
        }
    };

    const confirmReject = async () => {
        if (!reason.trim()) {
            show("Please write the reason for rejecting", true);
            return;
        }
        try {
            if (rejecting.kind === "booking") {
                await api.put(`/bookings/${rejecting.item.id}/reject`, { reason: reason.trim() });
                show(`Request #${rejecting.item.id} rejected`);
            } else {
                await api.put(`/payments/${rejecting.item.id}/verify`, {
                    status: "FAILED",
                    reason: reason.trim()
                });
                show(`Payment #${rejecting.item.id} rejected`);
            }
            setRejecting(null);
            setReason("");
            await load();
        } catch (error) {
            show(error.response?.data?.message || "Could not save the decision", true);
        }
    };

    const text = search.trim().toLowerCase();
    const filtered = bookings.filter(
        (b) =>
            !text ||
            String(b.id) === text ||
            (b.user?.name || "").toLowerCase().includes(text) ||
            (b.user?.email || "").toLowerCase().includes(text) ||
            (b.vehicle?.vehicleNumber || "").toLowerCase().includes(text)
    );
    const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    const rows = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

    return (
        <div className="container mt-4">
            <h2 className="mb-3">Parking Requests</h2>

            {message && (
                <div className={`alert ${isError ? "alert-danger" : "alert-success"}`} role="alert">
                    {message}
                </div>
            )}

            <div className="row g-2 mb-3">
                <div className="col-md-4">
                    <label htmlFor="bookingStatus" className="form-label">Show</label>
                    <select
                        id="bookingStatus"
                        className="form-select"
                        value={status}
                        onChange={(e) => {
                            setLoading(true);
                            setStatus(e.target.value);
                        }}
                    >
                        {FILTERS.map((f) => (
                            <option key={f.label} value={f.value}>{f.label}</option>
                        ))}
                    </select>
                </div>
                <div className="col-md-8">
                    <label htmlFor="bookingSearch" className="form-label">
                        Search (request number, user, email or vehicle)
                    </label>
                    <input
                        id="bookingSearch"
                        className="form-control"
                        value={search}
                        onChange={(e) => {
                            setSearch(e.target.value);
                            setPage(0);
                        }}
                    />
                </div>
            </div>

            <p className="text-muted small">
                To approve a request, first verify its payment. Slot status shows
                whether the slot is still open (Reserved = this or another request holds it).
            </p>

            {rejecting && (
                <div className="card border-danger mb-3">
                    <div className="card-body">
                        <h5>
                            Reject {rejecting.kind === "booking" ? "request" : "payment"} #{rejecting.item.id}
                        </h5>
                        <label htmlFor="rejectReason" className="form-label">
                            Reason (required, the user will see it)
                        </label>
                        <textarea
                            id="rejectReason"
                            className="form-control mb-2"
                            rows="2"
                            maxLength="500"
                            value={reason}
                            onChange={(e) => setReason(e.target.value)}
                        />
                        <button type="button" className="btn btn-danger me-2" onClick={confirmReject}>
                            Confirm reject
                        </button>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => {
                                setRejecting(null);
                                setReason("");
                            }}
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            )}

            {loading ? (
                <p>Loading...</p>
            ) : rows.length === 0 ? (
                <p className="text-muted">No requests found for this filter.</p>
            ) : (
                <div className="table-responsive">
                    <table className="table table-bordered table-hover align-middle">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>User</th>
                                <th>Vehicle</th>
                                <th>Location / slot</th>
                                <th>Time</th>
                                <th>Payment</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map((b) => {
                                const payment = paymentFor(b.id);
                                return (
                                    <tr key={b.id}>
                                        <td>{b.id}</td>
                                        <td>
                                            {b.user?.name}<br />
                                            <small>{b.user?.email}</small>
                                        </td>
                                        <td>
                                            {b.vehicle?.vehicleNumber}<br />
                                            <small>{b.vehicle?.vehicleType}</small>
                                        </td>
                                        <td>
                                            {b.parkingSlot?.parkingLocation?.name}<br />
                                            Slot {b.parkingSlot?.slotNumber}{" "}
                                            <small>
                                                ({b.parkingSlot?.status === "BOOKED"
                                                    ? "Reserved"
                                                    : b.parkingSlot?.status})
                                            </small>
                                        </td>
                                        <td>
                                            {formatWindow(b)}<br />
                                            <small>
                                                Requested {b.bookingTime ? new Date(b.bookingTime).toLocaleString() : "-"}
                                            </small>
                                        </td>
                                        <td>
                                            {payment ? (
                                                <>
                                                    {payment.status}<br />
                                                    <small>
                                                        {payment.paymentMethod} - ref {payment.transactionReference}
                                                    </small>
                                                    {payment.status === "PENDING_VERIFICATION" && (
                                                        <div className="mt-1">
                                                            <button
                                                                type="button"
                                                                className="btn btn-sm btn-outline-success me-1"
                                                                onClick={() => verifyPayment(payment)}
                                                            >
                                                                Verify payment
                                                            </button>
                                                            <button
                                                                type="button"
                                                                className="btn btn-sm btn-outline-danger"
                                                                onClick={() => {
                                                                    setRejecting({ kind: "payment", item: payment });
                                                                    setReason("");
                                                                }}
                                                            >
                                                                Reject payment
                                                            </button>
                                                        </div>
                                                    )}
                                                </>
                                            ) : (
                                                <span className="text-muted">Not submitted</span>
                                            )}
                                        </td>
                                        <td>
                                            <span className={`badge ${badgeClass(b.status)}`}>
                                                {statusLabel(b.status)}
                                            </span>
                                            {b.decisionReason && (
                                                <div><small>{b.decisionReason}</small></div>
                                            )}
                                        </td>
                                        <td>
                                            {b.status === "PENDING" && (
                                                <>
                                                    <button
                                                        type="button"
                                                        className="btn btn-sm btn-success me-1 mb-1"
                                                        onClick={() => approve(b)}
                                                    >
                                                        Approve
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="btn btn-sm btn-danger mb-1"
                                                        onClick={() => {
                                                            setRejecting({ kind: "booking", item: b });
                                                            setReason("");
                                                        }}
                                                    >
                                                        Reject
                                                    </button>
                                                </>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            <div className="d-flex justify-content-between align-items-center mb-4">
                <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                >
                    Previous
                </button>
                <span>Page {page + 1} of {pageCount} ({filtered.length} requests)</span>
                <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    disabled={page + 1 >= pageCount}
                    onClick={() => setPage(page + 1)}
                >
                    Next
                </button>
            </div>
        </div>
    );
}

export default AdminBookings;
