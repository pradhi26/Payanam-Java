// --- Payanam Client-Side Application Controller ---

const state = {
    user: null,
    token: localStorage.getItem('payanam_token') || null,
    packages: [],
    bookings: [],
    reviews: [],
    activeTab: 'explore',
    selectedPackage: null,
    searchDebounceTimer: null
};

// --- Initialization ---
document.addEventListener('DOMContentLoaded', async () => {
    // Set minimum travel date for booking modal to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dateInput = document.getElementById('book-date');
    if (dateInput) {
        dateInput.min = tomorrow.toISOString().split('T')[0];
        dateInput.value = tomorrow.toISOString().split('T')[0];
    }

    // Check stored session
    if (state.token) {
        await checkCurrentUser();
    } else {
        updateAuthUI();
    }

    // Load initial data
    loadPackages();
    loadReviews();
});

// --- Tab Switching ---
function switchTab(tabName) {
    state.activeTab = tabName;

    // Update buttons
    document.querySelectorAll('.nav-link').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.getElementById(`tab-btn-${tabName}`);
    if (activeBtn) activeBtn.classList.add('active');

    // Update panes
    document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));
    const activePane = document.getElementById(`tab-${tabName}`);
    if (activePane) activePane.classList.add('active');

    // Tab-specific refreshes
    if (tabName === 'mytrips') {
        if (!state.user) {
            showToast('Please log in to view your bookings', 'info');
            openModal('modal-login');
        } else {
            loadBookings();
        }
    } else if (tabName === 'admin') {
        if (!state.user || state.user.role !== 'admin') {
            showToast('Admin authorization required', 'error');
            switchTab('explore');
        } else {
            loadAdminStats();
            loadAdminBookings();
        }
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// --- Authentication ---
async function checkCurrentUser() {
    try {
        const res = await fetch('/api/auth/me', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (res.ok) {
            state.user = await res.json();
            updateAuthUI();
        } else {
            logout();
        }
    } catch (e) {
        console.error('Session verify failed:', e);
        logout();
    }
}

function updateAuthUI() {
    const loggedOutEl = document.getElementById('auth-logged-out');
    const loggedInEl = document.getElementById('auth-logged-in');
    const adminTabBtn = document.getElementById('tab-btn-admin');

    if (state.user) {
        loggedOutEl.style.display = 'none';
        loggedInEl.style.display = 'flex';

        document.getElementById('nav-user-name').textContent = state.user.name;
        document.getElementById('nav-user-role').textContent = state.user.role;
        document.getElementById('nav-user-avatar').textContent = state.user.name.charAt(0).toUpperCase();

        if (state.user.role === 'admin') {
            adminTabBtn.style.display = 'inline-flex';
        } else {
            adminTabBtn.style.display = 'none';
        }
    } else {
        loggedOutEl.style.display = 'flex';
        loggedInEl.style.display = 'none';
        adminTabBtn.style.display = 'none';
    }
}

async function handleLoginSubmit(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value.trim();
    const btn = document.getElementById('btn-login-submit');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Logging in...';

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            state.token = data.token;
            state.user = data.user;
            localStorage.setItem('payanam_token', data.token);
            updateAuthUI();
            closeModal('modal-login');
            showToast(`Welcome back, ${data.user.name}!`, 'success');

            if (data.user.role === 'admin') {
                switchTab('admin');
            }
        } else {
            showToast(data.message || 'Login failed. Check credentials.', 'error');
        }
    } catch (err) {
        showToast('Server connection error. Please retry.', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-arrow-right"></i> Login';
    }
}

async function handleRegisterSubmit(e) {
    e.preventDefault();
    const name = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value.trim();
    const btn = document.getElementById('btn-reg-submit');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Registering...';

    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            state.token = data.token;
            state.user = data.user;
            localStorage.setItem('payanam_token', data.token);
            updateAuthUI();
            closeModal('modal-register');
            showToast(`Account created! Welcome, ${data.user.name}.`, 'success');
        } else {
            showToast(data.message || 'Registration failed.', 'error');
        }
    } catch (err) {
        showToast('Server connection error. Please retry.', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-check"></i> Register';
    }
}

