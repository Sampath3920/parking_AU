import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../services/api";
import useApi from "../hooks/useApi";
import Pagination from "../components/Pagination";
import { getUser } from "../services/auth";

const PAGE_SIZE = 10;
const ROLES = ["STUDENT", "FACULTY", "STAFF", "VISITOR", "GUARD", "ADMIN"];

const formatDate = (value) => (value ? new Date(value).toLocaleString() : "-");

function statusBadge(status) {
    if (status === "VERIFIED" || status === "SUCCESS" || status === "BOOKED" || status === "COMPLETED") {
        return "bg-success";
    }
    if (status === "REJECTED" || status === "FAILED" || status === "CANCELLED") {
        return "bg-danger";
    }
    return "bg-warning text-dark";
}

function AdminUsers() {
    const currentUser = getUser();
    const [params] = useSearchParams();

    // Filters. The dashboard cards open this page as /admin/users?role=STUDENT or ?status=INACTIVE
    const [searchText, setSearchText] = useState("");
    const [search, setSearch] = useState("");
    const [role, setRole] = useState(params.get("role") ?? "");
    const [status, setStatus] = useState(params.get("status") ?? "");
    const [page, setPage] = useState(0);

    const [notice, setNotice] = useState(null); // { text, error }
    const [detailsId, setDetailsId] = useState(null);

    const list = useApi("/admin/users", { search, role, status, page, size: PAGE_SIZE });
    const details = useApi(detailsId ? `/admin/users/${detailsId}/details` : null);

    const show = (text, error = false) => setNotice({ text, error });

    const applySearch = () => {
        setSearch(searchText.trim());
        setPage(0);
    };

    const clearFilters = () => {
        setSearchText("");
        setSearch("");
        setRole("");
        setStatus("");
        setPage(0);
    };

    const changeRole = async (user, newRole) => {
        try {
            await api.put(`/admin/users/${user.id}/role`, { role: newRole });
            show(`${user.name} is now ${newRole}`);
        } catch (error) {
            show(error.response?.data?.message || "Could not change the role", true);
        } finally {
            list.reload();
        }
    };

    const toggleActive = async (user) => {
        const makeActive = !user.active;
        const question = makeActive
            ? `Activate the account of ${user.name}?`
            : `Deactivate the account of ${user.name}? They will not be able to log in.`;
        if (!window.confirm(question)) {
            return;
        }
        try {
            await api.put(`/admin/users/${user.id}/status`, { active: makeActive });
            show(`${user.name} was ${makeActive ? "activated" : "deactivated"}`);
        } catch (error) {
            show(error.response?.data?.message || "Could not change the account status", true);
        } finally {
            list.reload();
        }
    };

    const users = list.data?.content ?? [];

    return (
        <div className="container py-4">
            <h2>Users and accounts</h2>
            <p className="text-muted">
                Search people, change their role, and activate or deactivate accounts.
                Administrator and guard roles can only be given here.
            </p>

            {notice && (
                <div className={`alert ${notice.error ? "alert-danger" : "alert-success"}`} role="alert">
                    {notice.text}
                </div>
            )}
            {list.error && <div className="alert alert-danger" role="alert">{list.error}</div>}

            <div className="row g-2 mb-3">
                <div className="col-12 col-md-5">
                    <label htmlFor="user-search" className="form-label small mb-1">
                        Search by name, email, phone or ID
                    </label>
                    <div className="input-group">
                        <input
                            id="user-search"
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
                    <label htmlFor="user-role" className="form-label small mb-1">Role</label>
                    <select
                        id="user-role"
                        className="form-select"
                        value={role}
                        onChange={(e) => {
                            setRole(e.target.value);
                            setPage(0);
                        }}
                    >
                        <option value="">All roles</option>
                        {ROLES.map((r) => (
                            <option key={r} value={r}>{r}</option>
                        ))}
                    </select>
                </div>
                <div className="col-6 col-md-2">
                    <label htmlFor="user-status" className="form-label small mb-1">Account</label>
                    <select
                        id="user-status"
                        className="form-select"
                        value={status}
                        onChange={(e) => {
                            setStatus(e.target.value);
                            setPage(0);
                        }}
                    >
                        <option value="">All</option>
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Deactivated</option>
                    </select>
                </div>
                <div className="col-12 col-md-3 d-flex align-items-end">
                    <button type="button" className="btn btn-outline-secondary" onClick={clearFilters}>
                        Clear filters
                    </button>
                </div>
            </div>

            <div className="table-responsive">
                <table className="table table-bordered table-hover align-middle">
                    <caption className="visually-hidden">Users</caption>
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Name</th>
                            <th scope="col">Email</th>
                            <th scope="col">Phone</th>
                            <th scope="col">Role</th>
                            <th scope="col">Account</th>
                            <th scope="col">Registered</th>
                            <th scope="col">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {list.loading && users.length === 0 && (
                            <tr><td colSpan="8">Loading...</td></tr>
                        )}
                        {!list.loading && users.length === 0 && (
                            <tr><td colSpan="8" className="text-center text-muted">No users match these filters.</td></tr>
                        )}
                        {users.map((u) => {
                            const isMe = u.id === currentUser?.id;
                            return (
                                <tr key={u.id}>
                                    <td>{u.id}</td>
                                    <td>{u.name}</td>
                                    <td>{u.email}</td>
                                    <td>{u.phone || "-"}</td>
                                    <td>
                                        <select
                                            className="form-select form-select-sm"
                                            aria-label={`Role for ${u.name}`}
                                            value={(u.role || "").toUpperCase()}
                                            disabled={isMe}
                                            title={isMe ? "You cannot change your own role" : ""}
                                            onChange={(e) => changeRole(u, e.target.value)}
                                        >
                                            {ROLES.map((r) => (
                                                <option key={r} value={r}>{r}</option>
                                            ))}
                                        </select>
                                    </td>
                                    <td>
                                        <span className={`badge ${u.active ? "bg-success" : "bg-danger"}`}>
                                            {u.active ? "ACTIVE" : "DEACTIVATED"}
                                        </span>
                                    </td>
                                    <td>{formatDate(u.createdAt)}</td>
                                    <td>
                                        <div className="d-flex gap-1 flex-wrap">
                                            <button
                                                type="button"
                                                className="btn btn-outline-primary btn-sm"
                                                onClick={() => setDetailsId(u.id)}
                                            >
                                                Details
                                            </button>
                                            <button
                                                type="button"
                                                className={`btn btn-sm ${u.active ? "btn-outline-danger" : "btn-outline-success"}`}
                                                disabled={isMe}
                                                title={isMe ? "You cannot deactivate your own account" : ""}
                                                onClick={() => toggleActive(u)}
                                            >
                                                {u.active ? "Deactivate" : "Activate"}
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>

            <div className="text-muted small">
                {list.data ? `${list.data.totalElements} user(s) found` : ""}
            </div>
            <Pagination page={page} totalPages={list.data?.totalPages} onChange={setPage} />

            {detailsId && (
                <div className="card shadow-sm mt-4" aria-live="polite">
                    <div className="card-header d-flex justify-content-between align-items-center">
                        <strong>
                            Details: {details.data?.user?.name ?? `user #${detailsId}`}
                        </strong>
                        <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => setDetailsId(null)}
                        >
                            Close
                        </button>
                    </div>
                    <div className="card-body">
                        {details.loading && !details.data && <p>Loading...</p>}
                        {details.error && <div className="alert alert-danger">{details.error}</div>}
                        {details.data && (
                            <>
                                <p className="mb-3">
                                    {details.data.user.email} | {details.data.user.role} |{" "}
                                    {details.data.user.active ? "Active" : "Deactivated"}
                                </p>

                                <h6>Vehicles ({details.data.vehicles.length})</h6>
                                <div className="table-responsive mb-3">
                                    <table className="table table-sm">
                                        <thead>
                                            <tr><th>Number</th><th>Type</th><th>Model</th><th>Verification</th></tr>
                                        </thead>
                                        <tbody>
                                            {details.data.vehicles.length === 0 && (
                                                <tr><td colSpan="4" className="text-muted">No vehicles.</td></tr>
                                            )}
                                            {details.data.vehicles.map((v) => (
                                                <tr key={v.id}>
                                                    <td>{v.vehicleNumber}</td>
                                                    <td>{v.vehicleType}</td>
                                                    <td>{v.vehicleModel}</td>
                                                    <td>
                                                        <span className={`badge ${statusBadge(v.verificationStatus)}`}>
                                                            {v.verificationStatus}
                                                        </span>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                <h6>Bookings and parking history ({details.data.bookings.length})</h6>
                                <div className="table-responsive mb-3">
                                    <table className="table table-sm">
                                        <thead>
                                            <tr><th>Booking</th><th>Vehicle</th><th>Slot</th><th>From</th><th>To</th><th>Status</th></tr>
                                        </thead>
                                        <tbody>
                                            {details.data.bookings.length === 0 && (
                                                <tr><td colSpan="6" className="text-muted">No bookings.</td></tr>
                                            )}
                                            {details.data.bookings.map((b) => (
                                                <tr key={b.id}>
                                                    <td>#{b.id}</td>
                                                    <td>{b.vehicle?.vehicleNumber ?? "-"}</td>
                                                    <td>{b.parkingSlot?.slotNumber ?? "-"}</td>
                                                    <td>{formatDate(b.startTime)}</td>
                                                    <td>{formatDate(b.endTime)}</td>
                                                    <td>
                                                        <span className={`badge ${statusBadge(b.status)}`}>{b.status}</span>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                <h6>Payments ({details.data.payments.length})</h6>
                                <div className="table-responsive">
                                    <table className="table table-sm">
                                        <thead>
                                            <tr><th>Payment</th><th>Booking</th><th>Amount</th><th>Method</th><th>Date</th><th>Status</th></tr>
                                        </thead>
                                        <tbody>
                                            {details.data.payments.length === 0 && (
                                                <tr><td colSpan="6" className="text-muted">No payments.</td></tr>
                                            )}
                                            {details.data.payments.map((p) => (
                                                <tr key={p.id}>
                                                    <td>#{p.id}</td>
                                                    <td>#{p.booking?.id ?? "-"}</td>
                                                    <td>₹{p.amount}</td>
                                                    <td>{p.paymentMethod}</td>
                                                    <td>{formatDate(p.paymentTime)}</td>
                                                    <td>
                                                        <span className={`badge ${statusBadge(p.status)}`}>{p.status}</span>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

export default AdminUsers;
