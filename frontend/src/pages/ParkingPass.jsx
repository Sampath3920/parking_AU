import { useEffect, useState } from "react";
import api from "../services/api";
import { getUser } from "../services/auth";

function ParkingPass() {
    const userId = getUser()?.id;

    const [vehicles, setVehicles] = useState([]);
    const [passes, setPasses] = useState([]);

    const [selectedVehicle, setSelectedVehicle] = useState("");
    const [message, setMessage] = useState("");

    const loadData = async () => {
        try {
            const vehicleResponse = await api.get(
                `/vehicles/user/${userId}`
            );

            const passResponse = await api.get(
                `/parking-passes/user/${userId}`
            );

            setVehicles(vehicleResponse.data);
            setPasses(passResponse.data);
        } catch (error) {
            console.log(error.response);
            setMessage("Failed to load parking passes");
        }
    };

    useEffect(() => {
        let cancelled = false;
        Promise.all([
            api.get(`/vehicles/user/${userId}`),
            api.get(`/parking-passes/user/${userId}`)
        ])
            .then(([vehicleResponse, passResponse]) => {
                if (!cancelled) {
                    setVehicles(vehicleResponse.data);
                    setPasses(passResponse.data);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setMessage("Failed to load parking passes");
                }
            });
        return () => {
            cancelled = true;
        };
    }, [userId]);

    const createPass = async () => {
        if (!selectedVehicle) {
            setMessage("Please select a vehicle");
            return;
        }

        try {
            await api.post(
                `/parking-passes/create?userId=${userId}&vehicleId=${selectedVehicle}`,
                {
                    passType: "STUDENT",
                    status: "ACTIVE"
                }
            );

            setMessage(
                "Parking pass created successfully!"
            );

            setSelectedVehicle("");

            loadData();
        } catch (error) {
            console.log(error.response);

            setMessage(
                error.response?.data?.message ||
                "Failed to create parking pass"
            );
        }
    };

    const deletePass = async (id) => {
        try {
            await api.delete(
                `/parking-passes/${id}`
            );

            setMessage(
                "Parking pass deleted successfully"
            );

            loadData();
        } catch (error) {
            console.log(error.response);

            setMessage(
                "Failed to delete parking pass"
            );
        }
    };

    return (
        <div className="container mt-4 mb-5">

            <h2 className="text-center mb-4">
                Parking Pass
            </h2>

            {message && (
                <div className="alert alert-info">
                    {message}
                </div>
            )}

            <div className="card shadow mb-4">

                <div className="card-body">

                    <h5 className="mb-3">
                        Create Parking Pass
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

                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={createPass}
                    >
                        Create Parking Pass
                    </button>

                </div>

            </div>

            <h4 className="mb-3">
                My Parking Passes
            </h4>

            {passes.length === 0 ? (
                <div className="alert alert-warning">
                    No parking passes found.
                </div>
            ) : (
                <div className="row g-3">

                    {passes.map((pass) => (
                        <div
                            className="col-md-6"
                            key={pass.id}
                        >

                            <div className="card shadow">

                                <div className="card-body">

                                    <h5>
                                        Parking Pass #{pass.id}
                                    </h5>

                                    <p className="mb-2">
                                        <strong>
                                            Pass Number:
                                        </strong>{" "}
                                        {pass.passNumber || "-"}
                                    </p>

                                    <p className="mb-2">
                                        <strong>
                                            Pass Type:
                                        </strong>{" "}
                                        {pass.passType || "-"}
                                    </p>

                                    <p className="mb-2">
                                        <strong>
                                            Vehicle:
                                        </strong>{" "}
                                        {pass.vehicle?.vehicleNumber ||
                                            "-"}
                                    </p>

                                    <p className="mb-2">
                                        <strong>
                                            Vehicle Type:
                                        </strong>{" "}
                                        {pass.vehicle?.vehicleType ||
                                            "-"}
                                    </p>

                                    <p className="mb-2">
                                        <strong>
                                            Status:
                                        </strong>{" "}

                                        <span className="badge bg-success">
                                            {pass.status || "ACTIVE"}
                                        </span>
                                    </p>

                                    {pass.startDate && (
                                        <p className="mb-2">
                                            <strong>
                                                Start:
                                            </strong>{" "}

                                            {new Date(
                                                pass.startDate
                                            ).toLocaleDateString()}
                                        </p>
                                    )}

                                    {pass.endDate && (
                                        <p className="mb-3">
                                            <strong>
                                                End:
                                            </strong>{" "}

                                            {new Date(
                                                pass.endDate
                                            ).toLocaleDateString()}
                                        </p>
                                    )}

                                    <button
                                        type="button"
                                        className="btn btn-danger btn-sm"
                                        onClick={() =>
                                            deletePass(pass.id)
                                        }
                                    >
                                        Delete Pass
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

export default ParkingPass;