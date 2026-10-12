import { useEffect, useState } from "react";
import api from "../services/api";
import { getUser } from "../services/auth";

/**
 * Two views in one page:
 *  - Students / faculty / staff / visitors: pick an active booking and show its QR code.
 *  - Security guards / admins: type or scan the QR text, verify it, then check in or check out.
 */
function QR() {
    const user = getUser();
    const isStaff = user?.role === "GUARD" || user?.role === "ADMIN";

    const [message, setMessage] = useState("");
    const [error, setError] = useState(false);

    // user view
    const [bookings, setBookings] = useState([]);
    const [selectedBooking, setSelectedBooking] = useState("");
    const [qrData, setQrData] = useState(null);

    // staff view
    const [scanText, setScanText] = useState("");
    const [scanResult, setScanResult] = useState(null);

    const show = (text, isError = false) => {
        setMessage(text);
        setError(isError);
    };

    useEffect(() => {
        if (isStaff || !user?.id) {
            return undefined;
        }
        let cancelled = false;
        api.get(`/bookings/user/${user.id}`)
            .then((response) => {
                if (!cancelled) {
                    setBookings(response.data.filter((booking) => booking.status === "BOOKED"));
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    show(
                        err.response?.data?.message || "Failed to load bookings",
                        true
                    );
                }
            });
        return () => {
            cancelled = true;
        };
    }, [isStaff, user?.id]);

    const generateQR = async () => {
        if (!selectedBooking) {
            show("Please select a booking", true);
            return;
        }

        try {
            const response = await api.get(`/qr/generate/${selectedBooking}`);
            setQrData(response.data);
            show("QR code is ready. Show it to the security guard at the gate.");
        } catch (err) {
            setQrData(null);
            show(err.response?.data?.message || "Failed to generate QR", true);
        }
    };

    const verifyQR = async () => {
        if (!scanText.trim()) {
            show("Scan or type the QR text first", true);
            return;
        }

        try {
            const response = await api.post("/qr/scan", { qrData: scanText });
            setScanResult(response.data);
            show(response.data.message);
        } catch (err) {
            setScanResult(null);
            show(err.response?.data?.message || "QR verification failed", true);
        }
    };

    const runSessionAction = async (action) => {
        if (!scanResult) {
            show("Verify a QR code first", true);
            return;
        }

        try {
            const response = await api.post(
                `/parking-sessions/${action}/${scanResult.bookingId}`
            );
            show(response.data.message || "Done");

            if (action === "check-out") {
                setScanResult(null);
                setScanText("");
            }
        } catch (err) {
            show(err.response?.data?.message || "Action failed", true);
        }
    };

    return (
        <div className="container mt-4">
            <h2 className="text-center mb-4">QR Parking</h2>

            {message && (
                <div
                    className={`alert ${error ? "alert-danger" : "alert-info"}`}
                    role="alert"
                >
                    {message}
                </div>
            )}

            {!isStaff && (
                <div className="card shadow">
                    <div className="card-body">
                        <p className="text-muted">
                            Choose an active booking to get your entry QR code.
                        </p>

                        <label htmlFor="bookingSelect" className="form-label">
                            Active booking
                        </label>
                        <select
                            id="bookingSelect"
                            className="form-select mb-3"
                            value={selectedBooking}
                            onChange={(e) => {
                                setSelectedBooking(e.target.value);
                                setQrData(null);
                            }}
                        >
                            <option value="">Select booking</option>
                            {bookings.map((booking) => (
                                <option key={booking.id} value={booking.id}>
                                    Booking #{booking.id} - {booking.vehicle?.vehicleNumber} - Slot{" "}
                                    {booking.parkingSlot?.slotNumber}
                                    {booking.startTime
                                        ? ` - ${new Date(booking.startTime).toLocaleString()}`
                                        : ""}
                                </option>
                            ))}
                        </select>

                        {bookings.length === 0 && (
                            <p className="text-muted">
                                You have no active booking. Book a slot first.
                            </p>
                        )}

                        <button
                            type="button"
                            className="btn btn-primary"
                            onClick={generateQR}
                        >
                            Generate QR
                        </button>

                        {qrData && qrData.qrCode && (
                            <div className="text-center mt-4">
                                <img
                                    src={`data:image/png;base64,${qrData.qrCode}`}
                                    alt={`QR code for booking ${qrData.bookingId}`}
                                    width="260"
                                    height="260"
                                />
                                <p className="mt-2">Booking #{qrData.bookingId}</p>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {isStaff && (
                <div className="card shadow">
                    <div className="card-body">
                        <p className="text-muted">
                            Scan the visitor&apos;s QR code with a scanner (it types the text
                            here), or paste the text, then press Verify.
                        </p>

                        <label htmlFor="scanText" className="form-label">
                            QR text
                        </label>
                        <input
                            id="scanText"
                            className="form-control mb-3"
                            placeholder="PARKING:xxxxxxxx-xxxx-..."
                            value={scanText}
                            onChange={(e) => {
                                setScanText(e.target.value);
                                setScanResult(null);
                            }}
                        />

                        <button
                            type="button"
                            className="btn btn-primary me-2"
                            onClick={verifyQR}
                        >
                            Verify QR
                        </button>

                        <button
                            type="button"
                            className="btn btn-success me-2"
                            onClick={() => runSessionAction("check-in")}
                            disabled={!scanResult}
                        >
                            Check In
                        </button>

                        <button
                            type="button"
                            className="btn btn-warning"
                            onClick={() => runSessionAction("check-out")}
                            disabled={!scanResult}
                        >
                            Check Out
                        </button>

                        {scanResult && (
                            <p className="mt-3 mb-0">
                                Booking #{scanResult.bookingId} is{" "}
                                <strong>{scanResult.status}</strong>.
                            </p>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

export default QR;
