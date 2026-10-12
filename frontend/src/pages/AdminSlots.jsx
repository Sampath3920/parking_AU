import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../services/api";

const PAGE_SIZE = 10;
const EMPTY_FORM = { slotNumber: "", vehicleType: "4-WHEELER", floor: "", status: "AVAILABLE", locationId: "" };

function statusLabel(status) {
    if (status === "BOOKED") return "RESERVED";
    return status;
}

function badgeClass(status) {
    if (status === "AVAILABLE") return "bg-success";
    if (status === "BOOKED") return "bg-warning text-dark";
    if (status === "MAINTENANCE") return "bg-info text-dark";
    return "bg-danger";
}

function AdminSlots() {
    const [slots, setSlots] = useState([]);
    const [locations, setLocations] = useState([]);
    const [summary, setSummary] = useState(null);
    // The dashboard cards open this page as /admin/slots?status=AVAILABLE (or BOOKED, MAINTENANCE, OUT_OF_SERVICE).
    const [params] = useSearchParams();
    const [filters, setFilters] = useState({
        locationId: "",
        vehicleType: "",
        status: params.get("status") ?? "",
        search: ""
    });
    const [form, setForm] = useState(EMPTY_FORM);
    const [editingId, setEditingId] = useState(null);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    const [isError, setIsError] = useState(false);

    const show = (text, error = false) => {
        setMessage(text);
        setIsError(error);
    };

    // The loading flag is switched on by the handlers that change a filter (not inside this
    // function), so that the effect below does not set state synchronously.
    const load = async () => {
        try {
            const params = {};
            Object.entries(filters).forEach(([key, value]) => {
                if (value) params[key] = value;
            });
            const [slotResponse, summaryResponse, locationResponse] = await Promise.all([
                api.get("/parking-slots", { params }),
                api.get("/parking-slots/summary", {
                    params: filters.locationId ? { locationId: filters.locationId } : {}
                }),
                api.get("/parking-locations")
            ]);
            setSlots(slotResponse.data);
            setSummary(summaryResponse.data);
            setLocations(locationResponse.data);
            setPage(0);
            setForm((current) =>
                current.locationId || locationResponse.data.length === 0
                    ? current
                    : { ...current, locationId: String(locationResponse.data[0].id) }
            );
        } catch (error) {
            show(error.response?.data?.message || "Failed to load slots", true);
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
    }, [filters.locationId, filters.vehicleType, filters.status]);

    const changeForm = (field) => (e) => setForm({ ...form, [field]: e.target.value });

    const resetForm = () => {
        setEditingId(null);
        setForm({ ...EMPTY_FORM, locationId: form.locationId });
    };

    const save = async () => {
        if (!form.locationId) {
            show("Please choose a parking location", true);
            return;
        }
        if (!form.slotNumber.trim()) {
            show("Please enter the slot number", true);
            return;
        }

        const body = {
            slotNumber: form.slotNumber.trim(),
            vehicleType: form.vehicleType,
            floor: form.floor.trim(),
            status: form.status
        };

        try {
            if (editingId) {
                await api.put(`/parking-slots/${editingId}`, {
                    ...body,
                    parkingLocation: { id: Number(form.locationId) }
                });
                show("Slot updated");
            } else {
                await api.post(`/parking-slots/add/${form.locationId}`, body);
                show("Slot added");
            }
            resetForm();
            await load();
        } catch (error) {
            show(error.response?.data?.message || "Could not save the slot", true);
        }
    };

    const startEdit = (slot) => {
        setEditingId(slot.id);
        setForm({
            slotNumber: slot.slotNumber || "",
            vehicleType: slot.vehicleType || "4-WHEELER",
            floor: slot.floor || "",
            status: slot.status === "BOOKED" ? "AVAILABLE" : slot.status,
            locationId: String(slot.parkingLocation?.id || "")
        });
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    const quickStatus = async (slot, status) => {
        try {
            await api.put(`/parking-slots/${slot.id}`, {
                slotNumber: slot.slotNumber,
                vehicleType: slot.vehicleType,
                floor: slot.floor || "",
                status,
                parkingLocation: { id: slot.parkingLocation?.id }
            });
            show(`Slot ${slot.slotNumber} is now ${statusLabel(status)}`);
            await load();
        } catch (error) {
            show(error.response?.data?.message || "Could not change the status", true);
        }
    };

    const remove = async (slot) => {
        if (!window.confirm(`Delete slot ${slot.slotNumber}?`)) {
            return;
        }
        try {
            await api.delete(`/parking-slots/${slot.id}`);
            show("Slot deleted");
            await load();
        } catch (error) {
            show(error.response?.data?.message || "Could not delete the slot", true);
        }
    };

    const pageCount = Math.max(1, Math.ceil(slots.length / PAGE_SIZE));
    const rows = slots.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

    const cards = summary
        ? [
              ["Total", summary.total],
              ["Available", summary.available],
              ["Reserved", summary.reserved],
              ["Occupied now", summary.occupied],
              ["Maintenance", summary.maintenance],
              ["Out of service", summary.outOfService]
          ]
        : [];

    return (
        <div className="container mt-4">
            <h2 className="mb-3">Manage Parking Slots</h2>

            {message && (
                <div className={`alert ${isError ? "alert-danger" : "alert-success"}`} role="alert">
                    {message}
                </div>
            )}

            <div className="row g-2 mb-4">
                {cards.map(([label, value]) => (
                    <div className="col-6 col-md-2" key={label}>
                        <div className="card text-center shadow-sm">
                            <div className="card-body py-2">
                                <div className="small text-muted">{label}</div>
                                <div className="fs-4 fw-bold">{value}</div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            <div className="card shadow-sm mb-4">
                <div className="card-body">
                    <h5>{editingId ? "Edit slot" : "Add slot"}</h5>
                    <div className="row g-2">
                        <div className="col-md-3">
                            <label htmlFor="slotLocation" className="form-label">Location</label>
                            <select
                                id="slotLocation"
                                className="form-select"
                                value={form.locationId}
                                onChange={changeForm("locationId")}
                            >
                                <option value="">Select</option>
                                {locations.map((l) => (
                                    <option key={l.id} value={l.id}>{l.name}</option>
                                ))}
                            </select>
                        </div>
                        <div className="col-md-2">
                            <label htmlFor="slotNumber" className="form-label">Slot number</label>
                            <input
                                id="slotNumber"
                                className="form-control"
                                maxLength="20"
                                value={form.slotNumber}
                                onChange={changeForm("slotNumber")}
                            />
                        </div>
                        <div className="col-md-2">
                            <label htmlFor="slotType" className="form-label">Vehicle type</label>
                            <select
                                id="slotType"
                                className="form-select"
                                value={form.vehicleType}
                                onChange={changeForm("vehicleType")}
                            >
                                <option value="4-WHEELER">4-WHEELER</option>
                                <option value="2-WHEELER">2-WHEELER</option>
                            </select>
                        </div>
                        <div className="col-md-2">
                            <label htmlFor="slotFloor" className="form-label">Floor</label>
                            <input
                                id="slotFloor"
                                className="form-control"
                                maxLength="20"
                                value={form.floor}
                                onChange={changeForm("floor")}
                            />
                        </div>
                        <div className="col-md-3">
                            <label htmlFor="slotStatus" className="form-label">Status</label>
                            <select
                                id="slotStatus"
                                className="form-select"
                                value={form.status}
                                onChange={changeForm("status")}
                            >
                                <option value="AVAILABLE">Available (enabled)</option>
                                <option value="MAINTENANCE">Under maintenance</option>
                                <option value="OUT_OF_SERVICE">Out of service (disabled)</option>
                            </select>
                        </div>
                    </div>
                    <div className="mt-3">
                        <button type="button" className="btn btn-primary me-2" onClick={save}>
                            {editingId ? "Save changes" : "Add slot"}
                        </button>
                        {editingId && (
                            <button type="button" className="btn btn-secondary" onClick={resetForm}>
                                Cancel edit
                            </button>
                        )}
                    </div>
                    <p className="text-muted small mt-2 mb-0">
                        Reserved is set automatically while a request or approved booking holds the slot.
                        Slot numbers must be different inside one location.
                    </p>
                </div>
            </div>

            <div className="row g-2 mb-3">
                <div className="col-md-3">
                    <label htmlFor="filterLocation" className="form-label">Location</label>
                    <select
                        id="filterLocation"
                        className="form-select"
                        value={filters.locationId}
                        onChange={(e) => {
                            setLoading(true);
                            setFilters({ ...filters, locationId: e.target.value });
                        }}
                    >
                        <option value="">All</option>
                        {locations.map((l) => (
                            <option key={l.id} value={l.id}>{l.name}</option>
                        ))}
                    </select>
                </div>
                <div className="col-md-3">
                    <label htmlFor="filterType" className="form-label">Vehicle type</label>
                    <select
                        id="filterType"
                        className="form-select"
                        value={filters.vehicleType}
                        onChange={(e) => {
                            setLoading(true);
                            setFilters({ ...filters, vehicleType: e.target.value });
                        }}
                    >
                        <option value="">All</option>
                        <option value="2-WHEELER">2-WHEELER</option>
                        <option value="4-WHEELER">4-WHEELER</option>
                    </select>
                </div>
                <div className="col-md-3">
                    <label htmlFor="filterStatus" className="form-label">Status</label>
                    <select
                        id="filterStatus"
                        className="form-select"
                        value={filters.status}
                        onChange={(e) => {
                            setLoading(true);
                            setFilters({ ...filters, status: e.target.value });
                        }}
                    >
                        <option value="">All</option>
                        <option value="AVAILABLE">Available</option>
                        <option value="BOOKED">Reserved</option>
                        <option value="MAINTENANCE">Maintenance</option>
                        <option value="OUT_OF_SERVICE">Out of service</option>
                    </select>
                </div>
                <div className="col-md-3">
                    <label htmlFor="filterSearch" className="form-label">Slot number</label>
                    <input
                        id="filterSearch"
                        className="form-control"
                        value={filters.search}
                        onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") {
                                setLoading(true);
                                load();
                            }
                        }}
                    />
                </div>
            </div>

            {loading ? (
                <p>Loading...</p>
            ) : rows.length === 0 ? (
                <p className="text-muted">No slots found.</p>
            ) : (
                <div className="table-responsive">
                    <table className="table table-bordered table-hover align-middle">
                        <thead>
                            <tr>
                                <th>Slot</th>
                                <th>Location</th>
                                <th>Floor</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map((s) => (
                                <tr key={s.id}>
                                    <td>{s.slotNumber}</td>
                                    <td>{s.parkingLocation?.name}</td>
                                    <td>{s.floor || "-"}</td>
                                    <td>{s.vehicleType}</td>
                                    <td>
                                        <span className={`badge ${badgeClass(s.status)}`}>
                                            {statusLabel(s.status)}
                                        </span>
                                    </td>
                                    <td>
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-primary me-1 mb-1"
                                            onClick={() => startEdit(s)}
                                        >
                                            Edit
                                        </button>
                                        {(s.status === "AVAILABLE" || s.status === "BOOKED") ? (
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-warning me-1 mb-1"
                                                onClick={() => quickStatus(s, "OUT_OF_SERVICE")}
                                            >
                                                Disable
                                            </button>
                                        ) : (
                                            <button
                                                type="button"
                                                className="btn btn-sm btn-success me-1 mb-1"
                                                onClick={() => quickStatus(s, "AVAILABLE")}
                                            >
                                                Enable
                                            </button>
                                        )}
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-danger mb-1"
                                            onClick={() => remove(s)}
                                        >
                                            Delete
                                        </button>
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
                <span>Page {page + 1} of {pageCount} ({slots.length} slots)</span>
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

export default AdminSlots;
