import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../services/api";
import useApi from "../hooks/useApi";
import Pagination from "../components/Pagination";
import { STATUS_LABEL, statusBadge } from "../services/reportStatus";

const PAGE_SIZE = 10;

const STATUS_FILTERS = [
    ["", "All statuses"],
    ["UNRESOLVED", "Unresolved (new + in progress)"],
    ["SUBMITTED", "Submitted"],
    ["IN_PROGRESS", "In progress"],
    ["RESOLVED", "Resolved"],
    ["REJECTED", "Rejected"]
];

const formatDate = (value) => (value ? new Date(value).toLocaleString() : "-");
const typeLabel = (type) => (type === "COMPLAINT" ? "Complaint" : "Damage");

function AdminReports() {
    const [params] = useSearchParams();

    // The dashboard cards open this page as /admin/reports?type=DAMAGE&status=UNRESOLVED
    const [type, setType] = useState(params.get("type") ?? "");
    const [status, setStatus] = useState(params.get("status") ?? "");
    const [searchText, setSearchText] = useState("");
    const [search, setSearch] = useState("");
    const [page, setPage] = useState(0);

    const [openId, setOpenId] = useState(null);
    const [edit, setEdit] = useState(null); // form values of the open report
    const [saving, setSaving] = useState(false);
    const [notice, setNotice] = useState(null); // { text, error }

    const list = useApi("/admin/service-reports", { type, status, search, page, size: PAGE_SIZE });
    const history = useApi(
        openId ? "/admin/audit" : null,
        openId ? { targetType: "SERVICE_REPORT", targetId: openId } : undefined
    );

    const reports = list.data?.content ?? [];
    const open = reports.find((r) => r.id === openId) ?? null;

    const openReport = (report) => {
        setOpenId(report.id);
        setEdit({
            status: report.status,
            response: report.adminResponse ?? "",
            assignment: "",
            note: "",
            slotAction: ""
        });
    };

    const applySearch = () => {
        setSearch(searchText.trim());
        setPage(0);
    };

    const save = async () => {
        if (!open || !edit) {
            return;
        }
        // Only send what changed, so that the audit log stays readable.
        const body = {};
        if (edit.status !== open.status) body.status = edit.status;
        if (edit.response.trim() !== (open.adminResponse ?? "")) body.response = edit.response.trim();
        if (edit.assignment) body.assignment = edit.assignment;
        if (edit.note.trim()) body.note = edit.note.trim();
        if (edit.slotAction) body.slotAction = edit.slotAction;

        if (Object.keys(body).length === 0) {
            setNotice({ text: "Nothing was changed", error: true });
            return;
        }

        setSaving(true);
        try {
            await api.put(`/admin/service-reports/${open.id}`, body);
            setNotice({ text: `Report #${open.id} was updated`, error: false });
            setEdit((current) => ({ ...current, assignment: "", note: "", slotAction: "" }));
            list.reload();
            history.reload();
        } catch (error) {
            setNotice({ text: error.response?.data?.message || "Could not update the report", error: true });
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="container py-4">
            <h2>Complaints and damage reports</h2>
            <p className="text-muted">
                Answer the report, assign it to yourself and change its status. The person is notified
                when the status or the answer changes. For a damaged slot you can mark the slot as
                under maintenance so that nobody can book it until you clear it.
            </p>

            {notice && (
                <div className={`alert ${notice.error ? "alert-danger" : "alert-success"}`} role="alert">
                    {notice.text}
                </div>
            )}
            {list.error && <div className="alert alert-danger" role="alert">{list.error}</div>}

            <div className="row g-2 mb-3">
                <div className="col-12 col-md-4">
                    <label htmlFor="rep-search" className="form-label small mb-1">
                        Search subject, text, person or ID
                    </label>
                    <div className="input-group">
                        <input
                            id="rep-search"
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
                <div className="col-6 col-md-3">
                    <label htmlFor="rep-type" className="form-label small mb-1">Type</label>
                    <select
                        id="rep-type"
                        className="form-select"
                        value={type}
                        onChange={(e) => {
                            setType(e.target.value);
                            setPage(0);
                        }}
                    >
                        <option value="">All</option>
                        <option value="COMPLAINT">Complaints</option>
                        <option value="DAMAGE">Damage reports</option>
                    </select>
                </div>
                <div className="col-6 col-md-3">
                    <label htmlFor="rep-status" className="form-label small mb-1">Status</label>
                    <select
                        id="rep-status"
                        className="form-select"
                        value={status}
                        onChange={(e) => {
                            setStatus(e.target.value);
                            setPage(0);
                        }}
                    >
                        {STATUS_FILTERS.map(([value, label]) => (
                            <option key={value} value={value}>{label}</option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="table-responsive">
                <table className="table table-bordered table-hover align-middle">
                    <caption className="visually-hidden">Reports</caption>
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Type</th>
                            <th scope="col">Subject</th>
                            <th scope="col">From</th>
                            <th scope="col">Sent</th>
                            <th scope="col">Handler</th>
                            <th scope="col">Status</th>
                            <th scope="col">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        {list.loading && reports.length === 0 && (
                            <tr><td colSpan="8">Loading...</td></tr>
                        )}
                        {!list.loading && reports.length === 0 && (
                            <tr><td colSpan="8" className="text-center text-muted">No reports match these filters.</td></tr>
                        )}
                        {reports.map((r) => (
                            <tr key={r.id} className={r.id === openId ? "table-active" : ""}>
                                <td>#{r.id}</td>
                                <td>{typeLabel(r.type)}</td>
                                <td>{r.subject}</td>
                                <td>{r.reporter?.name}</td>
                                <td>{formatDate(r.createdAt)}</td>
                                <td>{r.assignedToName ?? "-"}</td>
                                <td>
                                    <span className={`badge ${statusBadge(r.status)}`}>
                                        {STATUS_LABEL[r.status] ?? r.status}
                                    </span>
                                </td>
                                <td>
                                    <button
                                        type="button"
                                        className="btn btn-outline-primary btn-sm"
                                        onClick={() => openReport(r)}
                                    >
                                        Open
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <div className="text-muted small">
                {list.data ? `${list.data.totalElements} report(s) found` : ""}
            </div>
            <Pagination page={page} totalPages={list.data?.totalPages} onChange={setPage} />

            {open && edit && (
                <div className="card shadow-sm mt-4">
                    <div className="card-header d-flex justify-content-between align-items-center">
                        <strong>
                            {typeLabel(open.type)} #{open.id}: {open.subject}
                        </strong>
                        <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => setOpenId(null)}
                        >
                            Close
                        </button>
                    </div>
                    <div className="card-body">
                        <p className="mb-2">{open.description}</p>
                        <ul className="small text-muted mb-3">
                            <li>
                                From: {open.reporter?.name} ({open.reporter?.email}), category {open.category}
                            </li>
                            {open.incidentDate && <li>Date it happened: {open.incidentDate}</li>}
                            {open.vehicle && (
                                <li>Vehicle: {open.vehicle.vehicleNumber} ({open.vehicle.vehicleType})</li>
                            )}
                            {open.parkingSlot && (
                                <li>
                                    Slot: {open.parkingSlot.parkingLocation?.name} - {open.parkingSlot.slotNumber}{" "}
                                    (now {open.parkingSlot.status})
                                </li>
                            )}
                        </ul>

                        <div className="row g-3">
                            <div className="col-12 col-md-4">
                                <label htmlFor="edit-status" className="form-label">Status</label>
                                <select
                                    id="edit-status"
                                    className="form-select"
                                    value={edit.status}
                                    onChange={(e) => setEdit({ ...edit, status: e.target.value })}
                                >
                                    {Object.entries(STATUS_LABEL).map(([value, label]) => (
                                        <option key={value} value={value}>{label}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="col-12 col-md-4">
                                <label htmlFor="edit-assign" className="form-label">Handler</label>
                                <select
                                    id="edit-assign"
                                    className="form-select"
                                    value={edit.assignment}
                                    onChange={(e) => setEdit({ ...edit, assignment: e.target.value })}
                                >
                                    <option value="">Keep as it is ({open.assignedToName ?? "nobody"})</option>
                                    <option value="ME">Assign to me</option>
                                    <option value="NONE">Remove handler</option>
                                </select>
                            </div>
                            {open.parkingSlot && (
                                <div className="col-12 col-md-4">
                                    <label htmlFor="edit-slot" className="form-label">Parking slot</label>
                                    <select
                                        id="edit-slot"
                                        className="form-select"
                                        value={edit.slotAction}
                                        onChange={(e) => setEdit({ ...edit, slotAction: e.target.value })}
                                    >
                                        <option value="">No change</option>
                                        <option value="FLAG">Mark as under maintenance (no new bookings)</option>
                                        <option value="CLEAR">Clear (open for booking again)</option>
                                    </select>
                                </div>
                            )}
                            <div className="col-12">
                                <label htmlFor="edit-response" className="form-label">
                                    Answer for the user (needed to resolve or reject)
                                </label>
                                <textarea
                                    id="edit-response"
                                    className="form-control"
                                    rows="3"
                                    maxLength="1000"
                                    value={edit.response}
                                    onChange={(e) => setEdit({ ...edit, response: e.target.value })}
                                />
                            </div>
                            <div className="col-12">
                                <label htmlFor="edit-note" className="form-label">
                                    Internal note (only in the history, the user does not see it)
                                </label>
                                <input
                                    id="edit-note"
                                    className="form-control"
                                    maxLength="500"
                                    value={edit.note}
                                    onChange={(e) => setEdit({ ...edit, note: e.target.value })}
                                />
                            </div>
                        </div>

                        <button type="button" className="btn btn-primary mt-3" onClick={save} disabled={saving}>
                            {saving ? "Saving..." : "Save changes"}
                        </button>

                        <h6 className="mt-4">History</h6>
                        {history.loading && !history.data && <p className="small">Loading...</p>}
                        <ul className="small">
                            {(history.data ?? []).map((entry) => (
                                <li key={entry.id}>
                                    {formatDate(entry.createdAt)} | {entry.actor} | {entry.action}: {entry.details}
                                </li>
                            ))}
                            {history.data && history.data.length === 0 && <li>No history yet.</li>}
                        </ul>
                    </div>
                </div>
            )}
        </div>
    );
}

export default AdminReports;
