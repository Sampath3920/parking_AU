import { useEffect, useState } from "react";
import api from "../services/api";
import { getUser } from "../services/auth";

function Vehicles() {
    const userId = getUser()?.id;

    const [vehicles, setVehicles] = useState([]);
    const [form, setForm] = useState({
        vehicleNumber: "",
        vehicleType: "2-WHEELER",
        vehicleModel: ""
    });
    const [editingId, setEditingId] = useState(null);
    const [message, setMessage] = useState("");

    const loadVehicles = async () => {
        try {
            const response = await api.get(`/vehicles/user/${userId}`);
            setVehicles(response.data);
        } catch (error) {
            setMessage(error.response?.data?.message || "Failed to load vehicles");
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
            .catch((error) => {
                if (!cancelled) {
                    setMessage(
                        error.response?.data?.message || "Failed to load vehicles"
                    );
                }
            });
        return () => {
            cancelled = true;
        };
    }, [userId]);

    const handleChange = (e) => {
        setForm({
            ...form,
            [e.target.name]: e.target.value
        });
    };

    const saveVehicle = async (e) => {
        e.preventDefault();

        try {
            if (editingId) {
                await api.put(`/vehicles/${editingId}`, form);
                setMessage("Vehicle updated successfully!");
            } else {
                await api.post(`/vehicles/add/${userId}`, form);
                setMessage("Vehicle added successfully!");
            }

            setForm({
                vehicleNumber: "",
                vehicleType: "2-WHEELER",
                vehicleModel: ""
            });

            setEditingId(null);
            loadVehicles();
        } catch (error) {
            setMessage(error.response?.data?.message || "Failed to save vehicle");
        }
    };

    const editVehicle = (vehicle) => {
        setEditingId(vehicle.id);

        setForm({
            vehicleNumber: vehicle.vehicleNumber,
            vehicleType: vehicle.vehicleType,
            vehicleModel: vehicle.vehicleModel
        });

        setMessage("");
    };

    const deleteVehicle = async (id) => {
        try {
            await api.delete(`/vehicles/${id}`);
            setMessage("Vehicle deleted successfully!");
            loadVehicles();
        } catch (error) {
            setMessage(error.response?.data?.message || "Failed to delete vehicle");
        }
    };

    const cancelEdit = () => {
        setEditingId(null);

        setForm({
            vehicleNumber: "",
            vehicleType: "2-WHEELER",
            vehicleModel: ""
        });
    };

    const vehicleLimitReached = vehicles.length >= 3 && !editingId;

    return (
        <div className="container mt-4">

            <h2 className="mb-4">My Vehicles</h2>

            <div className="card shadow mb-4">
                <div className="card-body">

                    <h5 className="mb-3">
                        {editingId ? "Edit Vehicle" : "Add Vehicle"}
                    </h5>

                    {vehicleLimitReached && (
                        <div className="alert alert-warning">
                            You have reached the limit of three registered vehicles.
                        </div>
                    )}

                    <form onSubmit={saveVehicle}>

                        <input
                            name="vehicleNumber"
                            className="form-control mb-3"
                            placeholder="Vehicle Number"
                            value={form.vehicleNumber}
                            onChange={handleChange}
                            required
                        />

                        <select
                            name="vehicleType"
                            className="form-select mb-3"
                            value={form.vehicleType}
                            onChange={handleChange}
                        >
                            <option value="2-WHEELER">
                                2-Wheeler
                            </option>

                            <option value="4-WHEELER">
                                4-Wheeler
                            </option>
                        </select>

                        <input
                            name="vehicleModel"
                            className="form-control mb-3"
                            placeholder="Vehicle Model"
                            value={form.vehicleModel}
                            onChange={handleChange}
                            required
                        />

                        <button
                            type="submit"
                            className="btn btn-primary me-2"
                            disabled={vehicleLimitReached}
                        >
                            {editingId ? "Update Vehicle" : "Add Vehicle"}
                        </button>

                        {editingId && (
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={cancelEdit}
                            >
                                Cancel
                            </button>
                        )}

                    </form>

                    {message && (
                        <div className="alert alert-info mt-3">
                            {message}
                        </div>
                    )}

                </div>
            </div>

            <div className="card shadow">
                <div className="card-body">

                    <h5 className="mb-3">
                        My Registered Vehicles
                    </h5>

                    {vehicles.length === 0 ? (
                        <p>No vehicles registered.</p>
                    ) : (
                        <div className="table-responsive">

                            <table className="table table-bordered table-hover">

                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Vehicle Number</th>
                                        <th>Type</th>
                                        <th>Model</th>
                                        <th>Verification</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {vehicles.map((vehicle) => (
                                        <tr key={vehicle.id}>

                                            <td>
                                                {vehicle.id}
                                            </td>

                                            <td>
                                                {vehicle.vehicleNumber}
                                            </td>

                                            <td>
                                                {vehicle.vehicleType}
                                            </td>

                                            <td>
                                                {vehicle.vehicleModel}
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        vehicle.verificationStatus === "VERIFIED"
                                                            ? "badge bg-success"
                                                            : vehicle.verificationStatus === "REJECTED"
                                                            ? "badge bg-danger"
                                                            : "badge bg-warning text-dark"
                                                    }
                                                >
                                                    {vehicle.verificationStatus}
                                                </span>
                                                {vehicle.reviewReason && (
                                                    <div><small>{vehicle.reviewReason}</small></div>
                                                )}
                                            </td>

                                            <td>
                                                <button
                                                    className="btn btn-sm btn-primary me-2 mb-1"
                                                    onClick={() =>
                                                        editVehicle(vehicle)
                                                    }
                                                >
                                                    Edit
                                                </button>

                                                <button
                                                    className="btn btn-sm btn-danger me-2 mb-1"
                                                    onClick={() =>
                                                        deleteVehicle(vehicle.id)
                                                    }
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

                </div>
            </div>

        </div>
    );
}

export default Vehicles;