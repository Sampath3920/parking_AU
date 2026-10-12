import axios from "axios";
import { clearSession } from "./auth";

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:4000/api"
});

// Send the JWT with every request
api.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

// 401 = token missing, wrong or expired -> go back to the login page.
// (A wrong password on the login page also returns 401, so we skip that URL.)
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;
        const url = error.config?.url || "";

        if (status === 401 && !url.includes("/auth/")) {
            clearSession();
            window.location.href = "/";
        }

        return Promise.reject(error);
    }
);

export default api;
