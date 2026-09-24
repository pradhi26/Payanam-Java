package com.payanam.model;

public class Review {
    private String id;
    private String user;
    private String destination;
    private int rating; // 1 to 5
    private String comment;
    private String createdAt;

    public Review() {}

    public Review(String id, String user, String destination, int rating, String comment, String createdAt) {
        this.id = id;
        this.user = user;
        this.destination = destination;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
