import { Fragment, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../services/api";
import useApi from "../hooks/useApi";
import Pagination from "../components/Pagination";
import { downloadFile } from "../services/download";

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
    ["", "All statuses"],
    ["PENDING_VERIFICATION", "Pending verification"],
    ["SUCCESS", "Successful"],
    ["FAILED", "Failed"],
    ["REFUNDED", "Refunded"]
];
const METHOD_OPTIONS = [
    ["", "All methods"],
    ["UPI", "UPI"],
    ["BANK_TRANSFER", "Bank transfer"],
    ["CASH", "Cash"]
];

const money = (value) => `₹${Number(value ?? 0).toFixed(2)}`;
const formatDate = (value) => (value ? new Date(value).toLocaleString() : "-");

function statusLabel(status) {
    if (status === "PENDING_VERIFICATION") return "PENDING";
    if (status === "SUCCESS") return "SUCCESSFUL";
    return status;
}

function badgeClass(status) {
    if (status === "SUCCESS") return "bg-success";
    if (status === "FAILED") return "bg-danger";
    if (status === "REFUNDED") return "bg-secondary";
    return "bg-warning text-dark";
}

/** Removes empty values so that the URL only carries real filters. */
function clean(values) {
    const result = {};
    Object.entries(values).forEach(([key, value]) => {
        if (value !== "" && value !== null && value !== undefined) {
            result[key] = value;
        }
    });
    return result;
}

