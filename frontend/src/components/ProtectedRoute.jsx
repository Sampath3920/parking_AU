import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import api from "../services/api";
import { getUser, homePathForRole, isLoggedIn, logout } from "../services/auth";

const LINKS = [
    { to: "/student-dashboard", label: "Home", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR"] },
    { to: "/vehicles", label: "Vehicles", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR"] },
    { to: "/parking", label: "Parking", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR", "ADMIN", "GUARD"] },
    { to: "/booking", label: "Book", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR"] },
    { to: "/history", label: "History", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR"] },
    { to: "/qr", label: "QR", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR", "GUARD", "ADMIN"] },
    { to: "/payments", label: "Payments", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR"] },
    { to: "/notifications", label: "Notifications", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR", "GUARD", "ADMIN"] },
    { to: "/reports", label: "Complaints", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR", "GUARD"] },
    { to: "/help", label: "Help", roles: ["STUDENT", "FACULTY", "STAFF", "VISITOR", "GUARD"] },
    { to: "/admin-dashboard", label: "Admin", roles: ["ADMIN"] },
    { to: "/admin/vehicles", label: "Vehicle Requests", roles: ["ADMIN"] },
    { to: "/admin/bookings", label: "Parking Requests", roles: ["ADMIN"] },
    { to: "/admin/slots", label: "Slots", roles: ["ADMIN"] },
    { to: "/admin/users", label: "Users", roles: ["ADMIN"] },
    { to: "/admin/payments", label: "Payment Management", roles: ["ADMIN"] },
    { to: "/admin/reports", label: "Complaints", roles: ["ADMIN"] },
    { to: "/admin/announcements", label: "Announcements", roles: ["ADMIN"] }
];

/** Number of unread notifications. Asked again every 30 seconds and when a page changes notifications. */
function useUnreadCount() {
    const [count, setCount] = useState(0);

    useEffect(() => {
        let cancelled = false;
        const refresh = () => {
            api.get("/notifications/unread-count")
                .then((response) => {
                    if (!cancelled) {
                        setCount(response.data.count);
                    }
                })
                .catch(() => {
                    // The badge is only a hint; ignore errors.
                });
        };
        refresh();
        const timer = setInterval(refresh, 30000);
        window.addEventListener("notifications-changed", refresh);
        return () => {
            cancelled = true;
            clearInterval(timer);
            window.removeEventListener("notifications-changed", refresh);
        };
    }, []);

    return count;
}

function NavBar({ user }) {
    const links = LINKS.filter((item) => item.roles.includes(user.role));
    const unread = useUnreadCount();

    return (
        <nav className="navbar navbar-expand-lg navbar-dark bg-dark" aria-label="Main menu">
            <div className="container-fluid">
                <span className="navbar-brand">Smart College Parking</span>

                <button
                    className="navbar-toggler"
                    type="button"
                    data-bs-toggle="collapse"
                    data-bs-target="#mainMenu"
                    aria-controls="mainMenu"
                    aria-expanded="false"
                    aria-label="Show or hide the menu"
                >
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="collapse navbar-collapse" id="mainMenu">
                    <ul className="navbar-nav me-auto mb-2 mb-lg-0 flex-wrap">
                        {links.map((item) => (
                            <li className="nav-item" key={item.to}>
                                <Link className="nav-link" to={item.to}>
                                    {item.label}
                                    {item.to === "/notifications" && unread > 0 && (
                                        <span className="badge rounded-pill bg-danger ms-1">
                                            {unread}
                                            <span className="visually-hidden"> unread notifications</span>
                                        </span>
                                    )}
                                </Link>
                            </li>
                        ))}
                    </ul>

                    <span className="navbar-text me-3">
                        {user.name} ({user.role})
                    </span>

                    <button
                        type="button"
                        className="btn btn-outline-light btn-sm"
                        onClick={logout}
                    >
                        Logout
                    </button>
                </div>
            </div>
        </nav>
    );
}

/**
 * Wrap a page with <ProtectedRoute roles={["ADMIN"]}>...</ProtectedRoute>
 * - not logged in  -> login page
 * - wrong role     -> that person's own home page
 * The real security is in the backend; this only gives a clean experience.
 */
function ProtectedRoute({ roles, children }) {
    if (!isLoggedIn()) {
        return <Navigate to="/" replace />;
    }

    const user = getUser();

    if (roles && !roles.includes(user.role)) {
        return <Navigate to={homePathForRole(user.role)} replace />;
    }

    return (
        <>
            <NavBar user={user} />
            {children}
        </>
    );
}

export default ProtectedRoute;
