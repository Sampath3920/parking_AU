import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import StudentDashboard from "./pages/StudentDashboard";
import Vehicles from "./pages/Vehicles";
import Parking from "./pages/Parking";
import Booking from "./pages/Booking";
import History from "./pages/History";
import QR from "./pages/QR";
import Payments from "./pages/Payments";
import Notifications from "./pages/Notifications";
import ParkingPass from "./pages/ParkingPass";
import ParkingPermission from "./pages/ParkingPermission";
import AdminDashboard from "./pages/AdminDashboard";
import AdminVehicles from "./pages/AdminVehicles";
import AdminBookings from "./pages/AdminBookings";
import AdminSlots from "./pages/AdminSlots";
import AdminUsers from "./pages/AdminUsers";
import AdminPayments from "./pages/AdminPayments";
import AdminAnnouncements from "./pages/AdminAnnouncements";
import AdminReports from "./pages/AdminReports";
import ServiceReports from "./pages/ServiceReports";
import Help from "./pages/Help";
import ProtectedRoute from "./components/ProtectedRoute";

const USERS = ["STUDENT", "FACULTY", "STAFF", "VISITOR"];
const EVERYONE = [...USERS, "GUARD", "ADMIN"];

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Login />} />
                <Route path="/register" element={<Register />} />

                <Route path="/student-dashboard" element={<ProtectedRoute roles={USERS}><StudentDashboard /></ProtectedRoute>} />
                <Route path="/vehicles" element={<ProtectedRoute roles={USERS}><Vehicles /></ProtectedRoute>} />
                <Route path="/parking" element={<ProtectedRoute roles={EVERYONE}><Parking /></ProtectedRoute>} />
                <Route path="/booking" element={<ProtectedRoute roles={USERS}><Booking /></ProtectedRoute>} />
                <Route path="/history" element={<ProtectedRoute roles={USERS}><History /></ProtectedRoute>} />
                <Route path="/qr" element={<ProtectedRoute roles={EVERYONE}><QR /></ProtectedRoute>} />
                <Route path="/payments" element={<ProtectedRoute roles={USERS}><Payments /></ProtectedRoute>} />
                <Route path="/notifications" element={<ProtectedRoute roles={EVERYONE}><Notifications /></ProtectedRoute>} />
                <Route path="/parking-pass" element={<ProtectedRoute roles={USERS}><ParkingPass /></ProtectedRoute>} />
                <Route path="/parking-permission" element={<ProtectedRoute roles={USERS}><ParkingPermission /></ProtectedRoute>} />
                <Route path="/admin-dashboard" element={<ProtectedRoute roles={["ADMIN"]}><AdminDashboard /></ProtectedRoute>} />
                <Route path="/admin/vehicles" element={<ProtectedRoute roles={["ADMIN"]}><AdminVehicles /></ProtectedRoute>} />
                <Route path="/admin/bookings" element={<ProtectedRoute roles={["ADMIN"]}><AdminBookings /></ProtectedRoute>} />
                <Route path="/admin/slots" element={<ProtectedRoute roles={["ADMIN"]}><AdminSlots /></ProtectedRoute>} />
                <Route path="/admin/users" element={<ProtectedRoute roles={["ADMIN"]}><AdminUsers /></ProtectedRoute>} />
                <Route path="/admin/payments" element={<ProtectedRoute roles={["ADMIN"]}><AdminPayments /></ProtectedRoute>} />
                <Route path="/admin/reports" element={<ProtectedRoute roles={["ADMIN"]}><AdminReports /></ProtectedRoute>} />
                <Route path="/reports" element={<ProtectedRoute roles={[...USERS, "GUARD"]}><ServiceReports /></ProtectedRoute>} />
                <Route path="/help" element={<ProtectedRoute roles={EVERYONE}><Help /></ProtectedRoute>} />
                <Route path="/admin/announcements" element={<ProtectedRoute roles={["ADMIN"]}><AdminAnnouncements /></ProtectedRoute>} />

                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
