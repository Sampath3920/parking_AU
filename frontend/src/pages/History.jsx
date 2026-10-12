import { useState } from "react";
import api from "../services/api";
import useApi from "../hooks/useApi";
import Pagination from "../components/Pagination";
import { getUser } from "../services/auth";

const PAGE_SIZE = 10;

const formatDate = (value) => (value ? new Date(value).toLocaleString() : "-");

function statusBadge(status) {
    if (status === "COMPLETED") return "badge bg-success";
    if (status === "BOOKED" || status === "ACTIVE") return "badge bg-primary";
    if (status === "REJECTED" || status === "CANCELLED" || status === "EXPIRED") return "badge bg-danger";
    return "badge bg-warning text-dark";
}

/** Quotes a value for CSV and stops spreadsheet formulas (=, +, -, @ at the start). */
function csvCell(value) {
    let text = value === null || value === undefined ? "" : String(value);
    if (text && "=+-@".includes(text[0])) {
        text = `'${text}`;
    }
    return `"${text.replace(/"/g, '""')}"`;
}

function History() {
    const userId = getUser()?.id;
    const [message, setMessage] = useState("");

    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [vehicle, setVehicle] = useState("");
    const [location, setLocation] = useState("");
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);

    const { data, loading, error, reload } = useApi(`/parking-history/user/${userId}`);
    const all = data ?? [];

    // Choices for the filter lists come from the person's own records.
    const vehicles = [...new Set(all.map((item) => item.vehicleNumber).filter(Boolean))];
    const locations = [...new Set(all.map((item) => item.locationName).filter(Boolean))];
    const statuses = [...new Set(all.map((item) => item.status).filter(Boolean))];

    const filtered = all
        .filter((item) => {
            const day = (item.startTime ?? item.bookingTime ?? "").slice(0, 10);
            return (!from || day >= from) && (!to || day <= to);
        })
        .filter((item) => !vehicle || item.vehicleNumber === vehicle)
        .filter((item) => !location || item.locationName === location)
        .filter((item) => !status || item.status === status)
        .sort((a, b) => String(b.bookingTime ?? "").localeCompare(String(a.bookingTime ?? "")));

    const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
    const safePage = Math.min(page, Math.max(totalPages - 1, 0));
    const visible = filtered.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);

    const changeFilter = (setter) => (event) => {
        setter(event.target.value);
        setPage(0);
    };

    const clearFilters = () => {
        setFrom("");
        setTo("");
        setVehicle("");
        setLocation("");
        setStatus("");
        setPage(0);
    };

    const cancelBooking = async (bookingId) => {
        if (!window.confirm("Cancel this booking?")) {
            return;
        }
        try {
            await api.put(`/bookings/${bookingId}/cancel`);
            setMessage("Booking cancelled");
        } catch (err) {
            setMessage(err.response?.data?.message || "Could not cancel the booking");
        } finally {
            reload();
        }
    };

    // Downloads exactly the rows that match the filters (all pages).
    const downloadCsv = () => {
        const header = [
            "Booking", "Vehicle", "Location", "Slot", "Booked at", "Start", "End",
            "Check in", "Check out", "Payment amount", "Payment status", "Status", "Note"
        ];
        const rows = filtered.map((item) => [
            item.bookingId, item.vehicleNumber, item.locationName, item.slotNumber,
            item.bookingTime, item.startTime, item.endTime, item.checkInTime, item.checkOutTime,
            item.paymentAmount, item.paymentStatus, item.status, item.decisionReason
        ]);
        const csv = [header, ...rows].map((row) => row.map(csvCell).join(",")).join("\n");

        const url = window.URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
        const link = document.createElement("a");
        link.href = url;
        link.download = "my-parking-history.csv";
        link.click();
        window.URL.revokeObjectURL(url);
    };

    return (
        <div className="container mt-4 mb-5">
            <h2 className="text-center mb-4">Parking History</h2>

            {message && <div className="alert alert-info" role="alert">{message}</div>}
            {error && <div className="alert alert-danger" role="alert">{error}</div>}
            {loading && all.length === 0 && <p>Loading...</p>}

            {!loading && all.length === 0 && !error ? (
                <div className="alert alert-info">No parking history found.</div>
            ) : (
                all.length > 0 && (
                    <>
                        <div className="row g-2 mb-3">
                            <div className="col-6 col-md-2">
                                <label htmlFor="h-from" className="form-label small mb-1">From</label>
                                <input id="h-from" type="date" className="form-control" value={from}
                                    max={to || undefined} onChange={changeFilter(setFrom)} />
                            </div>
                            <div className="col-6 col-md-2">
                                <label htmlFor="h-to" className="form-label small mb-1">To</label>
                                <input id="h-to" type="date" className="form-control" value={to}
                                    min={from || undefined} onChange={changeFilter(setTo)} />
                            </div>
                            <div className="col-6 col-md-2">
                                <label htmlFor="h-vehicle" className="form-label small mb-1">Vehicle</label>
                                <select id="h-vehicle" className="form-select" value={vehicle} onChange={changeFilter(setVehicle)}>
                                    <option value="">All</option>
                                    {vehicles.map((v) => <option key={v} value={v}>{v}</option>)}
                                </select>
                            </div>
                            <div className="col-6 col-md-2">
                                <label htmlFor="h-location" className="form-label small mb-1">Location</label>
                                <select id="h-location" className="form-select" value={location} onChange={changeFilter(setLocation)}>
                                    <option value="">All</option>
                                    {locations.map((l) => <option key={l} value={l}>{l}</option>)}
                                </select>
                            </div>
                            <div className="col-6 col-md-2">
                                <label htmlFor="h-status" className="form-label small mb-1">Status</label>
                                <select id="h-status" className="form-select" value={status} onChange={changeFilter(setStatus)}>
                                    <option value="">All</option>
                                    {statuses.map((s) => <option key={s} value={s}>{s}</option>)}
                                </select>
                            </div>
                            <div className="col-6 col-md-2 d-flex align-items-end gap-2">
                                <button type="button" className="btn btn-outline-secondary" onClick={clearFilters}>
                                    Clear
                                </button>
                            </div>
                        </div>

                        <div className="d-flex justify-content-between align-items-center mb-2">
                            <span className="text-muted small">{filtered.length} record(s)</span>
                            <button
                                type="button"
                                className="btn btn-outline-primary btn-sm"
                                disabled={filtered.length === 0}
                                onClick={downloadCsv}
                            >
                                Download my history (CSV)
                            </button>
                        </div>

                        <div className="card shadow">
                            <div className="card-body">
                                <div className="table-responsive">
                                    <table className="table table-bordered table-hover align-middle">
                                        <caption className="visually-hidden">My parking history</caption>
                                        <thead>
                                            <tr>
                                                <th scope="col">Booking</th>
                                                <th scope="col">Vehicle</th>
                                                <th scope="col">Location / slot</th>
                                                <th scope="col">Booked for</th>
                                                <th scope="col">Check in</th>
                                                <th scope="col">Check out</th>
                                                <th scope="col">Payment</th>
                                                <th scope="col">Status</th>
                                                <th scope="col">Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {visible.length === 0 && (
                                                <tr>
                                                    <td colSpan="9" className="text-center text-muted">
                                                        No records match these filters.
                                                    </td>
                                                </tr>
                                            )}
                                            {visible.map((item) => (
                                                <tr key={item.bookingId}>
                                                    <td>#{item.bookingId}</td>
                                                    <td>{item.vehicleNumber}</td>
                                                    <td>
                                                        {item.locationName ? `${item.locationName} / ` : ""}
                                                        {item.slotNumber}
                                                    </td>
                                                    <td>
                                                        {item.startTime && item.endTime
                                                            ? `${formatDate(item.startTime)} - ${new Date(item.endTime).toLocaleTimeString()}`
                                                            : formatDate(item.bookingTime)}
                                                    </td>
                                                    <td>{formatDate(item.checkInTime)}</td>
                                                    <td>{formatDate(item.checkOutTime)}</td>
                                                    <td>
                                                        {item.paymentStatus
                                                            ? `₹${item.paymentAmount} (${item.paymentStatus})`
                                                            : "-"}
                                                    </td>
                                                    <td>
                                                        <span className={statusBadge(item.status)}>{item.status}</span>
                                                        {item.decisionReason && (
                                                            <div><small>{item.decisionReason}</small></div>
                                                        )}
                                                    </td>
                                                    <td>
                                                        {(item.status === "BOOKED" || item.status === "PENDING") && !item.checkInTime && (
                                                            <button
                                                                type="button"
                                                                className="btn btn-sm btn-outline-danger"
                                                                onClick={() => cancelBooking(item.bookingId)}
                                                            >
                                                                Cancel
                                                            </button>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                                <Pagination page={safePage} totalPages={totalPages} onChange={setPage} />
                            </div>
                        </div>
                    </>
                )
            )}
        </div>
    );
}

export default History;
