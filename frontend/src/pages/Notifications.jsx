import { useState } from "react";
import api from "../services/api";
import useApi from "../hooks/useApi";
import { getUser } from "../services/auth";

/** Tells the menu bar to ask the server for the unread number again. */
function announceChange() {
    window.dispatchEvent(new Event("notifications-changed"));
}

function humanize(type) {
    if (!type) return "Notification";
    const text = type.toLowerCase().replace(/_/g, " ");
    return text.charAt(0).toUpperCase() + text.slice(1);
}

function typeBadge(type) {
    if (type === "ANNOUNCEMENT") return "bg-info text-dark";
    if (type && type.startsWith("PAYMENT")) return "bg-success";
    if (type && type.startsWith("BOOKING")) return "bg-primary";
    if (type && type.startsWith("VEHICLE")) return "bg-warning text-dark";
    return "bg-secondary";
}

function Notifications() {
    const userId = getUser()?.id;
    const [filter, setFilter] = useState("ALL"); // ALL | UNREAD
    const [notice, setNotice] = useState(null); // { text, error }

    const list = useApi(`/notifications/user/${userId}`);

    const show = (text, error = false) => setNotice({ text, error });

    const all = list.data ?? [];
    const unreadCount = all.filter((n) => !n.read).length;
    const visible = filter === "UNREAD" ? all.filter((n) => !n.read) : all;

    const markAsRead = async (id) => {
        try {
            await api.put(`/notifications/read/${id}`);
            setNotice(null);
        } catch (error) {
            show(error.response?.data?.message || "Failed to update notification", true);
        } finally {
            list.reload();
            announceChange();
        }
    };

    const markAllAsRead = async () => {
        try {
            const response = await api.put("/notifications/read-all");
            show(`${response.data.updated} notification(s) marked as read`);
        } catch (error) {
            show(error.response?.data?.message || "Failed to update notifications", true);
        } finally {
            list.reload();
            announceChange();
        }
    };

    const deleteNotification = async (id) => {
        if (!window.confirm("Delete this notification?")) {
            return;
        }
        try {
            await api.delete(`/notifications/${id}`);
            show("Notification deleted");
        } catch (error) {
            show(error.response?.data?.message || "Failed to delete notification", true);
        } finally {
            list.reload();
            announceChange();
        }
    };

    return (
        <div className="container mt-4 mb-5">
            <div className="d-flex justify-content-between align-items-center flex-wrap mb-3">
                <h2 className="mb-0">
                    Notifications{" "}
                    {unreadCount > 0 && (
                        <span className="badge bg-primary fs-6" aria-label={`${unreadCount} unread`}>
                            {unreadCount} new
                        </span>
                    )}
                </h2>
                <div className="d-flex gap-2 mt-2">
                    <div className="btn-group" role="group" aria-label="Show notifications">
                        <button
                            type="button"
                            className={`btn btn-sm ${filter === "ALL" ? "btn-primary" : "btn-outline-primary"}`}
                            onClick={() => setFilter("ALL")}
                        >
                            All
                        </button>
                        <button
                            type="button"
                            className={`btn btn-sm ${filter === "UNREAD" ? "btn-primary" : "btn-outline-primary"}`}
                            onClick={() => setFilter("UNREAD")}
                        >
                            Unread
                        </button>
                    </div>
                    <button
                        type="button"
                        className="btn btn-sm btn-outline-success"
                        disabled={unreadCount === 0}
                        onClick={markAllAsRead}
                    >
                        Mark all as read
                    </button>
                </div>
            </div>

            {notice && (
                <div className={`alert ${notice.error ? "alert-danger" : "alert-info"}`} role="alert">
                    {notice.text}
                </div>
            )}
            {list.error && <div className="alert alert-danger" role="alert">{list.error}</div>}
            {list.loading && all.length === 0 && <p>Loading...</p>}

            {!list.loading && visible.length === 0 && !list.error && (
                <div className="alert alert-warning text-center">
                    {filter === "UNREAD" ? "No unread notifications." : "No notifications found."}
                </div>
            )}

            <div className="row g-3">
                {visible.map((notification) => (
                    <div className="col-md-6" key={notification.id}>
                        <div
                            className={
                                notification.read ? "card shadow-sm h-100" : "card shadow border-primary h-100"
                            }
                        >
                            <div className="card-body">
                                <div className="d-flex justify-content-between align-items-start">
                                    <div>
                                        <h5 className="card-title">
                                            {notification.title || humanize(notification.type)}
                                        </h5>
                                        <span className={`badge mb-2 ${typeBadge(notification.type)}`}>
                                            {humanize(notification.type)}
                                        </span>
                                    </div>
                                    {!notification.read && <span className="badge bg-primary">NEW</span>}
                                </div>
                                <p className="card-text">{notification.message}</p>
                                <p className="text-muted small mb-3">
                                    {notification.createdAt
                                        ? new Date(notification.createdAt).toLocaleString()
                                        : ""}
                                </p>
                                <div className="d-flex gap-2 flex-wrap">
                                    {!notification.read && (
                                        <button
                                            type="button"
                                            className="btn btn-success btn-sm"
                                            onClick={() => markAsRead(notification.id)}
                                        >
                                            Mark as Read
                                        </button>
                                    )}
                                    <button
                                        type="button"
                                        className="btn btn-outline-danger btn-sm"
                                        onClick={() => deleteNotification(notification.id)}
                                    >
                                        Delete
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default Notifications;
