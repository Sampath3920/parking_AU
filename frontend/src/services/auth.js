// Keeps the logged-in user in the browser (localStorage).
// The JWT token is sent to the backend by services/api.js.

export function saveSession(loginResponse) {
    localStorage.setItem("token", loginResponse.token);
    localStorage.setItem(
        "user",
        JSON.stringify({
            id: loginResponse.id,
            name: loginResponse.name,
            email: loginResponse.email,
            role: loginResponse.role
        })
    );
}

export function getUser() {
    try {
        const text = localStorage.getItem("user");
        return text ? JSON.parse(text) : null;
    } catch {
        return null;
    }
}

export function getToken() {
    return localStorage.getItem("token");
}

export function isLoggedIn() {
    return Boolean(getToken() && getUser());
}

export function clearSession() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
}

export function logout() {
    clearSession();
    window.location.href = "/";
}

export function homePathForRole(role) {
    if (role === "ADMIN") return "/admin-dashboard";
    if (role === "GUARD") return "/qr";
    return "/student-dashboard";
}
