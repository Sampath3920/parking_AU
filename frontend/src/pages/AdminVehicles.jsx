import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../services/api";

const PAGE_SIZE = 10;
const FILTERS = [
    { value: "PENDING", label: "Pending" },
    { value: "VERIFIED", label: "Approved" },
    { value: "REJECTED", label: "Rejected" },
    { value: "", label: "All" }
];

function badgeClass(status) {
    if (status === "VERIFIED") return "bg-success";
    if (status === "REJECTED") return "bg-danger";
    return "bg-warning text-dark";
}

function AdminVehicles() {
    const [vehicles, setVehicles] = useState([]);
    // The dashboard cards open this page as /admin/vehicles?status=VERIFIED (or PENDING, REJECTED).
    const [params] = useSearchParams();
    const [status, setStatus] = useState(params.get("status") ?? "PENDING");
    const [search, setSearch] = useState("");
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    const [isError, setIsError] = useState(false);

    const [selected, setSelected] = useState(null);
    const [history, setHistory] = useState([]);
    const [rejecting, setRejecting] = useState(null);
    const [reason, setReason] = useState("");

    const show = (text, error = false) => {
        setMessage(text);
        setIsError(error);
    };

    // The loading flag is switched on by the handlers that change a filter (not inside this
    // function), so that the effect below does not set state synchronously.
    const load = async () => {
        try {
            const response = await api.get("/admin/vehicles", {
                params: { status, search }
            });
            setVehicles(response.data);
            setPage(0);
        } catch (error) {
            show(error.response?.data?.message || "Failed to load vehicles", true);
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

    const decide = async (vehicle, decision, decisionReason) => {
        try {
            await api.put(`/admin/vehicles/${vehicle.id}/decision`, {
                status: decision,
                reason: decisionReason
            });
            show(
                decision === "VERIFIED"
                    ? `Vehicle ${vehicle.vehicleNumber} approved`
                    : `Vehicle ${vehicle.vehicleNumber} rejected`
            );
            setRejecting(null);
            setReason("");
            if (selected?.id === vehicle.id) {
                setSelected(null);
            }
            await load();
        } catch (error) {
            show(error.response?.data?.message || "Could not save the decision", true);
        }
    };

    const confirmReject = () => {
        if (!reason.trim()) {
            show("Please write the reason for rejecting", true);
            return;
        }
        decide(rejecting, "REJECTED", reason.trim());
    };

    const openDetails = async (vehicle) => {
        setSelected(vehicle);
        setHistory([]);
        try {
            const response = await api.get(`/admin/vehicles/${vehicle.id}/history`);
            setHistory(response.data);
        } catch (error) {
            show(error.response?.data?.message || "Could not load the history", true);
        }
    };

    const pageCount = Math.max(1, Math.ceil(vehicles.length / PAGE_SIZE));
    const rows = vehicles.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

    return (
        <div className="container mt-4">
            <h2 className="mb-3">Vehicle Requests</h2>

            {message && (
                <div className={`alert ${isError ? "alert-danger" : "alert-success"}`} role="alert">
                    {message}
                </div>
            )}

            <div className="row g-2 mb-3">
                <div className="col-md-4">
                    <label htmlFor="vehicleStatus" className="form-label">Show</label>
                    <select
                        id="vehicleStatus"
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
                <div className="col-md-6">
                    <label htmlFor="vehicleSearch" className="form-label">
                        Search (number, model, owner name or email)
                    </label>
                    <input
                        id="vehicleSearch"
                        className="form-control"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") {
                                setLoading(true);
                                load();
                            }
                        }}
                    />
                </div>
                <div className="col-md-2 d-flex align-items-end">
                    <button type="button" className="btn btn-primary w-100" onClick={load}>
                        Search
                    </button>
                </div>
            </div>

            {rejecting && (
                <div className="card border-danger mb-3">
                    <div className="card-body">
                        <h5>Reject vehicle {rejecting.vehicleNumber}</h5>
                        <label htmlFor="rejectReason" className="form-label">
                            Reason (required, the owner will see it)
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
                <p className="text-muted">No vehicles found for this filter.</p>
            ) : (
                <div className="table-responsive">
                    <table className="table table-bordered table-hover align-middle">
                        <thead>
                            <tr>
                                <th>Vehicle</th>
                                <th>Type / model</th>
                                <th>Owner</th>
                                <th>Submitted</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map((v) => (
                                <tr key={v.id}>
                                    <td>{v.vehicleNumber}</td>
                                    <td>{v.vehicleType}<br />{v.vehicleModel}</td>
                                    <td>
                                        {v.user?.name}<br />
                                        <small>{v.user?.email} ({v.user?.role})</small>
                                    </td>
                                    <td>
                                        {v.submittedAt ? new Date(v.submittedAt).toLocaleString() : "-"}
                                    </td>
                                    <td>
                                        <span className={`badge ${badgeClass(v.verificationStatus)}`}>
                                            {v.verificationStatus}
                                        </span>
                                        {v.reviewReason && <div><small>{v.reviewReason}</small></div>}
                                    </td>
                                    <td>
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-outline-secondary me-1 mb-1"
                                            onClick={() => openDetails(v)}
                                        >
                                            Details
                                        </button>
                                        {v.verificationStatus !== "VERIFIED" && (
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-success me-1 mb-1"
                                                onClick={() => decide(v, "VERIFIED", "")}
                                            >
                                                Approve
                                            </button>
                                        )}
                                        {v.verificationStatus !== "REJECTED" && (
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-danger mb-1"
                                                onClick={() => {
                                                    setRejecting(v);
                                                    setReason("");
                                                }}
                                            >
                                                Reject
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
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
                <span>Page {page + 1} of {pageCount} ({vehicles.length} vehicles)</span>
                <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    disabled={page + 1 >= pageCount}
                    onClick={() => setPage(page + 1)}
                >
                    Next
                </button>
            </div>

            {selected && (
                <div className="card mb-4">
                    <div className="card-body">
                        <div className="d-flex justify-content-between">
                            <h5>Vehicle details: {selected.vehicleNumber}</h5>
                            <button
                                type="button"
                                className="btn btn-sm btn-outline-secondary"
                                onClick={() => setSelected(null)}
                            >
                                Close
                            </button>
                        </div>
                        <p className="mb-1">
                            Owner: {selected.user?.name} (user ID {selected.user?.id}, {selected.user?.email}, {selected.user?.role})
                        </p>
                        <p className="mb-1">
                            Type: {selected.vehicleType}, model: {selected.vehicleModel}
                        </p>
                        <p className="mb-1">
                            Status: {selected.verificationStatus}
                            {selected.reviewedAt && ` (reviewed ${new Date(selected.reviewedAt).toLocaleString()})`}
                        </p>
                        <h6 className="mt-3">Decision history</h6>
                        {history.length === 0 ? (
                            <p className="text-muted mb-0">No decisions yet.</p>
                        ) : (
                            <ul className="mb-0">
                                {history.map((h) => (
                                    <li key={h.id}>
                                        {new Date(h.createdAt).toLocaleString()} - {h.action} by {h.actor}
                                        {h.details ? ` (${h.details})` : ""}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

export default AdminVehicles;
