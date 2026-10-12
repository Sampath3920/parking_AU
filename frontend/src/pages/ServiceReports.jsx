import { useState } from "react";
import api from "../services/api";
import useApi from "../hooks/useApi";
import { getUser } from "../services/auth";
import { STATUS_LABEL, statusBadge } from "../services/reportStatus";

const CATEGORIES = {
    COMPLAINT: [
        ["BOOKING", "Booking problem"],
        ["PAYMENT", "Payment problem"],
        ["STAFF", "Staff or guard behaviour"],
        ["SAFETY", "Safety concern"],
        ["OTHER", "Something else"]
    ],
    DAMAGE: [
        ["DAMAGED_VEHICLE", "My vehicle was damaged"],
        ["SLOT_DAMAGE", "A parking slot is damaged"],
        ["AREA_ISSUE", "Problem in the parking area"]
    ]
};

const today = () => new Date().toISOString().slice(0, 10);
const formatDate = (value) => (value ? new Date(value).toLocaleString() : "-");
const categoryLabel = (type, code) =>
    (CATEGORIES[type] ?? []).find(([value]) => value === code)?.[1] ?? code;

const EMPTY_FORM = {
    type: "COMPLAINT",
    category: "BOOKING",
    subject: "",
    description: "",
    incidentDate: "",
    vehicleId: "",
    slotId: ""
};

