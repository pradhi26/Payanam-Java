package com.payanam.model;

public class Booking {
    private String id;
    private String customer;
    private String phone;
    private String userEmail;
    private String destination;
    private int packageId;
    private String travelDate;
    private int travellers;
    private double amount;
    private String status; // "Confirmed", "Cancelled", "Completed"
    private String bookedAt;

    public Booking() {}

    public Booking(String id, String customer, String phone, String userEmail, String destination,
                   int packageId, String travelDate, int travellers, double amount, String status, String bookedAt) {
        this.id = id;
        this.customer = customer;
        this.phone = phone;
        this.userEmail = userEmail;
        this.destination = destination;
        this.packageId = packageId;
        this.travelDate = travelDate;
        this.travellers = travellers;
        this.amount = amount;
        this.status = status;
        this.bookedAt = bookedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public int getPackageId() { return packageId; }
    public void setPackageId(int packageId) { this.packageId = packageId; }

    public String getTravelDate() { return travelDate; }
    public void setTravelDate(String travelDate) { this.travelDate = travelDate; }

    public int getTravellers() { return travellers; }
    public void setTravellers(int travellers) { this.travellers = travellers; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBookedAt() { return bookedAt; }
    public void setBookedAt(String bookedAt) { this.bookedAt = bookedAt; }
}
