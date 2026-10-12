import { useEffect, useState } from "react";
import api from "../services/api";
import { getUser } from "../services/auth";

function Booking() {
    const userId = getUser()?.id;

    const [vehicles, setVehicles] = useState([]);
    const [selectedVehicle, setSelectedVehicle] = useState("");
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [message, setMessage] = useState("");
    const [availableSlots, setAvailableSlots] = useState([]);
    const [reloadKey, setReloadKey] = useState(0);

    const pad = (n) => String(n).padStart(2, "0");
    const toInput = (date) =>
        `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;

    const [startTime, setStartTime] = useState(() => toInput(new Date()));
    const [endTime, setEndTime] = useState(() =>
        toInput(new Date(Date.now() + 2 * 60 * 60 * 1000))
    );

    const loadData = async () => {
        try {
            const vehicleResponse = await api.get(
                `/vehicles/user/${userId}`
            );
            setVehicles(vehicleResponse.data);
        } catch {
            setMessage("Failed to load booking information");
        }
    };

    useEffect(() => {
        let cancelled = false;
        api.get(`/vehicles/user/${userId}`)
            .then((response) => {
                if (!cancelled) {
                    setVehicles(response.data);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setMessage("Failed to load booking information");
                }
            });
        return () => {
            cancelled = true;
        };
    }, [userId]);

    // Only vehicles approved by the admin can be used for booking
    const approvedVehicles = vehicles.filter(
        (vehicle) => vehicle.verificationStatus === "VERIFIED"
    );

    const handleVehicleChange = (e) => {
        const vehicleId = e.target.value;

        setSelectedVehicle(vehicleId);

        // Clear old slot when vehicle changes
        setSelectedSlot(null);
        setAvailableSlots([]);

        setMessage("");
    };

    const selectedVehicleData = vehicles.find(
        (vehicle) =>
            vehicle.id.toString() === selectedVehicle
    );

    // Ask the backend which slots are free for the chosen time window
    useEffect(() => {
        let cancelled = false;
        if (!selectedVehicleData || !startTime || !endTime) {
            return () => {
                cancelled = true;
            };
        }

        const loadAvailableSlots = async () => {
            try {
                const response = await api.get("/bookings/available-slots", {
                    params: {
                        vehicleType: selectedVehicleData.vehicleType,
                        startTime,
                        endTime
                    }
                });
                if (!cancelled) {
                    setAvailableSlots(response.data);
                }
            } catch (error) {
                if (!cancelled) {
                    setAvailableSlots([]);
                    setMessage(
                        error.response?.data?.message ||
                            "Could not check slot availability"
                    );
                }
            }
        };

        void loadAvailableSlots();
        return () => {
            cancelled = true;
        };
    }, [selectedVehicleData, startTime, endTime, reloadKey]);

    const bookSlot = async () => {
        if (!selectedVehicle) {
            setMessage("Please select a vehicle");
            return;
        }

        if (!selectedSlot) {
            setMessage("Please select a parking slot");
            return;
        }

        try {
            await api.post(
                `/bookings/create?userId=${userId}&vehicleId=${selectedVehicle}&slotId=${selectedSlot.id}&startTime=${startTime}&endTime=${endTime}`
            );

            setMessage(
                `Request submitted for ${selectedVehicleData.vehicleNumber}. ` +
                "Next: submit your payment details on the Payments page. " +
                "The admin will check the payment and approve your request, " +
                "then your QR code appears on the QR page."
            );

            setSelectedSlot(null);
            setAvailableSlots([]);
            setReloadKey((key) => key + 1);

            await loadData();
        } catch (error) {
            console.log(error.response);

            setMessage(
                error.response?.data?.message ||
                error.response?.data ||
                "Booking failed"
            );
        }
    };

    return (
        <div className="container mt-4">

            <h2 className="text-center mb-4">
                Book Parking Slot
            </h2>

            {message && (
                <div className="alert alert-info">
                    {message}
                </div>
            )}

            <div className="card shadow mb-4">
                <div className="card-body">

                    <h5 className="text-center mb-3">
                        Select Vehicle
                    </h5>

                    {approvedVehicles.length === 0 && (
                        <div className="alert alert-warning">
                            You have no approved vehicle yet.
                            {vehicles.length === 0
                                ? " Register a vehicle first on the Vehicles page."
                                : " Your vehicle is waiting for admin approval (see the Vehicles page)."}
                        </div>
                    )}

                    <select
                        className="form-select"
                        value={selectedVehicle}
                        onChange={handleVehicleChange}
                    >
                        <option value="">
                            Select Vehicle
                        </option>

                        {approvedVehicles.map((vehicle) => (
                            <option
                                key={vehicle.id}
                                value={vehicle.id}
                            >
                                {vehicle.vehicleNumber} -{" "}
                                {vehicle.vehicleType}
                            </option>
                        ))}
                    </select>

                    {selectedVehicleData && (
                        <div className="alert alert-secondary mt-3 mb-0">
                            <strong>Selected Vehicle:</strong>{" "}
                            {selectedVehicleData.vehicleNumber}
                            <br />
                            <strong>Vehicle Type:</strong>{" "}
                            {selectedVehicleData.vehicleType}
                            <br />
                            <strong>Model:</strong>{" "}
                            {selectedVehicleData.vehicleModel}
                        </div>
                    )}

                </div>
            </div>

            {selectedVehicleData && (
                <div className="card shadow mb-4">
                    <div className="card-body">
                        <h5 className="text-center mb-3">Parking Time</h5>

                        <div className="row g-3">
                            <div className="col-md-6">
                                <label htmlFor="startTime" className="form-label">
                                    Start
                                </label>
                                <input
                                    id="startTime"
                                    type="datetime-local"
                                    className="form-control"
                                    value={startTime}
                                    onChange={(e) => {
                                        setSelectedSlot(null);
                                        setAvailableSlots([]);
                                        setStartTime(e.target.value);
                                    }}
                                />
                            </div>

                            <div className="col-md-6">
                                <label htmlFor="endTime" className="form-label">
                                    End
                                </label>
                                <input
                                    id="endTime"
                                    type="datetime-local"
                                    className="form-control"
                                    value={endTime}
                                    onChange={(e) => {
                                        setSelectedSlot(null);
                                        setAvailableSlots([]);
                                        setEndTime(e.target.value);
                                    }}
                                />
                            </div>
                        </div>

                        <p className="text-muted small mt-3 mb-0">
                            Minimum 15 minutes, maximum 24 hours, up to 7 days ahead.
                            Only slots free for this time are shown.
                        </p>
                    </div>
                </div>
            )}

            {selectedVehicleData ? (
                <>
                    <h4 className="text-center mb-3">
                        Available Parking Slots
                    </h4>

                    {availableSlots.length === 0 ? (
                        <div className="alert alert-warning">
                            No available slots for{" "}
                            {selectedVehicleData.vehicleType}.
                        </div>
                    ) : (
                        <div className="row g-3">

                            {availableSlots.map((slot) => (

                                <div
                                    className="col-md-4"
                                    key={slot.id}
                                >
                                    <div
                                        className={
                                            selectedSlot?.id === slot.id
                                                ? "card border-primary shadow"
                                                : "card border-success shadow"
                                        }
                                    >

                                        <div className="card-body text-center">

                                            <h5>
                                                Slot{" "}
                                                {slot.slotNumber}
                                            </h5>

                                            <p>
                                                {slot.vehicleType}
                                            </p>

                                            <span className="badge bg-success mb-3">
                                                AVAILABLE
                                            </span>

                                            <br />

                                            <button
                                                type="button"
                                                className="btn btn-primary"
                                                onClick={() =>
                                                    setSelectedSlot(slot)
                                                }
                                            >
                                                Select Slot
                                            </button>

                                        </div>

                                    </div>
                                </div>

                            ))}

                        </div>
                    )}

                    {selectedSlot && (
                        <div className="card shadow mt-4">
                            <div className="card-body text-center">

                                <h5>
                                    Booking Details
                                </h5>

                                <p>
                                    <strong>Vehicle:</strong>{" "}
                                    {selectedVehicleData.vehicleNumber}
                                </p>

                                <p>
                                    <strong>Vehicle Type:</strong>{" "}
                                    {selectedVehicleData.vehicleType}
                                </p>

                                <p>
                                    <strong>Slot:</strong>{" "}
                                    {selectedSlot.slotNumber}
                                </p>

                                <button
                                    type="button"
                                    className="btn btn-success"
                                    onClick={bookSlot}
                                >
                                    Confirm Booking
                                </button>

                            </div>
                        </div>
                    )}

                </>
            ) : (
                <div className="alert alert-primary text-center">
                    Please select a vehicle to view suitable parking slots.
                </div>
            )}

        </div>
    );
}

export default Booking;