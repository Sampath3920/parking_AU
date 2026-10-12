import { useState } from "react";
import { Link } from "react-router-dom";
import useApi from "../hooks/useApi";
import { downloadFile } from "../services/download";

const money = (value) => `₹${Number(value ?? 0).toFixed(2)}`;

/** One clickable number. "to" is the page with the detailed list. */
function StatCard({ title, value, color, to }) {
    return (
        <div className="col-6 col-md-4 col-xl-3">
            <Link
                to={to}
                className="text-decoration-none"
                aria-label={`${title}: ${value}. Open the list`}
            >
                <div className={`card border-${color} shadow-sm h-100`}>
                    <div className="card-body">
                        <h6 className="text-muted small mb-1">{title}</h6>
                        <h3 className={`text-${color} mb-0`}>{value}</h3>
                    </div>
                </div>
            </Link>
        </div>
    );
}

function Section({ title, link, children }) {
    return (
        <section className="mb-4">
            <div className="d-flex justify-content-between align-items-baseline">
                <h4 className="mb-3">{title}</h4>
                {link && (
                    <Link to={link.to} className="small">
                        {link.label}
                    </Link>
                )}
            </div>
            <div className="row g-3">{children}</div>
        </section>
    );
}

function AdminDashboard() {
    const { data, loading, error, reload } = useApi("/admin/summary");
    const [message, setMessage] = useState("");

    const download = async (url, fileName) => {
        try {
            await downloadFile(url, fileName);
            setMessage("");
        } catch {
            setMessage("Could not download the report");
        }
    };

    const users = data?.users;
    const vehicles = data?.vehicles;
    const bookings = data?.bookings;
    const slots = data?.slots;
    const payments = data?.payments;
    const reports = data?.reports;

    return (
        <div className="container py-4">
            <div className="d-flex justify-content-between align-items-center flex-wrap mb-3">
                <div>
                    <h2>Admin Dashboard</h2>
                    <p className="text-muted mb-0">
                        Smart College Parking Management System. Click a card to see the list.
                    </p>
                </div>
                <button
                    type="button"
                    className="btn btn-primary mt-2"
                    onClick={reload}
                    disabled={loading}
                >
                    {loading ? "Loading..." : "Refresh Dashboard"}
                </button>
            </div>

            {(error || message) && (
                <div className="alert alert-danger" role="alert">
                    {error || message}
                </div>
            )}
            {loading && !data && <p>Loading dashboard...</p>}

            {data && (
                <>
                    <Section
                        title="Users"
                        link={{ to: "/admin/users", label: "Manage users" }}
                    >
                        <StatCard title="Registered users" value={users.total} color="primary" to="/admin/users" />
                        <StatCard title="Students" value={users.students} color="primary" to="/admin/users?role=STUDENT" />
                        <StatCard title="Faculty" value={users.faculty} color="primary" to="/admin/users?role=FACULTY" />
                        <StatCard title="Staff" value={users.staff} color="primary" to="/admin/users?role=STAFF" />
                        <StatCard title="Visitors" value={users.visitors} color="primary" to="/admin/users?role=VISITOR" />
                        <StatCard title="Security guards" value={users.guards} color="secondary" to="/admin/users?role=GUARD" />
                        <StatCard title="Deactivated accounts" value={users.inactive} color="danger" to="/admin/users?status=INACTIVE" />
                    </Section>

                    <Section
                        title="Vehicles"
                        link={{ to: "/admin/vehicles?status=", label: "All vehicles" }}
                    >
                        <StatCard title="Registered vehicles" value={vehicles.total} color="info" to="/admin/vehicles?status=" />
                        <StatCard title="2-wheelers" value={vehicles.twoWheeler} color="info" to="/admin/vehicles?status=" />
                        <StatCard title="4-wheelers" value={vehicles.fourWheeler} color="info" to="/admin/vehicles?status=" />
                        <StatCard title="Waiting for verification" value={vehicles.pending} color="warning" to="/admin/vehicles?status=PENDING" />
                        <StatCard title="Approved" value={vehicles.approved} color="success" to="/admin/vehicles?status=VERIFIED" />
                        <StatCard title="Rejected" value={vehicles.rejected} color="danger" to="/admin/vehicles?status=REJECTED" />
                    </Section>

                    <Section
                        title="Parking bookings"
                        link={{ to: "/admin/bookings?status=", label: "All bookings" }}
                    >
                        <StatCard title="Pending requests" value={bookings.pending} color="warning" to="/admin/bookings?status=PENDING" />
                        <StatCard title="Approved bookings" value={bookings.approved} color="success" to="/admin/bookings?status=BOOKED" />
                        <StatCard title="Completed" value={bookings.completed} color="success" to="/admin/bookings?status=COMPLETED" />
                        <StatCard title="Rejected" value={bookings.rejected} color="danger" to="/admin/bookings?status=REJECTED" />
                        <StatCard title="Cancelled" value={bookings.cancelled} color="secondary" to="/admin/bookings?status=CANCELLED" />
                        <StatCard title="All bookings" value={bookings.total} color="primary" to="/admin/bookings?status=" />
                    </Section>

                    <Section
                        title="Parking slots"
                        link={{ to: "/admin/slots", label: "Manage slots" }}
                    >
                        <StatCard title="Total slots" value={slots.total} color="dark" to="/admin/slots" />
                        <StatCard title="Available" value={slots.available} color="success" to="/admin/slots?status=AVAILABLE" />
                        <StatCard title="Reserved" value={slots.reserved} color="warning" to="/admin/slots?status=BOOKED" />
                        <StatCard title="Occupied (vehicle parked now)" value={slots.currentlyParked} color="danger" to="/admin/slots" />
                        <StatCard title="Under maintenance" value={slots.maintenance} color="secondary" to="/admin/slots?status=MAINTENANCE" />
                        <StatCard title="Out of service" value={slots.outOfService} color="secondary" to="/admin/slots?status=OUT_OF_SERVICE" />
                    </Section>

                    <Section
                        title="Payments"
                        link={{ to: "/admin/payments", label: "Payment management" }}
                    >
                        <StatCard title="Waiting for verification" value={payments.pending} color="warning" to="/admin/payments?status=PENDING_VERIFICATION" />
                        <StatCard title="Successful" value={payments.successful} color="success" to="/admin/payments?status=SUCCESS" />
                        <StatCard title="Failed" value={payments.failed} color="danger" to="/admin/payments?status=FAILED" />
                        <StatCard title="Refunded" value={payments.refunded} color="secondary" to="/admin/payments?status=REFUNDED" />
                        <StatCard title="Revenue today" value={money(payments.revenueToday)} color="success" to="/admin/payments" />
                        <StatCard title="Revenue this month" value={money(payments.revenueThisMonth)} color="success" to="/admin/payments" />
                        <StatCard title="Total revenue" value={money(payments.revenueTotal)} color="success" to="/admin/payments" />
                    </Section>

                    {reports && (
                        <Section
                            title="Complaints and damage reports"
                            link={{ to: "/admin/reports", label: "Open reports" }}
                        >
                            <StatCard title="Complaints received" value={reports.complaintsTotal} color="primary" to="/admin/reports?type=COMPLAINT" />
                            <StatCard title="Complaints not resolved" value={reports.complaintsUnresolved} color="warning" to="/admin/reports?type=COMPLAINT&status=UNRESOLVED" />
                            <StatCard title="Damage reports received" value={reports.damageTotal} color="primary" to="/admin/reports?type=DAMAGE" />
                            <StatCard title="Damage reports not resolved" value={reports.damageUnresolved} color="danger" to="/admin/reports?type=DAMAGE&status=UNRESOLVED" />
                        </Section>
                    )}
                </>
            )}

            <h4 className="mt-4">Reports (CSV)</h4>
            <div className="d-flex flex-wrap gap-2 mb-4">
                <button
                    type="button"
                    className="btn btn-outline-primary"
                    onClick={() => download("/reports/bookings.csv", "bookings.csv")}
                >
                    Download bookings
                </button>
                <button
                    type="button"
                    className="btn btn-outline-primary"
                    onClick={() => download("/reports/sessions.csv", "sessions.csv")}
                >
                    Download parking sessions
                </button>
                <button
                    type="button"
                    className="btn btn-outline-primary"
                    onClick={() => download("/admin/payments/report.csv", "payments.csv")}
                >
                    Download payments
                </button>
            </div>

            <h4>Quick links</h4>
            <div className="d-flex flex-wrap gap-2">
                <Link className="btn btn-outline-secondary" to="/admin/users">Users and accounts</Link>
                <Link className="btn btn-outline-secondary" to="/admin/vehicles">Vehicle requests</Link>
                <Link className="btn btn-outline-secondary" to="/admin/bookings">Parking requests</Link>
                <Link className="btn btn-outline-secondary" to="/admin/payments">Payments</Link>
                <Link className="btn btn-outline-secondary" to="/admin/slots">Slots</Link>
                <Link className="btn btn-outline-secondary" to="/admin/announcements">Announcements</Link>
            </div>
        </div>
    );
}

export default AdminDashboard;
