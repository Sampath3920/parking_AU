import { useState } from "react";
import api from "../services/api";
import useApi from "../hooks/useApi";

const AUDIENCES = [
    ["ALL", "Everyone"],
    ["STUDENT", "Students"],
    ["FACULTY", "Faculty"],
    ["STAFF", "Staff"],
    ["VISITOR", "Visitors"],
    ["GUARD", "Security guards"]
];

const audienceLabel = (value) => AUDIENCES.find(([code]) => code === value)?.[1] ?? value;
const formatDate = (value) => (value ? new Date(value).toLocaleString() : "-");

function AdminAnnouncements() {
    const [title, setTitle] = useState("");
    const [message, setMessage] = useState("");
    const [audience, setAudience] = useState("ALL");
    const [sending, setSending] = useState(false);
    const [notice, setNotice] = useState(null); // { text, error }

    const history = useApi("/admin/announcements");

    const send = async (event) => {
        event.preventDefault();

        if (!title.trim() || !message.trim()) {
            setNotice({ text: "Please write a title and a message", error: true });
            return;
        }
        if (!window.confirm(`Send this announcement to: ${audienceLabel(audience)}?`)) {
            return;
        }

        setSending(true);
        try {
            const response = await api.post("/admin/announcements", {
                title: title.trim(),
                message: message.trim(),
                audience
            });
            setNotice({
                text: `Announcement sent to ${response.data.recipientCount} user(s)`,
                error: false
            });
            setTitle("");
            setMessage("");
            history.reload();
        } catch (error) {
            setNotice({
                text: error.response?.data?.message || "Could not send the announcement",
                error: true
            });
        } finally {
            setSending(false);
        }
    };

    const items = history.data ?? [];

    return (
        <div className="container py-4">
            <h2>Announcements</h2>
            <p className="text-muted">
                Each person in the chosen group gets the announcement in their Notifications page.
                Only active accounts receive it.
            </p>

            {notice && (
                <div className={`alert ${notice.error ? "alert-danger" : "alert-success"}`} role="alert">
                    {notice.text}
                </div>
            )}

            <form className="card shadow-sm mb-4" onSubmit={send}>
                <div className="card-body">
                    <div className="row g-3">
                        <div className="col-12 col-md-8">
                            <label htmlFor="ann-title" className="form-label">Title</label>
                            <input
                                id="ann-title"
                                className="form-control"
                                maxLength="150"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                            />
                        </div>
                        <div className="col-12 col-md-4">
                            <label htmlFor="ann-audience" className="form-label">Send to</label>
                            <select
                                id="ann-audience"
                                className="form-select"
                                value={audience}
                                onChange={(e) => setAudience(e.target.value)}
                            >
                                {AUDIENCES.map(([code, label]) => (
                                    <option key={code} value={code}>{label}</option>
                                ))}
                            </select>
                        </div>
                        <div className="col-12">
                            <label htmlFor="ann-message" className="form-label">Message</label>
                            <textarea
                                id="ann-message"
                                className="form-control"
                                rows="4"
                                maxLength="1500"
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                            />
                            <div className="form-text">{message.length} / 1500</div>
                        </div>
                    </div>
                    <button type="submit" className="btn btn-primary mt-3" disabled={sending}>
                        {sending ? "Sending..." : "Send announcement"}
                    </button>
                </div>
            </form>

            <h4>Sent announcements</h4>
            {history.error && <div className="alert alert-danger" role="alert">{history.error}</div>}
            <div className="table-responsive">
                <table className="table table-bordered align-middle">
                    <caption className="visually-hidden">Sent announcements</caption>
                    <thead>
                        <tr>
                            <th scope="col">Date</th>
                            <th scope="col">Title</th>
                            <th scope="col">Message</th>
                            <th scope="col">Sent to</th>
                            <th scope="col">Users reached</th>
                            <th scope="col">By</th>
                        </tr>
                    </thead>
                    <tbody>
                        {history.loading && items.length === 0 && (
                            <tr><td colSpan="6">Loading...</td></tr>
                        )}
                        {!history.loading && items.length === 0 && (
                            <tr><td colSpan="6" className="text-center text-muted">No announcements yet.</td></tr>
                        )}
                        {items.map((a) => (
                            <tr key={a.id}>
                                <td>{formatDate(a.createdAt)}</td>
                                <td>{a.title}</td>
                                <td style={{ maxWidth: "24rem" }}>{a.message}</td>
                                <td>{audienceLabel(a.audience)}</td>
                                <td>{a.recipientCount}</td>
                                <td>{a.createdBy}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export default AdminAnnouncements;
