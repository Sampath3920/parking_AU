import { useEffect, useState } from "react";
import api from "../services/api";

function Parking() {
    const [locations, setLocations] = useState([]);
    const [slots, setSlots] = useState([]);
    const [message, setMessage] = useState("");
    const [search, setSearch] = useState("");

    useEffect(() => {
        let cancelled = false;
        Promise.all([
            api.get("/parking-locations"),
            api.get("/parking-slots")
        ])
            .then(([locationResponse, slotResponse]) => {
                if (!cancelled) {
                    setLocations(locationResponse.data);
                    setSlots(slotResponse.data);
                }
            })
            .catch((error) => {
                if (!cancelled) {
                    setMessage(
                        error.response?.data?.message ||
                            "Failed to load parking information"
                    );
                }
            });
        return () => {
            cancelled = true;
        };
    }, []);

    const getSlotCount = (locationId, status) => {
        return slots.filter(
            (slot) =>
                slot.parkingLocation?.id === locationId &&
                slot.status === status
        ).length;
    };

    return (
        <div className="container mt-4">

            <h2 className="mb-3">Parking Availability</h2>

            <label htmlFor="parkingSearch" className="form-label">
                Search by name or place
            </label>
            <input
                id="parkingSearch"
                type="search"
                className="form-control mb-4"
                placeholder="Example: main block"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
            />

            {message && (
                <div className="alert alert-danger">
                    {message}
                </div>
            )}

            <div className="row g-4">

                {locations
                    .filter((location) =>
                        `${location.name} ${location.location}`
                            .toLowerCase()
                            .includes(search.trim().toLowerCase())
                    )
                    .map((location) => {

                    const available =
                        getSlotCount(location.id, "AVAILABLE");

                    const booked =
                        getSlotCount(location.id, "BOOKED");

                    return (
                        <div
                            className="col-md-6"
                            key={location.id}
                        >
                            <div className="card shadow h-100">

                                <div className="card-body">

                                    <h4>
                                        {location.name}
                                    </h4>

                                    <p className="text-muted">
                                        📍 {location.location}
                                    </p>

                                    <span className="badge bg-success me-2">
                                        {available} Available
                                    </span>

                                    <span className="badge bg-danger">
                                        {booked} Booked
                                    </span>

                                    <hr />

                                    <h5 className="mb-3">
                                        Parking Slots
                                    </h5>

                                    <div className="row g-2">

                                        {slots
                                            .filter(
                                                (slot) =>
                                                    slot.parkingLocation?.id ===
                                                    location.id
                                            )
                                            .map((slot) => (

                                                <div
                                                    className="col-6"
                                                    key={slot.id}
                                                >
                                                    <div
                                                        className={
                                                            slot.status === "AVAILABLE"
                                                                ? "border border-success rounded p-3 text-center"
                                                                : "border border-danger rounded p-3 text-center"
                                                        }
                                                    >

                                                        <h6>
                                                            Slot{" "}
                                                            {slot.slotNumber}
                                                        </h6>

                                                        <small>
                                                            {slot.vehicleType}
                                                        </small>

                                                        <br />

                                                        <span
                                                            className={
                                                                slot.status === "AVAILABLE"
                                                                    ? "badge bg-success mt-2"
                                                                    : "badge bg-danger mt-2"
                                                            }
                                                        >
                                                            {slot.status}
                                                        </span>

                                                    </div>
                                                </div>

                                            ))}

                                    </div>

                                </div>
                            </div>
                        </div>
                    );
                })}

            </div>

        </div>
    );
}

export default Parking;