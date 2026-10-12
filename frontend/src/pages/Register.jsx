import { useState } from "react";
import api from "../services/api";

function Register() {
    const [form, setForm] = useState({
        name: "",
        email: "",
        password: "",
        phone: "",
        role: "STUDENT"
    });

    const [message, setMessage] = useState("");

    const handleChange = (e) => {
        setForm({
            ...form,
            [e.target.name]: e.target.value
        });
    };

    const handleRegister = async (e) => {
        e.preventDefault();

        try {
            await api.post("/auth/register", form);
            setMessage("Registration successful! You can login now.");
        } catch (error) {
            setMessage(
                error.response?.data?.message || "Registration failed"
            );
        }
    };

    return (
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-md-6">
                    <div className="card shadow">
                        <div className="card-body p-4">
                            <h2 className="text-center mb-4">
                                Smart College Parking
                            </h2>

                            <h5 className="text-center mb-4">
                                Register
                            </h5>

                            <form onSubmit={handleRegister}>
                                <input
                                    name="name"
                                    className="form-control mb-3"
                                    placeholder="Full Name"
                                    value={form.name}
                                    onChange={handleChange}
                                    required
                                />

                                <input
                                    name="email"
                                    type="email"
                                    className="form-control mb-3"
                                    placeholder="Email"
                                    value={form.email}
                                    onChange={handleChange}
                                    required
                                />

                                <input
                                    name="password"
                                    type="password"
                                    className="form-control mb-3"
                                    placeholder="Password"
                                    value={form.password}
                                    onChange={handleChange}
                                    required
                                />

                                <input
                                    name="phone"
                                    className="form-control mb-3"
                                    placeholder="Phone"
                                    value={form.phone}
                                    onChange={handleChange}
                                    required
                                />

                                <select
                                    name="role"
                                    className="form-select mb-3"
                                    value={form.role}
                                    onChange={handleChange}
                                >
                                    <option value="STUDENT">Student</option>
                                    <option value="FACULTY">Faculty</option>
                                    <option value="STAFF">Staff</option>
                                    <option value="VISITOR">Visitor</option>
                                </select>

                                <button
                                    type="submit"
                                    className="btn btn-success w-100"
                                >
                                    Register
                                </button>
                            </form>

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

export default Register;