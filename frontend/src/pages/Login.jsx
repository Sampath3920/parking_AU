import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../services/api";
import { homePathForRole, saveSession } from "../services/auth";

function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();

        try {
            const response = await api.post("/auth/login", {
                email,
                password
            });

            saveSession(response.data);
            navigate(homePathForRole(response.data.role), { replace: true });
        } catch (error) {
            setMessage(
                error.response?.data?.message || "Invalid email or password"
            );
        }
    };

    return (
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-md-5">
                    <div className="card shadow">
                        <div className="card-body p-4">

                            <h2 className="text-center mb-4">
                                Smart College Parking
                            </h2>

                            <h5 className="text-center mb-4">
                                Login
                            </h5>

                            <form onSubmit={handleLogin}>

                                <div className="mb-3">
                                    <label className="form-label">
                                        Email
                                    </label>

                                    <input
                                        type="email"
                                        className="form-control"
                                        value={email}
                                        onChange={(e) =>
                                            setEmail(e.target.value)
                                        }
                                        required
                                    />
                                </div>

                                <div className="mb-3">
                                    <label className="form-label">
                                        Password
                                    </label>

                                    <input
                                        type="password"
                                        className="form-control"
                                        value={password}
                                        onChange={(e) =>
                                            setPassword(e.target.value)
                                        }
                                        required
                                    />
                                </div>

                                <button
                                    type="submit"
                                    className="btn btn-primary w-100"
                                >
                                    Login
                                </button>

                            </form>

                            <div className="text-center mt-3">
                                <span>Don't have an account? </span>
                                <Link to="/register">
                                    Register
                                </Link>
                            </div>

                            {message && (
                                <div className="alert alert-info mt-3">
                                    {message}
                                </div>
                            )}

                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Login;