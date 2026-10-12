import { useEffect, useState } from "react";
import api from "../services/api";
import { getUser } from "../services/auth";

function ParkingPermission() {
    const userId = getUser()?.id;

    const [vehicles, setVehicles] = useState([]);
    const [permissions, setPermissions] = useState([]);

    const [selectedVehicle, setSelectedVehicle] = useState("");
    const [reason, setReason] = useState("");
    const [message, setMessage] = useState("");

    const loadData = async () => {
        try {
            const vehicleResponse = await api.get(
                `/vehicles/user/${userId}`
            );

            const permissionResponse = await api.get(
                `/parking-permissions/user/${userId}`
            );

            setVehicles(vehicleResponse.data);
            setPermissions(permissionResponse.data);
        } catch (error) {
            console.log(error.response);
            setMessage("Failed to load parking permissions");
        }
    };

    useEffect(() => {
        let cancelled = false;
        Promise.all([
            api.get(`/vehicles/user/${userId}`),
            api.get(`/parking-permissions/user/${userId}`)
        ])
            .then(([vehicleResponse, permissionResponse]) => {
                if (!cancelled) {
                    setVehicles(vehicleResponse.data);
                    setPermissions(permissionResponse.data);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setMessage("Failed to load parking permissions");
                }
            });
        return () => {
            cancelled = true;
        };
    }, [userId]);

    const createPermission = async () => {
        if (!selectedVehicle) {
            setMessage("Please select a vehicle");
            return;
        }

        if (!reason.trim()) {
            setMessage("Please enter a reason");
            return;
        }

        try {
            await api.post(
                `/parking-permissions/create?userId=${userId}&vehicleId=${selectedVehicle}`,
                {
                    reason: reason
                }
            );

            setMessage(
                "Parking permission request created successfully!"
            );

            setSelectedVehicle("");
            setReason("");

            loadData();
        } catch (error) {
            console.log(error.response);

            setMessage(
                error.response?.data?.message ||
                "Failed to create permission request"
            );
        }
    };

    const deletePermission = async (id) => {
        try {
            await api.delete(
                `/parking-permissions/${id}`
            );

            setMessage(
                "Permission request deleted successfully"
            );

            loadData();
        } catch (error) {
            console.log(error.response);

            setMessage(
                "Failed to delete permission request"
            );
        }
    };

    return (
        <div className="container mt-4 mb-5">

            <h2 className="text-center mb-4">
                Parking Permission
            </h2>

            {message && (
                <div className="alert alert-info">
                    {message}
                </div>
            )}

            <div className="card shadow mb-4">

                <div className="card-body">

                    <h5 className="mb-3">
                        Request Parking Permission
                    </h5>

                    <label className="form-label">
                        Select Vehicle
                    </label>

                    <select
                        className="form-select mb-3"
                        value={selectedVehicle}
                        onChange={(e) =>
                            setSelectedVehicle(e.target.value)
                        }
                    >
                        <option value="">
                            Select Vehicle
                        </option>

                        {vehicles.map((vehicle) => (
                            <option
                                key={vehicle.id}
                                value={vehicle.id}
                            >
                                {vehicle.vehicleNumber} -{" "}
                                {vehicle.vehicleType}
                            </option>
                        ))}
                    </select>

                    <label className="form-label">
                        Reason
                    </label>

                    <textarea
                        className="form-control mb-3"
                        rows="4"
                        placeholder="Enter reason for parking permission"
                        value={reason}
                        onChange={(e) =>
                            setReason(e.target.value)
                        }
                    />

                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={createPermission}
                    >
                        Submit Permission Request
                    </button>

                </div>

            </div>

            <h4 className="mb-3">
                My Permission Requests
            </h4>

            {permissions.length === 0 ? (
                <div className="alert alert-warning">
                    No permission requests found.
                </div>
            ) : (
                <div className="row g-3">

                    {permissions.map((permission) => (

                        <div
                            className="col-md-6"
                            key={permission.id}
                        >

                            <div className="card shadow">

                                <div className="card-body">

                                    <h5>
                                        Permission #{permission.id}
                                    </h5>

                                    <p className="mb-2">
                                        <strong>
                                            Vehicle:
                                        </strong>{" "}
                                        {permission.vehicle?.vehicleNumber ||
                                            "-"}
                                    </p>

                                    <p className="mb-2">
                                        <strong>
                                            Vehicle Type:
                                        </strong>{" "}
                                        {permission.vehicle?.vehicleType ||
                                            "-"}
                                    </p>

                                    <p className="mb-2">
                                        <strong>
                                            Reason:
                                        </strong>{" "}
                                        {permission.reason || "-"}
                                    </p>

                                    <p className="mb-3">
                                        <strong>
                                            Status:
                                        </strong>{" "}

                                        <span
                                            className={
                                                permission.status ===
                                                "APPROVED"
                                                    ? "badge bg-success"
                                                    : permission.status ===
                                                      "REJECTED"
                                                    ? "badge bg-danger"
                                                    : "badge bg-warning text-dark"
                                            }
                                        >
                                            {permission.status ||
                                                "PENDING"}
                                        </span>
                                    </p>

                                    <button
                                        type="button"
                                        className="btn btn-danger btn-sm"
                                        onClick={() =>
                                            deletePermission(
                                                permission.id
                                            )
                                        }
                                    >
                                        Delete Request
                                    </button>

                                </div>

                            </div>

                        </div>

                    ))}

                </div>
            )}

        </div>
    );
}

export default ParkingPermission;