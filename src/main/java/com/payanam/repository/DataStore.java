package com.payanam.repository;

import com.payanam.model.*;
import com.payanam.util.JsonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DataStore {
    private static DataStore instance;

    private final List<User> users = new ArrayList<>();
    private final List<TourPackage> packages = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();
    private final Map<String, User> activeSessions = new ConcurrentHashMap<>();
    private final AtomicInteger packageIdSeq = new AtomicInteger(100);
    private final AtomicInteger bookingIdSeq = new AtomicInteger(1000);

    private final Path dbPath = Paths.get("data", "database.json");

    private DataStore() {
        loadData();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public synchronized void seedDefaultData() {
        users.clear();
        packages.clear();
        bookings.clear();
        reviews.clear();

        // 1. Users (Admin + Demo Tourist)
        users.add(new User("usr-1", "Admin", "admin@gmail.com", "admin123", "admin", "2026-09-01 10:00"));
        users.add(new User("usr-2", "Rahul Sharma", "rahul@gmail.com", "rahul123", "tourist", "2026-09-05 14:30"));
        users.add(new User("usr-3", "Pradhiksha G", "pradhi@gmail.com", "pradhi123", "tourist", "2026-09-10 09:15"));

        // 2. Packages (Original Payanam destinations + Enhanced Packages)
        packages.add(new TourPackage(
            1, "Goa", "4 Days / 3 Nights", 12000.0, 4.8, "Beach",
            "Sun-kissed beaches, thrilling water sports, historic Portuguese forts, vibrant nightlife, and luxury sunset catamaran cruises.",
            "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=800&auto=format&fit=crop&q=80",
            "Beachfront Resort, Breakfast, Calangute & Baga Tour, Scuba Diving Session, Sunset Cruise"
        ));

        packages.add(new TourPackage(
            2, "Ooty", "3 Days / 2 Nights", 16500.0, 4.5, "Hill Station",
            "Queen of Nilgiri Hill Stations with rolling emerald tea gardens, scenic Nilgiri Mountain Toy Train, and Botanical marvels.",
            "https://images.unsplash.com/photo-1589182373726-e4f658ab50f0?w=800&auto=format&fit=crop&q=80",
            "Heritage Hillside Villa, Daily Meals, Botanical Garden Pass, Tea Factory Tasting, Toy Train Ride"
        ));

        packages.add(new TourPackage(
            3, "Manali", "5 Days / 4 Nights", 15000.0, 4.9, "Adventure",
            "Snow-clad Himalayan peaks, adventure in Solang Valley, Rohtang Pass excursions, hot springs, and Beas river rafting.",
            "https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?w=800&auto=format&fit=crop&q=80",
            "4-Star Mountain View Hotel, Breakfast & Dinner, Solang Valley Cab, River Rafting Gear, Paragliding"
        ));

        packages.add(new TourPackage(
            4, "Kerala", "6 Days / 5 Nights", 9000.0, 4.6, "Backwaters",
            "God\'s Own Country featuring serene Alleppey backwater houseboats, Munnar cloud-capped tea estates, and Kochi spice bazaars.",
            "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=800&auto=format&fit=crop&q=80",
            "Deluxe Alleppey Houseboat Stay, Munnar Tea Valley Cottage, Authentic Kerala Meals, Kathakali Show"
        ));

        packages.add(new TourPackage(
            5, "Ladakh", "7 Days / 6 Nights", 24000.0, 4.9, "Adventure",
            "The land of high passes, mesmerizing azure Pangong Tso Lake, ancient Buddhist monasteries, and dramatic high-altitude desert dunes.",
            "https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?w=800&auto=format&fit=crop&q=80",
            "Luxury Glamping Tents, 4x4 Vehicle with Permit, Oxygen Cylinder Support, Pangong Lake & Nubra Excursions"
        ));

        packages.add(new TourPackage(
            6, "Jaipur", "3 Days / 2 Nights", 11500.0, 4.7, "Heritage",
            "The royal Pink City with grand hilltop fortresses like Amber Fort, majestic palaces, and vibrant Rajasthani cultural bazaars.",
            "https://images.unsplash.com/photo-1599661046289-e31897846e41?w=800&auto=format&fit=crop&q=80",
            "Palace Heritage Stay, Royal Breakfast, Guided Fort & Palace Tours, Chokhi Dhani Cultural Dinner"
        ));

        // 3. Demo Bookings
        bookings.add(new Booking(
            "BK-1001", "Rahul Sharma", "9876543210", "rahul@gmail.com", "Goa",
            1, "2026-10-15", 2, 24000.0, "Confirmed", "2026-09-12 11:20"
        ));

        bookings.add(new Booking(
            "BK-1002", "Pradhiksha G", "9840123456", "pradhi@gmail.com", "Kerala",
            4, "2026-11-01", 3, 27000.0, "Confirmed", "2026-09-18 16:45"
        ));

        // 4. Demo Reviews
        reviews.add(new Review("REV-1", "Rahul Sharma", "Goa", 5, 
            "Wonderful experience! The catamaran sunset cruise and private beach resort organized by Payanam were top-notch.", 
            "2026-09-14 18:30"));
        reviews.add(new Review("REV-2", "Pradhiksha G", "Kerala", 5, 
            "The houseboat stay across Alleppey backwaters was magical. Food was authentic and the staff was super courteous!", 
            "2026-09-20 12:10"));
        reviews.add(new Review("REV-3", "Vikram S", "Manali", 5, 
            "Snowcapped mountains, paragliding in Solang Valley, and riverside bonfire. Best tour of my life!", 
            "2026-09-22 09:40"));
        reviews.add(new Review("REV-4", "Ragavi M", "Ooty", 4, 
            "Scenic tea estate tour and pleasant weather. Toy train tickets were arranged smoothly without any hassle.", 
            "2026-09-23 15:20"));

        saveData();
    }

    public synchronized void saveData() {
        try {
            if (!Files.exists(dbPath.getParent())) {
                Files.createDirectories(dbPath.getParent());
            }

            Map<String, Object> root = new LinkedHashMap<>();

            // Serialize users
            List<Map<String, Object>> uList = new ArrayList<>();
            for (User u : users) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", u.getId());
                m.put("name", u.getName());
                m.put("email", u.getEmail());
                m.put("password", u.getPassword());
                m.put("role", u.getRole());
                m.put("createdAt", u.getCreatedAt());
                uList.add(m);
            }
            root.put("users", uList);

            // Serialize packages
            List<Map<String, Object>> pList = new ArrayList<>();
            for (TourPackage p : packages) {
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
                pList.add(m);
            }
            root.put("packages", pList);

            // Serialize bookings
            List<Map<String, Object>> bList = new ArrayList<>();
            for (Booking b : bookings) {
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
                bList.add(m);
            }
            root.put("bookings", bList);

            // Serialize reviews
            List<Map<String, Object>> rList = new ArrayList<>();
            for (Review r : reviews) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("user", r.getUser());
                m.put("destination", r.getDestination());
                m.put("rating", r.getRating());
                m.put("comment", r.getComment());
                m.put("createdAt", r.getCreatedAt());
                rList.add(m);
            }
            root.put("reviews", rList);

            String json = JsonUtils.toJson(root);
            Files.writeString(dbPath, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Error saving database: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadData() {
        if (!Files.exists(dbPath)) {
            seedDefaultData();
            return;
        }

        try {
            String json = Files.readString(dbPath, StandardCharsets.UTF_8);
            Map<String, Object> root = JsonUtils.parseObject(json);

            users.clear();
            packages.clear();
            bookings.clear();
            reviews.clear();

            // Load users
            List<?> uList = (List<?>) root.get("users");
            if (uList != null) {
                for (Object item : uList) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    users.add(new User(
                        (String) m.get("id"),
                        (String) m.get("name"),
                        (String) m.get("email"),
                        (String) m.get("password"),
                        (String) m.get("role"),
                        (String) m.get("createdAt")
                    ));
                }
            }

            // Load packages
            List<?> pList = (List<?>) root.get("packages");
            int maxPId = 10;
            if (pList != null) {
                for (Object item : pList) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    int id = ((Number) m.get("id")).intValue();
                    if (id > maxPId) maxPId = id;
                    packages.add(new TourPackage(
                        id,
                        (String) m.get("destination"),
                        (String) m.get("duration"),
                        ((Number) m.get("price")).doubleValue(),
                        ((Number) m.get("rating")).doubleValue(),
                        (String) m.get("category"),
                        (String) m.get("description"),
                        (String) m.get("imageUrl"),
                        (String) m.get("inclusions")
                    ));
                }
            }
            packageIdSeq.set(maxPId + 1);

            // Load bookings
            List<?> bList = (List<?>) root.get("bookings");
            int maxBId = 1000;
            if (bList != null) {
                for (Object item : bList) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    String idStr = (String) m.get("id");
                    if (idStr != null && idStr.startsWith("BK-")) {
                        try {
                            int num = Integer.parseInt(idStr.substring(3));
                            if (num > maxBId) maxBId = num;
                        } catch (Exception ignored) {}
                    }
                    bookings.add(new Booking(
                        idStr,
                        (String) m.get("customer"),
                        (String) m.get("phone"),
                        (String) m.get("userEmail"),
                        (String) m.get("destination"),
                        ((Number) m.get("packageId")).intValue(),
                        (String) m.get("travelDate"),
                        ((Number) m.get("travellers")).intValue(),
                        ((Number) m.get("amount")).doubleValue(),
                        (String) m.get("status"),
                        (String) m.get("bookedAt")
                    ));
                }
            }
            bookingIdSeq.set(maxBId + 1);

            // Load reviews
            List<?> rList = (List<?>) root.get("reviews");
            if (rList != null) {
                for (Object item : rList) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    reviews.add(new Review(
                        (String) m.get("id"),
                        (String) m.get("user"),
                        (String) m.get("destination"),
                        ((Number) m.get("rating")).intValue(),
                        (String) m.get("comment"),
                        (String) m.get("createdAt")
                    ));
                }
            }

            if (packages.isEmpty()) {
                seedDefaultData();
            }
        } catch (Exception e) {
            System.err.println("Could not parse database.json, seeding defaults: " + e.getMessage());
            seedDefaultData();
        }
    }

    // --- Authentication & User Operations ---
    public synchronized User findUserByEmail(String email) {
        if (email == null) return null;
        for (User u : users) {
            if (email.equalsIgnoreCase(u.getEmail())) {
                return u;
            }
        }
        return null;
    }

    public synchronized User registerUser(String name, String email, String password) {
        if (findUserByEmail(email) != null) {
            return null; // Email already exists
        }
        String id = "usr-" + (users.size() + 1);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        User user = new User(id, name, email, password, "tourist", now);
        users.add(user);
        saveData();
        return user;
    }

    public synchronized String createSession(User user) {
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, user);
        return token;
    }

    public User getSessionUser(String token) {
        if (token == null) return null;
        return activeSessions.get(token);
    }

    public void removeSession(String token) {
        if (token != null) activeSessions.remove(token);
    }

    // --- Tour Packages Operations ---
    public synchronized List<TourPackage> getAllPackages() {
        return new ArrayList<>(packages);
    }

    public synchronized TourPackage getPackageById(int id) {
        for (TourPackage p : packages) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    public synchronized TourPackage addPackage(String destination, String duration, double price, 
                                              double rating, String category, String description, 
                                              String imageUrl, String inclusions) {
        int id = packageIdSeq.getAndIncrement();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&auto=format&fit=crop&q=80";
        }
        if (duration == null || duration.trim().isEmpty()) {
            duration = "3 Days / 2 Nights";
        }
        if (category == null || category.trim().isEmpty()) {
            category = "General";
        }
        TourPackage pkg = new TourPackage(id, destination, duration, price, rating, category, description, imageUrl, inclusions);
        packages.add(pkg);
        saveData();
        return pkg;
    }

    public synchronized boolean deletePackage(int id) {
        boolean removed = packages.removeIf(p -> p.getId() == id);
        if (removed) saveData();
        return removed;
    }

    // --- Bookings Operations ---
    public synchronized List<Booking> getAllBookings() {
        return new ArrayList<>(bookings);
    }

    public synchronized List<Booking> getBookingsByEmail(String email) {
        List<Booking> list = new ArrayList<>();
        if (email == null) return list;
        for (Booking b : bookings) {
            if (email.equalsIgnoreCase(b.getUserEmail())) {
                list.add(b);
            }
        }
        return list;
    }

    public synchronized Booking createBooking(String customer, String phone, String email, 
                                              int packageId, String travelDate, int travellers) {
        TourPackage pkg = getPackageById(packageId);
        if (pkg == null) return null;

        String id = "BK-" + bookingIdSeq.getAndIncrement();
        double totalAmount = pkg.getPrice() * Math.max(1, travellers);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Booking booking = new Booking(
            id, customer, phone, email, pkg.getDestination(), packageId,
            travelDate, travellers, totalAmount, "Confirmed", now
        );
        bookings.add(0, booking); // Latest first
        saveData();
        return booking;
    }

    public synchronized boolean cancelBooking(String bookingId, String userEmail, boolean isAdmin) {
        for (Booking b : bookings) {
            if (b.getId().equalsIgnoreCase(bookingId)) {
                if (isAdmin || (userEmail != null && userEmail.equalsIgnoreCase(b.getUserEmail()))) {
                    b.setStatus("Cancelled");
                    saveData();
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized boolean updateBookingStatus(String bookingId, String newStatus) {
        for (Booking b : bookings) {
            if (b.getId().equalsIgnoreCase(bookingId)) {
                b.setStatus(newStatus);
                saveData();
                return true;
            }
        }
        return false;
    }

    // --- Reviews Operations ---
    public synchronized List<Review> getAllReviews() {
        return new ArrayList<>(reviews);
    }

    public synchronized Review addReview(String user, String destination, int rating, String comment) {
        String id = "REV-" + (reviews.size() + 1);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Review r = new Review(id, user, destination, rating, comment, now);
        reviews.add(0, r); // Latest first
        saveData();
        return r;
    }

    public synchronized double getAverageRating() {
        if (reviews.isEmpty()) return 5.0;
        double sum = 0;
        for (Review r : reviews) sum += r.getRating();
        return Math.round((sum / reviews.size()) * 10.0) / 10.0;
    }

    // --- Admin Stats ---
    public synchronized Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        double totalRevenue = 0;
        int confirmedBookings = 0;
        for (Booking b : bookings) {
            if (!"Cancelled".equalsIgnoreCase(b.getStatus())) {
                totalRevenue += b.getAmount();
                confirmedBookings++;
            }
        }
        stats.put("totalBookings", bookings.size());
        stats.put("confirmedBookings", confirmedBookings);
        stats.put("totalPackages", packages.size());
        stats.put("totalUsers", users.size());
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageRating", getAverageRating());
        return stats;
    }
}