function fillLogin(email, password) {
    document.getElementById('login-email').value = email;
    document.getElementById('login-password').value = password;
}

function logout() {
    if (state.token) {
        fetch('/api/auth/logout', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        }).catch(() => {});
    }
    state.user = null;
    state.token = null;
    localStorage.removeItem('payanam_token');
    updateAuthUI();
    showToast('Logged out safely.', 'info');
    switchTab('explore');
}

// --- Tour Packages ---
async function loadPackages() {
    try {
        const res = await fetch('/api/packages');
        if (res.ok) {
            state.packages = await res.json();
            renderPackages(state.packages);
            renderAdminPackagesTable(state.packages);
            document.getElementById('stat-package-count').textContent = state.packages.length;
        }
    } catch (e) {
        console.error('Error fetching packages:', e);
    }
}

function renderPackages(list) {
    const grid = document.getElementById('packages-grid');
    if (!list || list.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align:center; padding:50px 20px;">
                <i class="fa-solid fa-compass" style="font-size:2.5rem; color:#94a3b8; margin-bottom:12px;"></i>
                <h3 style="color:#1e293b;">No Packages Found</h3>
                <p style="color:#64748b;">Try adjusting your search keyword or category filter.</p>
            </div>`;
        return;
    }

    grid.innerHTML = list.map(pkg => `
        <div class="package-card">
            <div class="package-media">
                <img src="${pkg.imageUrl}" alt="${pkg.destination}" onerror="this.src='https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&auto=format&fit=crop&q=80'">
                <div class="package-badges">
                    <span class="badge badge-category">${pkg.category || 'Tour'}</span>
                    <span class="badge badge-rating"><i class="fa-solid fa-star" style="color:#f59e0b;"></i> ${pkg.rating.toFixed(1)}</span>
                </div>
            </div>
            <div class="package-content">
                <div class="package-header">
                    <h3 class="package-dest">${pkg.destination}</h3>
                </div>
                <div class="package-duration">
                    <i class="fa-regular fa-clock"></i> ${pkg.duration}
                </div>
                <p class="package-desc">${pkg.description || 'Experience the authentic culture, scenery and beauty of ' + pkg.destination + '.'}</p>
                
                ${pkg.inclusions ? `
                    <div class="package-inclusions">
                        ${pkg.inclusions.split(',').slice(0, 3).map(inc => `<span class="inc-tag"><i class="fa-solid fa-check"></i> ${inc.trim()}</span>`).join('')}
                    </div>` : ''}

                <div class="package-footer">
                    <div class="package-price-wrap">
                        <span class="price-label">Starting From</span>
                        <div class="price-val">₹${pkg.price.toLocaleString('en-IN')} <small>/ person</small></div>
                    </div>
                    <button class="btn btn-primary" onclick="initiateBooking(${pkg.id})">
                        <i class="fa-solid fa-paper-plane"></i> Book Now
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

function debounceSearch() {
    clearTimeout(state.searchDebounceTimer);
    state.searchDebounceTimer = setTimeout(() => {
        filterPackages();
    }, 250);
}

function filterPackages() {
    const searchVal = document.getElementById('search-input').value.toLowerCase().trim();
    const catVal = document.getElementById('category-filter').value;
    const sortVal = document.getElementById('sort-filter').value;

    let filtered = state.packages.filter(p => {
        const matchesSearch = !searchVal || 
                              p.destination.toLowerCase().includes(searchVal) ||
                              (p.description && p.description.toLowerCase().includes(searchVal));
        const matchesCat = catVal === 'all' || (p.category && p.category.toLowerCase() === catVal.toLowerCase());
        return matchesSearch && matchesCat;
    });

    if (sortVal === 'price-low') {
        filtered.sort((a, b) => a.price - b.price);
    } else if (sortVal === 'price-high') {
        filtered.sort((a, b) => b.price - a.price);
    } else if (sortVal === 'rating') {
        filtered.sort((a, b) => b.rating - a.rating);
    }

    renderPackages(filtered);
}

// --- Booking Flow ---
function initiateBooking(pkgId) {
    const pkg = state.packages.find(p => p.id === pkgId);
    if (!pkg) return;

    state.selectedPackage = pkg;

    // Populate modal
    document.getElementById('booking-pkg-img').src = pkg.imageUrl;
    document.getElementById('booking-pkg-cat').textContent = pkg.category || 'Tour';
    document.getElementById('booking-pkg-title').textContent = pkg.destination;
    document.getElementById('booking-pkg-duration').textContent = pkg.duration;
    document.getElementById('booking-pkg-rating').textContent = pkg.rating.toFixed(1);
    document.getElementById('booking-pkg-base-price').textContent = pkg.price.toLocaleString('en-IN');

    // Pre-fill user details if logged in
    if (state.user) {
        document.getElementById('book-name').value = state.user.name;
    }

    // Reset stepper
    document.getElementById('book-travellers').value = 1;
    updatePriceCalc();

    openModal('modal-booking');
}

function adjustTravellers(delta) {
    const input = document.getElementById('book-travellers');
    let val = parseInt(input.value) || 1;
    val = Math.max(1, Math.min(20, val + delta));
    input.value = val;
    updatePriceCalc();
}

function updatePriceCalc() {
    if (!state.selectedPackage) return;
    const count = parseInt(document.getElementById('book-travellers').value) || 1;
    const base = state.selectedPackage.price;
    const total = base * count;

    document.getElementById('calc-base').textContent = base.toLocaleString('en-IN');
    document.getElementById('calc-count').textContent = count;
    document.getElementById('calc-total').textContent = total.toLocaleString('en-IN');
}

async function handleBookingSubmit(e) {
    e.preventDefault();
    if (!state.selectedPackage) return;

    const customer = document.getElementById('book-name').value.trim();
    const phone = document.getElementById('book-phone').value.trim();
    const travelDate = document.getElementById('book-date').value;
    const travellers = parseInt(document.getElementById('book-travellers').value) || 1;
    const btn = document.getElementById('btn-confirm-booking');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Processing...';

    const payload = {
        customer,
        phone,
        travelDate,
        travellers,
        packageId: state.selectedPackage.id
    };

    try {
        const headers = { 'Content-Type': 'application/json' };
        if (state.token) {
            headers['Authorization'] = `Bearer ${state.token}`;
        }

        const res = await fetch('/api/bookings', {
            method: 'POST',
            headers,
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (res.ok && data.success) {
            closeModal('modal-booking');

            // Show success modal
            document.getElementById('success-booking-id').textContent = data.booking.id;
            document.getElementById('success-dest').textContent = data.booking.destination;
            document.getElementById('success-date').textContent = data.booking.travelDate;
            document.getElementById('success-amount').textContent = '₹' + data.booking.amount.toLocaleString('en-IN');
            openModal('modal-booking-success');

            showToast('Reservation confirmed successfully!', 'success');
        } else {
            showToast(data.message || 'Booking failed. Try again.', 'error');
        }
    } catch (err) {
        showToast('Connection error. Could not book.', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-shield-check"></i> Confirm Reservation';
    }
}

// --- My Trips Tab ---
async function loadBookings() {
    if (!state.user) return;
    const container = document.getElementById('mytrips-container');
    container.innerHTML = '<div class="loading-spinner"><i class="fa-solid fa-circle-notch fa-spin"></i> Fetching your reservations...</div>';

    try {
        const res = await fetch('/api/bookings', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (res.ok) {
            state.bookings = await res.json();
            renderBookings(state.bookings);
        } else {
            container.innerHTML = '<p style="color:#ef4444;">Failed to load bookings. Please log in again.</p>';
        }
    } catch (e) {
        container.innerHTML = '<p style="color:#ef4444;">Connection error while fetching bookings.</p>';
    }
}

function renderBookings(list) {
    const container = document.getElementById('mytrips-container');
    if (!list || list.length === 0) {
        container.innerHTML = `
            <div style="background:white; border:1px solid #e2e8f0; border-radius:16px; text-align:center; padding:60px 20px;">
                <i class="fa-solid fa-suitcase-rolling" style="font-size:3rem; color:#94a3b8; margin-bottom:14px;"></i>
                <h3 style="color:#1e293b; font-size:1.4rem;">No Bookings Yet</h3>
                <p style="color:#64748b; margin:6px 0 20px;">You haven't reserved any journeys yet. Explore our packages and start your adventure!</p>
                <button class="btn btn-primary" onclick="switchTab('explore')">
                    <i class="fa-solid fa-compass"></i> Explore Packages
                </button>
            </div>`;
        return;
    }

    container.innerHTML = list.map(b => `
        <div class="trip-card">
            <div class="trip-main-info">
                <div class="trip-dest-icon">
                    <i class="fa-solid fa-location-dot"></i>
                </div>
                <div class="trip-details">
                    <div style="display:flex; align-items:center; gap:10px;">
                        <h3>${b.destination}</h3>
                        <span class="trip-status-pill status-${b.status.toLowerCase()}">${b.status}</span>
                    </div>
                    <div class="trip-meta-row">
                        <span><i class="fa-solid fa-hashtag"></i> <strong>${b.id}</strong></span>
                        <span><i class="fa-regular fa-calendar"></i> Travel: <strong>${b.travelDate}</strong></span>
                        <span><i class="fa-solid fa-user-group"></i> <strong>${b.travellers}</strong> Traveler${b.travellers > 1 ? 's' : ''}</span>
                        <span><i class="fa-solid fa-phone"></i> ${b.phone}</span>
                    </div>
                </div>
            </div>

            <div class="trip-price-actions">
                <div class="trip-amount-box">
                    <span>Total Amount</span>
                    <strong>₹${b.amount.toLocaleString('en-IN')}</strong>
                </div>

                <div style="display:flex; gap:8px;">
                    ${b.status === 'Confirmed' ? `
                        <button class="btn btn-outline" style="color:#ef4444; border-color:#fca5a5;" onclick="cancelTrip('${b.id}')">
                            <i class="fa-solid fa-ban"></i> Cancel Trip
                        </button>
                    ` : ''}
                    <button class="btn btn-primary" onclick="openReviewModalFor('${b.destination}')">
                        <i class="fa-solid fa-star"></i> Review
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

async function cancelTrip(bookingId) {
    if (!confirm(`Are you sure you want to cancel reservation ${bookingId}?`)) return;

    try {
        const res = await fetch('/api/bookings/cancel', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${state.token}`
            },
            body: JSON.stringify({ id: bookingId })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showToast('Booking cancelled successfully', 'info');
            loadBookings();
        } else {
            showToast(data.message || 'Could not cancel booking', 'error');
        }
    } catch (e) {
        showToast('Connection error', 'error');
    }
}

// --- Reviews ---
async function loadReviews() {
    try {
        const res = await fetch('/api/reviews');
        if (res.ok) {
            const data = await res.json();
            state.reviews = data.reviews || [];

            document.getElementById('review-avg-score').textContent = data.averageRating.toFixed(1);
            document.getElementById('review-total-count').textContent = `Based on ${data.totalReviews} verified traveler reviews`;

            renderStarsRow(document.getElementById('review-avg-stars'), data.averageRating);
            renderReviews(state.reviews);
        }
    } catch (e) {
        console.error('Error loading reviews:', e);
    }
}

function renderStarsRow(el, rating) {
    let html = '';
    for (let i = 1; i <= 5; i++) {
        if (rating >= i) {
            html += '<i class="fa-solid fa-star"></i>';
        } else if (rating >= i - 0.5) {
            html += '<i class="fa-solid fa-star-half-stroke"></i>';
        } else {
            html += '<i class="fa-regular fa-star"></i>';
        }
    }
    el.innerHTML = html;
}

function renderReviews(list) {
    const grid = document.getElementById('reviews-grid');
    if (!list || list.length === 0) {
        grid.innerHTML = '<p style="grid-column:1/-1; text-align:center; color:#64748b;">No reviews yet. Be the first to share your experience!</p>';
        return;
    }

    grid.innerHTML = list.map(r => `
        <div class="review-card">
            <div class="review-author-row">
                <div class="reviewer-profile">
                    <div class="reviewer-avatar">${r.user.charAt(0).toUpperCase()}</div>
                    <div class="reviewer-meta">
                        <strong>${r.user}</strong>
                        <span>${r.createdAt || 'Verified Traveler'}</span>
                    </div>
                </div>
                <div class="stars-row" style="margin:0; font-size:0.95rem;">
                    ${renderStarIcons(r.rating)}
                </div>
            </div>
            <p class="review-body">"${r.comment}"</p>
            <span class="review-dest-tag"><i class="fa-solid fa-location-dot"></i> ${r.destination}</span>
        </div>
    `).join('');
}

function renderStarIcons(rating) {
    let out = '';
    for (let i = 1; i <= 5; i++) {
        out += i <= rating ? '<i class="fa-solid fa-star"></i>' : '<i class="fa-regular fa-star"></i>';
    }
    return out;
}

function openReviewModal() {
    if (state.user) {
        document.getElementById('rev-name').value = state.user.name;
    }
    setRating(5);
    openModal('modal-review');
}

function openReviewModalFor(destination) {
    openReviewModal();
    const destSelect = document.getElementById('rev-dest');
    for (let opt of destSelect.options) {
        if (opt.value.toLowerCase() === destination.toLowerCase()) {
            destSelect.value = opt.value;
            break;
        }
    }
}

function setRating(val) {
    document.getElementById('rev-rating').value = val;
    const stars = document.querySelectorAll('#star-picker i');
    stars.forEach(s => {
        const starVal = parseInt(s.getAttribute('data-val'));
        if (starVal <= val) {
            s.classList.add('active');
        } else {
            s.classList.remove('active');
        }
    });
}

async function handleReviewSubmit(e) {
    e.preventDefault();
    const user = document.getElementById('rev-name').value.trim();
    const destination = document.getElementById('rev-dest').value;
    const rating = parseInt(document.getElementById('rev-rating').value) || 5;
    const comment = document.getElementById('rev-comment').value.trim();
    const btn = document.getElementById('btn-submit-review');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Submitting...';

    try {
        const res = await fetch('/api/reviews', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ user, destination, rating, comment })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            closeModal('modal-review');
            document.getElementById('rev-comment').value = '';
            showToast('Review added! Thank you for sharing your experience.', 'success');
            loadReviews();
        } else {
            showToast(data.message || 'Could not post review.', 'error');
        }
    } catch (err) {
        showToast('Connection error. Could not post review.', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Submit Review';
    }
}

// --- Admin Portal ---
async function loadAdminStats() {
    try {
        const res = await fetch('/api/admin/stats', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (res.ok) {
            const stats = await res.json();
            document.getElementById('admin-stat-bookings').textContent = stats.totalBookings;
            document.getElementById('admin-stat-revenue').textContent = '₹' + stats.totalRevenue.toLocaleString('en-IN');
            document.getElementById('admin-stat-packages').textContent = stats.totalPackages;
            document.getElementById('admin-stat-users').textContent = stats.totalUsers;
        }
    } catch (e) {
        console.error('Error loading admin stats:', e);
    }
}

async function loadAdminBookings() {
    try {
        const res = await fetch('/api/bookings', {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (res.ok) {
            const list = await res.json();
            renderAdminBookingsTable(list);
        }
    } catch (e) {
        console.error('Admin bookings fetch error:', e);
    }
}

function renderAdminPackagesTable(list) {
    const tbody = document.getElementById('admin-packages-table');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">No tour packages available.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(p => `
        <tr>
            <td><strong>#${p.id}</strong></td>
            <td><strong>${p.destination}</strong> <small style="color:#64748b;">(${p.category})</small></td>
            <td>${p.duration}</td>
            <td>₹${p.price.toLocaleString('en-IN')}</td>
            <td><i class="fa-solid fa-star" style="color:#f59e0b;"></i> ${p.rating.toFixed(1)}</td>
            <td>
                <button class="btn btn-sm btn-outline" style="color:#ef4444; border-color:#fca5a5;" onclick="deletePackage(${p.id})">
                    <i class="fa-solid fa-trash"></i> Delete
                </button>
            </td>
        </tr>
    `).join('');
}

function renderAdminBookingsTable(list) {
    const tbody = document.getElementById('admin-bookings-table');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">No customer bookings recorded yet.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(b => `
        <tr>
            <td><strong>${b.id}</strong></td>
            <td>
                ${b.customer}<br>
                <small style="color:#64748b;">${b.phone}</small>
            </td>
            <td>${b.destination}</td>
            <td>${b.travelDate}</td>
            <td><strong>₹${b.amount.toLocaleString('en-IN')}</strong></td>
            <td><span class="trip-status-pill status-${b.status.toLowerCase()}">${b.status}</span></td>
            <td>
                <select class="btn btn-sm btn-outline" onchange="changeBookingStatus('${b.id}', this.value)" style="padding:4px 8px;">
                    <option value="Confirmed" ${b.status === 'Confirmed' ? 'selected' : ''}>Confirmed</option>
                    <option value="Completed" ${b.status === 'Completed' ? 'selected' : ''}>Completed</option>
                    <option value="Cancelled" ${b.status === 'Cancelled' ? 'selected' : ''}>Cancelled</option>
                </select>
            </td>
        </tr>
    `).join('');
}

async function changeBookingStatus(bookingId, status) {
    try {
        const res = await fetch('/api/bookings/status', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${state.token}`
            },
            body: JSON.stringify({ id: bookingId, status })
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showToast(`Booking ${bookingId} marked as ${status}`, 'success');
            loadAdminBookings();
            loadAdminStats();
        } else {
            showToast(data.message || 'Status update failed', 'error');
        }
    } catch (e) {
        showToast('Connection error', 'error');
    }
}

async function deletePackage(id) {
    if (!confirm(`Are you sure you want to delete tour package #${id}?`)) return;

    try {
        const res = await fetch(`/api/packages/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        const data = await res.json();
        if (res.ok && data.success) {
            showToast('Package removed successfully', 'success');
            loadPackages();
            loadAdminStats();
        } else {
            showToast(data.message || 'Could not delete package', 'error');
        }
    } catch (e) {
        showToast('Connection error', 'error');
    }
}

async function handleAddPackageSubmit(e) {
    e.preventDefault();
    const destination = document.getElementById('pkg-dest').value.trim();
    const price = parseFloat(document.getElementById('pkg-price').value);
    const duration = document.getElementById('pkg-duration').value.trim();
    const category = document.getElementById('pkg-category').value;
    const imageUrl = document.getElementById('pkg-img').value.trim();
    const inclusions = document.getElementById('pkg-inclusions').value.trim();
    const description = document.getElementById('pkg-desc').value.trim();

    const btn = document.getElementById('btn-save-package');
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Saving...';

    try {
        const res = await fetch('/api/packages', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${state.token}`
            },
            body: JSON.stringify({
                destination,
                price,
                duration,
                category,
                imageUrl,
                inclusions,
                description,
                rating: 4.8
            })
        });

        if (res.ok) {
            closeModal('modal-add-package');
            showToast(`Package "${destination}" added successfully!`, 'success');
            loadPackages();
            loadAdminStats();
            document.getElementById('pkg-dest').value = '';
            document.getElementById('pkg-price').value = '';
            document.getElementById('pkg-duration').value = '';
            document.getElementById('pkg-img').value = '';
            document.getElementById('pkg-inclusions').value = '';
            document.getElementById('pkg-desc').value = '';
        } else {
            const err = await res.json();
            showToast(err.message || 'Failed to save package.', 'error');
        }
    } catch (err) {
        showToast('Connection error.', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Save Package';
    }
}

// --- Modal & Toast Utilities ---
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }
}

// Close modals when clicking backdrop
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-backdrop')) {
        e.target.classList.remove('active');
        document.body.style.overflow = '';
    }
});

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'fa-info-circle';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(50px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}
