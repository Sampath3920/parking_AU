import { Link } from "react-router-dom";
import useApi from "../hooks/useApi";
import { getUser, logout } from "../services/auth";

const money = (value) => `₹${Number(value ?? 0).toFixed(2)}`;
const formatDate = (value) => (value ? new Date(value).toLocaleString() : "-");

/** A small number card that opens the matching page. */
function Stat({ title, value, hint, color, to }) {
    return (
        <div className="col-6 col-lg-3">
            <Link to={to} className="text-decoration-none" aria-label={`${title}: ${value}. Open`}>
                <div className={`card border-${color} shadow-sm h-100`}>
                    <div className="card-body">
                        <h6 className="text-muted small mb-1">{title}</h6>
                        <h3 className={`text-${color} mb-0`}>{value}</h3>
                        {hint && <div className="small text-muted">{hint}</div>}
                    </div>
                </div>
            </Link>
        </div>
    );
}

const SHORTCUTS = [
    ["/vehicles", "My Vehicles", "Add and manage your registered vehicles."],
    ["/parking", "Parking", "View available parking slots and book a slot."],
    ["/history", "Parking History", "View your previous parking records."],
    ["/qr", "QR Parking", "Manage QR check-in and check-out."],
    ["/payments", "Payments", "View your parking payment information."],
    ["/notifications", "Notifications", "View parking notifications and updates."],
    ["/parking-pass", "Parking Pass", "Create and view your parking passes."],
    ["/parking-permission", "Parking Permission", "Request permission to park a vehicle."],
    ["/reports", "Complaints", "Report a complaint or damage and read the answer."],
    ["/help", "Help", "Instructions for booking, QR codes and payments."]
];

function StudentDashboard() {
    const { data, error } = useApi("/me/summary");
    const active = data?.activeBooking;

    return (
        <div className="container mt-4 mb-5">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2>Welcome, {getUser()?.name}</h2>
                <button className="btn btn-danger" onClick={logout}>
                    Logout
                </button>
            </div>

            {error && <div className="alert alert-danger" role="alert">{error}</div>}

            {active && (
                <div className="alert alert-success" role="status">
                    <strong>
                        {active.checkedIn ? "You are parked now" : "Your next booking"}:
                    </strong>{" "}
                    #{active.id}, slot {active.slotNumber}
                    {active.locationName ? ` (${active.locationName})` : ""}, vehicle {active.vehicleNumber},{" "}
                    {formatDate(active.startTime)} to {formatDate(active.endTime)}.{" "}
                    <Link to="/qr">Open my QR code</Link>
                </div>
            )}
            {data && !active && (
                <div className="alert alert-light border" role="status">
                    You have no approved booking right now. <Link to="/parking">Book a slot</Link>
                </div>
            )}

            {data && (
                <div className="row g-3 mb-4">
                    <Stat
                        title="My vehicles"
                        value={`${data.vehicles.total} / ${data.vehicles.limit}`}
                        hint={`${data.vehicles.approved} approved, ${data.vehicles.pending} waiting`}
                        color="info"
                        to="/vehicles"
                    />
                    <Stat
                        title="Pending booking requests"
                        value={data.bookings.pending}
                        hint="waiting for the administrator"
                        color="warning"
                        to="/history"
                    />
                    <Stat
                        title="Available parking slots"
                        value={data.availableSlots}
                        hint="free right now"
                        color="success"
                        to="/parking"
                    />
                    <Stat
                        title="Parking history"
                        value={data.bookings.total}
                        hint={`${data.bookings.completed} completed`}
                        color="primary"
                        to="/history"
                    />
                    <Stat
                        title="Payments"
                        value={money(data.payments.totalPaid)}
                        hint={`${data.payments.pending} waiting for verification`}
                        color="success"
                        to="/payments"
                    />
                    <Stat
                        title="Notifications"
                        value={data.unreadNotifications}
                        hint="unread"
                        color={data.unreadNotifications > 0 ? "danger" : "secondary"}
                        to="/notifications"
                    />
                    <Stat
                        title="My complaints and reports"
                        value={data.reports.total}
                        hint={`${data.reports.unresolved} not resolved`}
                        color="secondary"
                        to="/reports"
                    />
                </div>
            )}

            <div className="row g-4">
                {SHORTCUTS.map(([to, title, text]) => (
                    <div className="col-md-4" key={to}>
                        <div className="card shadow h-100">
                            <div className="card-body">
                                <h5>{title}</h5>
                                <p>{text}</p>
                                <Link to={to} className="btn btn-primary">
                                    Open
                                </Link>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default StudentDashboard;