/** A row of small bars: the amount of money per day or per month. */
function RevenueBars({ title, rows, labelKey }) {
    const max = Math.max(1, ...rows.map((row) => row.amount));
    return (
        <div className="card shadow-sm h-100">
            <div className="card-body">
                <h6>{title}</h6>
                {rows.map((row) => (
                    <div className="d-flex align-items-center mb-1" key={row[labelKey]}>
                        <span className="small text-muted me-2" style={{ width: "5.5rem" }}>
                            {row[labelKey]}
                        </span>
                        <div className="progress flex-grow-1" style={{ height: "0.9rem" }}>
                            <div
                                className="progress-bar bg-success"
                                role="progressbar"
                                style={{ width: `${(row.amount / max) * 100}%` }}
                                aria-valuenow={row.amount}
                                aria-valuemin="0"
                                aria-valuemax={max}
                                aria-label={`${row[labelKey]}: ${money(row.amount)}`}
                            />
                        </div>
                        <span className="small ms-2" style={{ width: "5rem", textAlign: "right" }}>
                            {money(row.amount)}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}

function AdminPayments() {
    const [params] = useSearchParams();

    // The dashboard cards open this page as /admin/payments?status=PENDING_VERIFICATION
    const [status, setStatus] = useState(params.get("status") ?? "");
    const [method, setMethod] = useState("");
    const [searchText, setSearchText] = useState("");
    const [search, setSearch] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [page, setPage] = useState(0);

    const [expandedId, setExpandedId] = useState(null);
    const [action, setAction] = useState(null); // { payment, kind: "reject" | "refund" }
    const [reason, setReason] = useState("");
    const [notice, setNotice] = useState(null); // { text, error }

    const filters = clean({ status, method, search, from, to });
    const list = useApi("/admin/payments", { ...filters, page, size: PAGE_SIZE });
    const summary = useApi("/admin/payments/summary");

    const show = (text, error = false) => setNotice({ text, error });
    const reloadAll = () => {
        list.reload();
        summary.reload();
    };

    const applySearch = () => {
        setSearch(searchText.trim());
        setPage(0);
    };

    const clearFilters = () => {
        setStatus("");
        setMethod("");
        setSearchText("");
        setSearch("");
        setFrom("");
        setTo("");
        setPage(0);
    };

    const verify = async (payment) => {
        if (!window.confirm(`Confirm that the money for payment #${payment.id} was received?`)) {
            return;
        }
        try {
            await api.put(`/payments/${payment.id}/verify`, { status: "SUCCESS" });
            show(`Payment #${payment.id} was verified`);
        } catch (error) {
            show(error.response?.data?.message || "Could not verify the payment", true);
        } finally {
            reloadAll();
        }
    };

    const submitAction = async () => {
        if (!action) {
            return;
        }
        const text = reason.trim();
        if (!text) {
            show("A reason is required", true);
            return;
        }
        const { payment, kind } = action;
        try {
            if (kind === "reject") {
                await api.put(`/payments/${payment.id}/verify`, { status: "FAILED", reason: text });
                show(`Payment #${payment.id} was rejected`);
            } else {
                await api.put(`/admin/payments/${payment.id}/refund`, { reason: text });
                show(`Payment #${payment.id} was refunded`);
            }
            setAction(null);
            setReason("");
        } catch (error) {
            show(error.response?.data?.message || "The action failed", true);
        } finally {
            reloadAll();
        }
    };

    const downloadReport = async () => {
        try {
            await downloadFile("/admin/payments/report.csv", "payments.csv", filters);
        } catch {
            show("Could not download the report", true);
        }
    };

    const payments = list.data?.content ?? [];
    const totals = summary.data;

    return (
        <div className="container py-4">
            <h2>Payment management</h2>
            <p className="text-muted">
                Manual / demo payments: a user submits a payment reference and you confirm that the
                money really arrived. Nothing is marked successful automatically.
            </p>

            {notice && (
                <div className={`alert ${notice.error ? "alert-danger" : "alert-success"}`} role="alert">
                    {notice.text}
                </div>
            )}
            {(list.error || summary.error) && (
                <div className="alert alert-danger" role="alert">{list.error || summary.error}</div>
            )}

            {totals && (
                <>
                    <div className="row g-3 mb-3">
                        {[
                            ["Revenue today", money(totals.revenueToday), "success", null],
                            ["Revenue this month", money(totals.revenueThisMonth), "success", null],
                            ["Total revenue", money(totals.revenueTotal), "success", null],
                            ["Pending verification", totals.pending, "warning", "PENDING_VERIFICATION"],
                            ["Successful", totals.successful, "success", "SUCCESS"],
                            ["Failed", totals.failed, "danger", "FAILED"],
                            ["Refunded", totals.refunded, "secondary", "REFUNDED"]
                        ].map(([title, value, color, filterStatus]) => (
                            <div className="col-6 col-md-4 col-xl-3" key={title}>
                                <div className={`card border-${color} shadow-sm h-100`}>
                                    <div className="card-body">
                                        <h6 className="text-muted small mb-1">{title}</h6>
                                        <h4 className={`text-${color} mb-1`}>{value}</h4>
                                        {filterStatus && (
                                            <button
                                                type="button"
                                                className="btn btn-link btn-sm p-0"
                                                onClick={() => {
                                                    setStatus(filterStatus);
                                                    setPage(0);
                                                }}
                                            >
                                                Show these
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="row g-3 mb-4">
                        <div className="col-12 col-lg-6">
                            <RevenueBars title="Revenue per day (last 14 days)" rows={totals.daily} labelKey="date" />
                        </div>
                        <div className="col-12 col-lg-6">
                            <RevenueBars title="Revenue per month (last 6 months)" rows={totals.monthly} labelKey="month" />
                        </div>
                    </div>
                </>
            )}

            {action && (
                <div className="card border-warning shadow-sm mb-3">
                    <div className="card-body">
                        <h5>
                            {action.kind === "reject" ? "Reject" : "Refund"} payment #{action.payment.id}
                        </h5>
                        <label htmlFor="action-reason" className="form-label">Reason (required)</label>
                        <textarea
                            id="action-reason"
                            className="form-control mb-2"
                            rows="2"
                            maxLength="200"
                            value={reason}
                            onChange={(e) => setReason(e.target.value)}
                        />
                        <button type="button" className="btn btn-danger me-2" onClick={submitAction}>
                            Confirm {action.kind === "reject" ? "rejection" : "refund"}
                        </button>
                        <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={() => {
                                setAction(null);
                                setReason("");
                            }}
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            )}

            <div className="row g-2 mb-3">
                <div className="col-12 col-md-4">
                    <label htmlFor="pay-search" className="form-label small mb-1">
                        Search user, email, reference or ID
                    </label>
                    <div className="input-group">
                        <input
                            id="pay-search"
                            className="form-control"
                            value={searchText}
                            onChange={(e) => setSearchText(e.target.value)}
                            onKeyDown={(e) => e.key === "Enter" && applySearch()}
                        />
                        <button type="button" className="btn btn-primary" onClick={applySearch}>
                            Search
                        </button>
                    </div>
                </div>
                <div className="col-6 col-md-2">
                    <label htmlFor="pay-status" className="form-label small mb-1">Status</label>
                    <select
                        id="pay-status"
                        className="form-select"
                        value={status}
                        onChange={(e) => {
                            setStatus(e.target.value);
                            setPage(0);
                        }}
                    >
                        {STATUS_OPTIONS.map(([value, label]) => (
                            <option key={value} value={value}>{label}</option>
                        ))}
                    </select>
                </div>
                <div className="col-6 col-md-2">
                    <label htmlFor="pay-method" className="form-label small mb-1">Method</label>
                    <select
                        id="pay-method"
                        className="form-select"
                        value={method}
                        onChange={(e) => {
                            setMethod(e.target.value);
                            setPage(0);
                        }}
                    >
                        {METHOD_OPTIONS.map(([value, label]) => (
                            <option key={value} value={value}>{label}</option>
                        ))}
                    </select>
                </div>
                <div className="col-6 col-md-2">
                    <label htmlFor="pay-from" className="form-label small mb-1">From</label>
                    <input
                        id="pay-from"
                        type="date"
                        className="form-control"
                        value={from}
                        max={to || undefined}
                        onChange={(e) => {
                            setFrom(e.target.value);
                            setPage(0);
                        }}
                    />
                </div>
                <div className="col-6 col-md-2">
                    <label htmlFor="pay-to" className="form-label small mb-1">To</label>
                    <input
                        id="pay-to"
                        type="date"
                        className="form-control"
                        value={to}
                        min={from || undefined}
                        onChange={(e) => {
                            setTo(e.target.value);
                            setPage(0);
                        }}
                    />
                </div>
            </div>
            <div className="d-flex gap-2 mb-3">
                <button type="button" className="btn btn-outline-secondary" onClick={clearFilters}>
                    Clear filters
                </button>
                <button type="button" className="btn btn-outline-primary" onClick={downloadReport}>
                    Download payment report (CSV)
                </button>
            </div>

            <div className="table-responsive">
                <table className="table table-bordered table-hover align-middle">
                    <caption className="visually-hidden">Payments</caption>
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Booking</th>
                            <th scope="col">User</th>
                            <th scope="col">Amount</th>
                            <th scope="col">Method</th>
                            <th scope="col">Reference</th>
                            <th scope="col">Date</th>
                            <th scope="col">Status</th>
                            <th scope="col">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {list.loading && payments.length === 0 && (
                            <tr><td colSpan="9">Loading...</td></tr>
                        )}
                        {!list.loading && payments.length === 0 && (
                            <tr><td colSpan="9" className="text-center text-muted">No payments match these filters.</td></tr>
                        )}
                        {payments.map((p) => (
                            <Fragment key={p.id}>
                                <tr>
                                    <td>#{p.id}</td>
                                    <td>#{p.booking?.id ?? "-"}</td>
                                    <td>{p.booking?.user?.name ?? "-"}</td>
                                    <td>{money(p.amount)}</td>
                                    <td>{p.paymentMethod}</td>
                                    <td>{p.transactionReference}</td>
                                    <td>{formatDate(p.paymentTime)}</td>
                                    <td>
                                        <span className={`badge ${badgeClass(p.status)}`}>{statusLabel(p.status)}</span>
                                    </td>
                                    <td>
                                        <div className="d-flex gap-1 flex-wrap">
                                            <button
                                                type="button"
                                                className="btn btn-outline-primary btn-sm"
                                                aria-expanded={expandedId === p.id}
                                                onClick={() => setExpandedId(expandedId === p.id ? null : p.id)}
                                            >
                                                {expandedId === p.id ? "Hide" : "Details"}
                                            </button>
                                            {p.status === "PENDING_VERIFICATION" && (
                                                <>
                                                    <button
                                                        type="button"
                                                        className="btn btn-success btn-sm"
                                                        onClick={() => verify(p)}
                                                    >
                                                        Verify
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="btn btn-danger btn-sm"
                                                        onClick={() => {
                                                            setAction({ payment: p, kind: "reject" });
                                                            setReason("");
                                                        }}
                                                    >
                                                        Reject
                                                    </button>
                                                </>
                                            )}
                                            {p.status === "SUCCESS" && (
                                                <button
                                                    type="button"
                                                    className="btn btn-outline-danger btn-sm"
                                                    onClick={() => {
                                                        setAction({ payment: p, kind: "refund" });
                                                        setReason("");
                                                    }}
                                                >
                                                    Refund
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                                {expandedId === p.id && (
                                    <tr>
                                        <td colSpan="9" className="bg-light">
                                            <div className="row small">
                                                <div className="col-md-4">
                                                    <strong>User:</strong> {p.booking?.user?.name} ({p.booking?.user?.email})
                                                </div>
                                                <div className="col-md-4">
                                                    <strong>Vehicle:</strong> {p.booking?.vehicle?.vehicleNumber ?? "-"}
                                                    {" | "}
                                                    <strong>Slot:</strong> {p.booking?.parkingSlot?.slotNumber ?? "-"}
                                                </div>
                                                <div className="col-md-4">
                                                    <strong>Booking status:</strong> {p.booking?.status ?? "-"}
                                                </div>
                                                <div className="col-12">
                                                    <strong>Parking time:</strong> {formatDate(p.booking?.startTime)} to {formatDate(p.booking?.endTime)}
                                                </div>
                                                <div className="col-12">
                                                    <strong>Admin note:</strong> {p.reviewReason || "-"}
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                )}
                            </Fragment>
                        ))}
                    </tbody>
                </table>
            </div>

            <div className="text-muted small">
                {list.data ? `${list.data.totalElements} payment(s) found` : ""}
            </div>
            <Pagination page={page} totalPages={list.data?.totalPages} onChange={setPage} />
        </div>
    );
}

export default AdminPayments;
