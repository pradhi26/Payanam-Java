package com.payanam.server;

import com.payanam.model.*;
import com.payanam.repository.DataStore;
import com.payanam.util.JsonUtils;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

public class PayanamServer {
    private final int port;
    private final DataStore dataStore;
    private HttpServer server;

    public PayanamServer(int port) {
        this.port = port;
        this.dataStore = DataStore.getInstance();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // Java 21 Virtual Threads!

        // API Contexts
        server.createContext("/api/auth/login", this::handleLogin);
        server.createContext("/api/auth/register", this::handleRegister);
        server.createContext("/api/auth/logout", this::handleLogout);
        server.createContext("/api/auth/me", this::handleMe);

        server.createContext("/api/packages", this::handlePackages);
        server.createContext("/api/bookings", this::handleBookings);
        server.createContext("/api/reviews", this::handleReviews);
        server.createContext("/api/admin/stats", this::handleAdminStats);

        // Static Files
        server.createContext("/", this::handleStaticFiles);

        server.start();
        System.out.println("=================================================");
        System.out.println("   PAYANAM - Tourist Booking Web Application   ");
        System.out.println("=================================================");
        System.out.println(" Server running at: http://localhost:" + port);
        System.out.println(" Serving responsive web frontend & REST APIs");
        System.out.println(" Default Admin: admin@gmail.com / admin123");
        System.out.println(" Default Tourist: rahul@gmail.com / rahul123");
        System.out.println("=================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- Helper Methods ---
    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = (data instanceof String && ((String) data).startsWith("{")) 
                      ? (String) data : JsonUtils.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", true);
        err.put("message", message);
        sendJson(exchange, statusCode, err);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private User getAuthUser(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            return dataStore.getSessionUser(token);
        }
        return null;
    }

    private boolean handleCors(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    // --- Handlers ---
    private void handleLogin(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String raw = readBody(exchange);
        Map<String, Object> body = JsonUtils.parseObject(raw);
        String email = (String) body.get("email");
        String password = (String) body.get("password");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            sendError(exchange, 400, "Please provide both email and password.");
            return;
        }

        User user = dataStore.findUserByEmail(email);
        if (user == null || !password.equals(user.getPassword())) {
            sendError(exchange, 401, "Invalid email or password.");
            return;
        }

        String token = dataStore.createSession(user);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("token", token);
        resp.put("user", Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole()
        ));
        sendJson(exchange, 200, resp);
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, Object> body = JsonUtils.parseObject(readBody(exchange));
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        String password = (String) body.get("password");

        if (name == null || email == null || password == null || name.isBlank() || email.isBlank() || password.isBlank()) {
            sendError(exchange, 400, "All fields (Name, Email, Password) are required.");
            return;
        }

        User user = dataStore.registerUser(name, email, password);
        if (user == null) {
            sendError(exchange, 400, "An account with this email already exists.");
            return;
        }

        String token = dataStore.createSession(user);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("token", token);
        resp.put("user", Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole()
        ));
        sendJson(exchange, 201, resp);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            dataStore.removeSession(authHeader.substring(7).trim());
        }
        sendJson(exchange, 200, Map.of("success", true, "message", "Logged out successfully"));
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        User user = getAuthUser(exchange);
        if (user == null) {
            sendError(exchange, 401, "Not logged in");
            return;
        }
        sendJson(exchange, 200, Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole()
        ));
    }

    private void handlePackages(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method)) {
            // Optional query param search & filter
            String query = exchange.getRequestURI().getQuery();
            List<TourPackage> list = dataStore.getAllPackages();

            if (query != null) {
                Map<String, String> qp = parseQuery(query);
                String search = qp.get("search");
                String category = qp.get("category");

                if (search != null && !search.isBlank()) {
                    String sLower = search.toLowerCase();
                    list = list.stream().filter(p -> 
                        p.getDestination().toLowerCase().contains(sLower) ||
                        p.getDescription().toLowerCase().contains(sLower)
                    ).toList();
                }

                if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
                    list = list.stream().filter(p -> 
                        category.equalsIgnoreCase(p.getCategory())
                    ).toList();
                }
            }

            List<Map<String, Object>> respList = new ArrayList<>();
            for (TourPackage p : list) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("destination", p.getDestination());
                m.put("duration", p.getDuration());
                m.put("price", p.getPrice());
                m.put("rating", p.getRating());
                m.put("category", p.getCategory());
                m.put("description", p.getDescription());
                m.put("imageUrl", p.getImageUrl());
                m.put("inclusions", p.getInclusions());
                respList.add(m);
            }
            sendJson(exchange, 200, respList);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            User user = getAuthUser(exchange);
            if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
                sendError(exchange, 403, "Admin authorization required to add packages.");
                return;
            }

            Map<String, Object> body = JsonUtils.parseObject(readBody(exchange));
            String dest = (String) body.get("destination");
            String duration = (String) body.get("duration");
            Number priceNum = (Number) body.get("price");
            Number ratingNum = (Number) body.get("rating");
            String category = (String) body.get("category");
            String desc = (String) body.get("description");
            String img = (String) body.get("imageUrl");
            String inc = (String) body.get("inclusions");

            if (dest == null || dest.isBlank() || priceNum == null) {
                sendError(exchange, 400, "Destination and Price are required.");
                return;
            }

            double price = priceNum.doubleValue();
            double rating = ratingNum != null ? ratingNum.doubleValue() : 4.8;
            TourPackage pkg = dataStore.addPackage(dest, duration, price, rating, category, desc, img, inc);
            sendJson(exchange, 201, pkg);
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            User user = getAuthUser(exchange);
            if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
                sendError(exchange, 403, "Admin authorization required to delete packages.");
                return;
            }

            String[] parts = path.split("/");
            if (parts.length >= 4) {
                try {
                    int id = Integer.parseInt(parts[3]);
                    boolean deleted = dataStore.deletePackage(id);
                    if (deleted) {
                        sendJson(exchange, 200, Map.of("success", true, "message", "Package removed successfully"));
                    } else {
                        sendError(exchange, 404, "Package not found");
                    }
                    return;
                } catch (NumberFormatException ignored) {}
            }
            sendError(exchange, 400, "Invalid package ID");
            return;
        }

        sendError(exchange, 405, "Method not allowed");
    }

    private void handleBookings(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        User user = getAuthUser(exchange);

        if ("GET".equalsIgnoreCase(method)) {
            if (user == null) {
                sendError(exchange, 401, "Please log in to view your bookings.");
                return;
            }

            List<Booking> list;
            if ("admin".equalsIgnoreCase(user.getRole())) {
                list = dataStore.getAllBookings();
            } else {
                list = dataStore.getBookingsByEmail(user.getEmail());
            }

            List<Map<String, Object>> respList = new ArrayList<>();
            for (Booking b : list) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", b.getId());
                m.put("customer", b.getCustomer());
                m.put("phone", b.getPhone());
                m.put("userEmail", b.getUserEmail());
                m.put("destination", b.getDestination());
                m.put("packageId", b.getPackageId());
                m.put("travelDate", b.getTravelDate());
                m.put("travellers", b.getTravellers());
                m.put("amount", b.getAmount());
                m.put("status", b.getStatus());
                m.put("bookedAt", b.getBookedAt());
                respList.add(m);
            }
            sendJson(exchange, 200, respList);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            if (path.endsWith("/cancel")) {
                // Cancel booking
                if (user == null) {
                    sendError(exchange, 401, "Login required to cancel bookings.");
                    return;
                }
                Map<String, Object> body = JsonUtils.parseObject(readBody(exchange));
                String bId = (String) body.get("id");
                boolean ok = dataStore.cancelBooking(bId, user.getEmail(), "admin".equalsIgnoreCase(user.getRole()));
                if (ok) {
                    sendJson(exchange, 200, Map.of("success", true, "message", "Booking cancelled successfully"));
                } else {
                    sendError(exchange, 400, "Unable to cancel booking. It may not exist or belongs to another user.");
                }
                return;
            }

            if (path.endsWith("/status")) {
                // Admin change status
                if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
                    sendError(exchange, 403, "Admin authorization required.");
                    return;
                }
                Map<String, Object> body = JsonUtils.parseObject(readBody(exchange));
                String bId = (String) body.get("id");
                String newStatus = (String) body.get("status");
                boolean ok = dataStore.updateBookingStatus(bId, newStatus);
                if (ok) {
                    sendJson(exchange, 200, Map.of("success", true, "message", "Status updated to " + newStatus));
                } else {
                    sendError(exchange, 404, "Booking not found.");
                }
                return;
            }

            // Create new booking
            Map<String, Object> body = JsonUtils.parseObject(readBody(exchange));
            String customer = (String) body.get("customer");
            String phone = (String) body.get("phone");
            String travelDate = (String) body.get("travelDate");
            Number travellersNum = (Number) body.get("travellers");
            Number pkgIdNum = (Number) body.get("packageId");

            if (customer == null || phone == null || travelDate == null || pkgIdNum == null ||
                customer.isBlank() || phone.isBlank() || travelDate.isBlank()) {
                sendError(exchange, 400, "Customer Name, Phone, Travel Date, and Package are required.");
                return;
            }

            int travellers = travellersNum != null ? travellersNum.intValue() : 1;
            int pkgId = pkgIdNum.intValue();
            String email = (user != null) ? user.getEmail() : (String) body.getOrDefault("email", "guest@gmail.com");

            Booking booking = dataStore.createBooking(customer, phone, email, pkgId, travelDate, travellers);
            if (booking != null) {
                sendJson(exchange, 201, Map.of(
                    "success", true,
                    "message", "Package Booking Confirmed Successfully!",
                    "booking", booking
                ));
            } else {
                sendError(exchange, 400, "Selected tour package not found.");
            }
            return;
        }

        sendError(exchange, 405, "Method not allowed");
    }

    private void handleReviews(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            List<Review> list = dataStore.getAllReviews();
            double avg = dataStore.getAverageRating();

            List<Map<String, Object>> rList = new ArrayList<>();
            for (Review r : list) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("user", r.getUser());
                m.put("destination", r.getDestination());
                m.put("rating", r.getRating());
                m.put("comment", r.getComment());
                m.put("createdAt", r.getCreatedAt());
                rList.add(m);
            }

            sendJson(exchange, 200, Map.of(
                "reviews", rList,
                "averageRating", avg,
                "totalReviews", list.size()
            ));
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            Map<String, Object> body = JsonUtils.parseObject(readBody(exchange));
            String user = (String) body.get("user");
            String dest = (String) body.get("destination");
            Number ratingNum = (Number) body.get("rating");
            String comment = (String) body.get("comment");

            if (user == null || ratingNum == null || comment == null || user.isBlank() || comment.isBlank()) {
                sendError(exchange, 400, "Please provide your Name, Star Rating (1-5), and Review comment.");
                return;
            }

            int rating = ratingNum.intValue();
            if (rating < 1 || rating > 5) {
                sendError(exchange, 400, "Rating must be between 1 and 5 stars.");
                return;
            }

            if (dest == null || dest.isBlank()) dest = "General Experience";

            Review rev = dataStore.addReview(user, dest, rating, comment);
            sendJson(exchange, 201, Map.of(
                "success", true,
                "message", "Review added successfully! Thank you for your feedback.",
                "review", rev
            ));
            return;
        }

        sendError(exchange, 405, "Method not allowed");
    }

    private void handleAdminStats(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        User user = getAuthUser(exchange);
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            sendError(exchange, 403, "Admin authorization required.");
            return;
        }
        sendJson(exchange, 200, dataStore.getAdminStats());
    }

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }

        // Security check against directory traversal
        if (path.contains("..")) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        Path file = Paths.get("src", "main", "resources", "static", path.substring(1));
        if (!Files.exists(file) || Files.isDirectory(file)) {
            // Fallback to index.html for SPA routing
            file = Paths.get("src", "main", "resources", "static", "index.html");
        }

        if (!Files.exists(file)) {
            sendError(exchange, 404, "Page Not Found");
            return;
        }

        String mime = getMimeType(file.getFileName().toString());
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                try {
                    map.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            }
        }
        return map;
    }
}