function ServiceReports() {
    const userId = getUser()?.id;
    const [form, setForm] = useState(EMPTY_FORM);
    const [sending, setSending] = useState(false);
    const [notice, setNotice] = useState(null); // { text, error }
    const [openId, setOpenId] = useState(null);

    const reports = useApi("/service-reports/mine");
    const vehicles = useApi(`/vehicles/user/${userId}`);
    const slots = useApi("/parking-slots");

    const change = (field) => (e) => setForm({ ...form, [field]: e.target.value });

    const changeType = (type) => {
        setForm({ ...form, type, category: CATEGORIES[type][0][0] });
    };

    const submit = async (event) => {
        event.preventDefault();

        if (!form.subject.trim() || !form.description.trim()) {
            setNotice({ text: "Please write a subject and a description", error: true });
            return;
        }
        if (
            form.type === "DAMAGE" &&
            form.category !== "AREA_ISSUE" &&
            !form.vehicleId &&
            !form.slotId
        ) {
            setNotice({ text: "Choose the vehicle or the parking slot that was damaged", error: true });
            return;
        }

        setSending(true);
        try {
            await api.post("/service-reports", {
                type: form.type,
                category: form.category,
                subject: form.subject.trim(),
                description: form.description.trim(),
                incidentDate: form.incidentDate || null,
                vehicleId: form.vehicleId ? Number(form.vehicleId) : null,
                slotId: form.slotId ? Number(form.slotId) : null
            });
            setNotice({ text: "Your report was sent. You will get a notification when it is updated.", error: false });
            setForm({ ...EMPTY_FORM, type: form.type, category: CATEGORIES[form.type][0][0] });
            reports.reload();
        } catch (error) {
            setNotice({ text: error.response?.data?.message || "Could not send the report", error: true });
        } finally {
            setSending(false);
        }
    };

    const items = reports.data ?? [];

    return (
        <div className="container py-4">
            <h2>Complaints and damage reports</h2>
            <p className="text-muted">
                Tell the parking office about a problem. You can follow the answer of the administrator below.
            </p>

            {notice && (
                <div className={`alert ${notice.error ? "alert-danger" : "alert-success"}`} role="alert">
                    {notice.text}
                </div>
            )}

            <form className="card shadow-sm mb-4" onSubmit={submit}>
                <div className="card-body">
                    <div className="btn-group mb-3" role="group" aria-label="Type of report">
                        <button
                            type="button"
                            className={`btn ${form.type === "COMPLAINT" ? "btn-primary" : "btn-outline-primary"}`}
                            onClick={() => changeType("COMPLAINT")}
                        >
                            Complaint
                        </button>
                        <button
                            type="button"
                            className={`btn ${form.type === "DAMAGE" ? "btn-primary" : "btn-outline-primary"}`}
                            onClick={() => changeType("DAMAGE")}
                        >
                            Damage report
                        </button>
                    </div>

                    <div className="row g-3">
                        <div className="col-12 col-md-6">
                            <label htmlFor="rep-category" className="form-label">Category</label>
                            <select
                                id="rep-category"
                                className="form-select"
                                value={form.category}
                                onChange={change("category")}
                            >
                                {CATEGORIES[form.type].map(([value, label]) => (
                                    <option key={value} value={value}>{label}</option>
                                ))}
                            </select>
                        </div>
                        <div className="col-12 col-md-6">
                            <label htmlFor="rep-date" className="form-label">Date it happened (optional)</label>
                            <input
                                id="rep-date"
                                type="date"
                                className="form-control"
                                max={today()}
                                value={form.incidentDate}
                                onChange={change("incidentDate")}
                            />
                        </div>

                        {form.type === "DAMAGE" && (
                            <>
                                <div className="col-12 col-md-6">
                                    <label htmlFor="rep-vehicle" className="form-label">My vehicle (optional)</label>
                                    <select
                                        id="rep-vehicle"
                                        className="form-select"
                                        value={form.vehicleId}
                                        onChange={change("vehicleId")}
                                    >
                                        <option value="">None</option>
                                        {(vehicles.data ?? []).map((v) => (
                                            <option key={v.id} value={v.id}>
                                                {v.vehicleNumber} ({v.vehicleType})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-12 col-md-6">
                                    <label htmlFor="rep-slot" className="form-label">Parking slot (optional)</label>
                                    <select
                                        id="rep-slot"
                                        className="form-select"
                                        value={form.slotId}
                                        onChange={change("slotId")}
                                    >
                                        <option value="">None</option>
                                        {(slots.data ?? []).map((s) => (
                                            <option key={s.id} value={s.id}>
                                                {s.parkingLocation?.name ? `${s.parkingLocation.name} - ` : ""}
                                                {s.slotNumber}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </>
                        )}

                        <div className="col-12">
                            <label htmlFor="rep-subject" className="form-label">Subject</label>
                            <input
                                id="rep-subject"
                                className="form-control"
                                maxLength="150"
                                value={form.subject}
                                onChange={change("subject")}
                            />
                        </div>
                        <div className="col-12">
                            <label htmlFor="rep-description" className="form-label">What happened?</label>
                            <textarea
                                id="rep-description"
                                className="form-control"
                                rows="4"
                                maxLength="2000"
                                value={form.description}
                                onChange={change("description")}
                            />
                            <div className="form-text">{form.description.length} / 2000</div>
                        </div>
                    </div>

                    <button type="submit" className="btn btn-primary mt-3" disabled={sending}>
                        {sending ? "Sending..." : "Send report"}
                    </button>
                </div>
            </form>

            <h4>My reports</h4>
            {reports.error && <div className="alert alert-danger" role="alert">{reports.error}</div>}
            {reports.loading && items.length === 0 && <p>Loading...</p>}
            {!reports.loading && items.length === 0 && !reports.error && (
                <div className="alert alert-warning text-center">You have not sent any report yet.</div>
            )}

            <div className="row g-3">
                {items.map((r) => (
                    <div className="col-12 col-lg-6" key={r.id}>
                        <div className="card shadow-sm h-100">
                            <div className="card-body">
                                <div className="d-flex justify-content-between align-items-start">
                                    <h5 className="card-title">
                                        #{r.id} {r.subject}
                                    </h5>
                                    <span className={`badge ${statusBadge(r.status)}`}>
                                        {STATUS_LABEL[r.status] ?? r.status}
                                    </span>
                                </div>
                                <p className="text-muted small mb-2">
                                    {r.type === "COMPLAINT" ? "Complaint" : "Damage report"} |{" "}
                                    {categoryLabel(r.type, r.category)} | sent {formatDate(r.createdAt)}
                                </p>
                                {r.assignedToName && (
                                    <p className="small mb-2">Handled by: {r.assignedToName}</p>
                                )}
                                {r.adminResponse ? (
                                    <div className="alert alert-info py-2 mb-2">
                                        <strong>Answer:</strong> {r.adminResponse}
                                    </div>
                                ) : (
                                    <p className="small text-muted mb-2">No answer yet.</p>
                                )}
                                <button
                                    type="button"
                                    className="btn btn-link btn-sm p-0"
                                    aria-expanded={openId === r.id}
                                    onClick={() => setOpenId(openId === r.id ? null : r.id)}
                                >
                                    {openId === r.id ? "Hide details" : "Show details"}
                                </button>
                                {openId === r.id && (
                                    <div className="small mt-2">
                                        <p className="mb-1">{r.description}</p>
                                        {r.vehicle && <p className="mb-1">Vehicle: {r.vehicle.vehicleNumber}</p>}
                                        {r.parkingSlot && <p className="mb-1">Slot: {r.parkingSlot.slotNumber}</p>}
                                        {r.incidentDate && <p className="mb-1">Date it happened: {r.incidentDate}</p>}
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default ServiceReports;
