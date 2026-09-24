# 🧭 Payanam (பயணம்) — Tourist Booking Web Application

> **Modern full-stack Java web platform for exploring travel destinations, booking custom tours, managing customer itineraries, and collecting verified community traveler reviews.**

Re-engineered and modernized from the original Python Tkinter desktop application into an enterprise-ready, high-performance Java 21 web application with a responsive web frontend and REST API architecture.

---

## 🚀 Key Features

### 1. 🌍 Explore Destinations & Packages
- Pre-seeded with signature tours:
  - **Goa**: Beach Paradise & Sunset Catamaran Cruises (4 Days / 3 Nights, ₹12,000, 4.8★)
  - **Ooty**: Queen of Nilgiri Hill Stations & Toy Train (3 Days / 2 Nights, ₹16,500, 4.5★)
  - **Manali**: Snow Peaks & Solang Valley Adventure (5 Days / 4 Nights, ₹15,000, 4.9★)
  - **Kerala**: Backwaters & Alleppey Houseboat Stay (6 Days / 5 Nights, ₹9,000, 4.6★)
  - **Ladakh**: High Himalayan Passes & Pangong Lake (7 Days / 6 Nights, ₹24,000, 4.9★)
  - **Jaipur**: Royal Forts, Amber Palace & Cultural Safari (3 Days / 2 Nights, ₹11,500, 4.7★)
- **Live Search**: Instant keyword search for destinations and attractions.
- **Category Filter**: Beach, Hill Station, Adventure, Backwaters, Heritage.
- **Sorting**: Recommended, Price (Low to High, High to Low), Top-Rated.

### 2. 📝 Interactive Booking System
- Dynamic multi-traveler price computation ($$\text{Total} = \text{Base Price} \times \text{Travellers}$$).
- Passenger details (Name, Phone number, Travel date picker).
- Instant booking confirmation modal with ticket breakdown and reference ID (`BK-XXXX`).

### 3. 🧳 "My Trips" Dashboard
- View all past and upcoming reservations for the logged-in traveler.
- Status badges: **Confirmed** (🟢), **Completed** (🔵), **Cancelled** (🔴).
- Self-service **"Cancel Trip"** action with real-time state synchronization.
- Quick **"Review"** shortcut directly linked to the booked journey.

### 4. ⭐ Reviews & Community Feed
- Interactive 5-star rating submission with traveler name, destination tag, and feedback.
- Real-time aggregation of the overall traveler score (e.g. 4.8 / 5.0).
- Community review cards featuring verified traveler badges.

### 5. 🛡️ Administrator Portal
- **Live Executive Metrics**: Total Bookings, Active Tours, Revenue Generated (₹), Registered Users.
- **Package Management**: Add new packages (Destination, Duration, Price, Image URL, Description) or Delete existing packages.
- **Reservation Management**: View all customer bookings and update reservation statuses (Confirmed / Completed / Cancelled).

### 6. 🔐 Authentication & Roles
- **Role-Based Access Control**: Tourist vs Admin.
- Pre-configured demo accounts for 1-click testing:
  - **Administrator**: `admin@gmail.com` / `admin123`
  - **Tourist**: `rahul@gmail.com` / `rahul123`
  - **Tourist**: `pradhi@gmail.com` / `pradhi123`

### 7. 💾 Thread-Safe Persistent Storage
- In-memory data store with automatic file persistence to `data/database.json`.
- All updates (new packages, bookings, reviews, registrations) survive server restarts without external database setup.

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| **Backend Language** | Java 21 (LTS) |
| **Server Engine** | `com.sun.net.httpserver.HttpServer` with Java 21 Virtual Threads |
| **Architecture** | Layered Model-Repository-Service-Server REST Architecture |
| **Build & Dependency Tool** | Standard Maven (`pom.xml`) + Standalone Java Batch Launchers |
| **Frontend** | Modern HTML5, Responsive CSS3 (Glassmorphism & Flexbox/Grid), ES6 JavaScript |
| **Database** | Persistent JSON DataStore with atomic file synchronization |

---

## ⚡ Quick Start Guide

### Prerequisites
- **Java 21** (or Java 17+) installed. Check with:
  ```bash
  java -version
  ```

### Option 1: One-Click Run (Windows)
Double-click `run.bat` or run in PowerShell / Command Prompt:
```cmd
.\run.bat
```
This compiles all Java source files and starts the server at [http://localhost:8080](http://localhost:8080).

### Option 2: Linux / macOS
```bash
chmod +x run.sh
./run.sh
```

### Option 3: Standard Maven Build
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.payanam.Main"
```

---

## 🌐 REST API Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/login` | Authenticate and obtain session token | Public |
| `POST` | `/api/auth/register` | Register new tourist account | Public |
| `GET` | `/api/auth/me` | Get logged-in user profile | Authenticated |
| `GET` | `/api/packages` | List tour packages (with search & filter) | Public |
| `POST` | `/api/packages` | Create a new tour package | Admin |
| `DELETE` | `/api/packages/{id}` | Delete a tour package | Admin |
| `GET` | `/api/bookings` | Get bookings (user's or all for admin) | Authenticated |
| `POST` | `/api/bookings` | Book a tour package | Public / Authenticated |
| `POST` | `/api/bookings/cancel` | Cancel a reservation | Authenticated |
| `POST` | `/api/bookings/status` | Update reservation status | Admin |
| `GET` | `/api/reviews` | Get all reviews & average score | Public |
| `POST` | `/api/reviews` | Submit a new review | Public |
| `GET` | `/api/admin/stats` | Get admin metrics & revenue | Admin |

---

## 📦 How to Push to Your New GitHub Repository

To upload this clean Java repository to your own GitHub account:

1. Create a **new repository** on GitHub (e.g. named `Payanam-Java` or `payanam-travel`).
2. Open terminal in this folder (`c:\Users\pradh\Desktop\Antigravity\Payanam-Java`) and run:

```bash
git init
git add .
git commit -m "Initial commit: Payanam Java tourist booking web platform"
git branch -M main
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/YOUR_NEW_REPO.git
git push -u origin main
```

---

## 👥 Contributors & Credits
- **Original Concept**: Payanam (Python Tkinter desktop system by Vikram & Collaborators)
- **Java Modernization**: Pradhiksha G & Antigravity
