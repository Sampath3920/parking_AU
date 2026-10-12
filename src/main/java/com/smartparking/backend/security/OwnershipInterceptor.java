package com.smartparking.backend.security;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.ParkingPass;
import com.smartparking.backend.model.ParkingPermission;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingPassRepository;
import com.smartparking.backend.repository.ParkingPermissionRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stops a normal user from reading or changing another user's data.
 * Administrators and security guards are allowed everywhere this check applies.
 *
 * It checks three kinds of URLs:
 *  1. /api/xxx/user/{userId}  and  /api/vehicles/add/{userId}   (and ?userId=... query parameter)
 *  2. /api/bookings/{id}, /api/bookings/{id}/cancel, /api/qr/generate/{id}, /api/payments/...{bookingId}
 *  3. /api/vehicles/{id}
 */
@Component
public class OwnershipInterceptor implements HandlerInterceptor {

    private static final Pattern USER_PATH =
            Pattern.compile("^/api/[a-z-]+/(?:user|add)/(\\d+)/?$");

    private static final Pattern BOOKING_PATH = Pattern.compile(
            "^/api/(?:bookings|qr/generate|payments/create|payments/booking)/(\\d+)(?:/cancel)?/?$");

    private static final Pattern VEHICLE_PATH = Pattern.compile("^/api/vehicles/(\\d+)/?$");

    private static final Pattern PASS_PATH = Pattern.compile("^/api/parking-passes/(\\d+)/?$");

    private static final Pattern PERMISSION_PATH = Pattern.compile("^/api/parking-permissions/(\\d+)/?$");

    private static final Pattern PAYMENT_PATH = Pattern.compile("^/api/payments/(\\d+)/?$");

    private static final Pattern BY_VEHICLE_PATH =
            Pattern.compile("^/api/(?:parking-passes|parking-permissions)/vehicle/(\\d+)/?$");

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingPassRepository passRepository;
    private final ParkingPermissionRepository permissionRepository;
    private final PaymentRepository paymentRepository;

    public OwnershipInterceptor(UserRepository userRepository,
                                BookingRepository bookingRepository,
                                VehicleRepository vehicleRepository,
                                ParkingPassRepository passRepository,
                                ParkingPermissionRepository permissionRepository,
                                PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.passRepository = passRepository;
        this.permissionRepository = permissionRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true; // Spring Security already decides about anonymous requests
        }
        if (hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ROLE_GUARD")) {
            return true;
        }

        String path = request.getRequestURI();
        Long ownerId = null;
        boolean needsCheck = false;

        Matcher userMatcher = USER_PATH.matcher(path);
        Matcher bookingMatcher = BOOKING_PATH.matcher(path);
        Matcher vehicleMatcher = VEHICLE_PATH.matcher(path);
        Matcher passMatcher = PASS_PATH.matcher(path);
        Matcher permissionMatcher = PERMISSION_PATH.matcher(path);
        Matcher paymentMatcher = PAYMENT_PATH.matcher(path);
        Matcher byVehicleMatcher = BY_VEHICLE_PATH.matcher(path);

        if (userMatcher.matches()) {
            needsCheck = true;
            ownerId = Long.valueOf(userMatcher.group(1));
        } else if (bookingMatcher.matches()) {
            needsCheck = true;
            Optional<Booking> booking = bookingRepository.findById(Long.valueOf(bookingMatcher.group(1)));
            if (booking.isEmpty()) {
                return true; // the controller will answer "Booking not found"
            }
            ownerId = booking.get().getUser() != null ? booking.get().getUser().getId() : null;
        } else if (vehicleMatcher.matches()) {
            needsCheck = true;
            Optional<Vehicle> vehicle = vehicleRepository.findById(Long.valueOf(vehicleMatcher.group(1)));
            if (vehicle.isEmpty()) {
                return true;
            }
            ownerId = vehicle.get().getUser() != null ? vehicle.get().getUser().getId() : null;
        } else if (byVehicleMatcher.matches()) {
            needsCheck = true;
            Optional<Vehicle> vehicle = vehicleRepository.findById(Long.valueOf(byVehicleMatcher.group(1)));
            if (vehicle.isEmpty()) {
                return true;
            }
            ownerId = vehicle.get().getUser() != null ? vehicle.get().getUser().getId() : null;
        } else if (passMatcher.matches()) {
            needsCheck = true;
            Optional<ParkingPass> pass = passRepository.findById(Long.valueOf(passMatcher.group(1)));
            if (pass.isEmpty()) {
                return true;
            }
            ownerId = pass.get().getUser() != null ? pass.get().getUser().getId() : null;
        } else if (permissionMatcher.matches()) {
            needsCheck = true;
            Optional<ParkingPermission> permission =
                    permissionRepository.findById(Long.valueOf(permissionMatcher.group(1)));
            if (permission.isEmpty()) {
                return true;
            }
            ownerId = permission.get().getUser() != null ? permission.get().getUser().getId() : null;
        } else if (paymentMatcher.matches()) {
            needsCheck = true;
            Optional<Payment> payment = paymentRepository.findById(Long.valueOf(paymentMatcher.group(1)));
            if (payment.isEmpty()) {
                return true;
            }
            Booking paidBooking = payment.get().getBooking();
            ownerId = (paidBooking != null && paidBooking.getUser() != null)
                    ? paidBooking.getUser().getId() : null;
        }

        String userIdParam = request.getParameter("userId");
        if (userIdParam != null && userIdParam.matches("\\d+")) {
            // For example POST /api/bookings/create?userId=5&...
            Long paramId = Long.valueOf(userIdParam);
            Long me = currentUserId(auth);
            if (me == null || !me.equals(paramId)) {
                return deny(response);
            }
        }

        String vehicleIdParam = request.getParameter("vehicleId");
        if (vehicleIdParam != null && vehicleIdParam.matches("\\d+")) {
            // For example POST /api/parking-passes/create?userId=5&vehicleId=7
            // The vehicle must belong to the logged-in user.
            Optional<Vehicle> paramVehicle = vehicleRepository.findById(Long.valueOf(vehicleIdParam));
            Long me = currentUserId(auth);
            if (paramVehicle.isPresent()) {
                Vehicle v = paramVehicle.get();
                Long vehicleOwner = v.getUser() != null ? v.getUser().getId() : null;
                if (me == null || !me.equals(vehicleOwner)) {
                    return deny(response);
                }
            }
        }

        if (!needsCheck) {
            return true;
        }

        Long me = currentUserId(auth);
        if (me != null && me.equals(ownerId)) {
            return true;
        }
        return deny(response);
    }

    private boolean hasRole(Authentication auth, String role) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Long currentUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }

    private boolean deny(HttpServletResponse response) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"You can only access your own data\"}");
        return false;
    }
}
