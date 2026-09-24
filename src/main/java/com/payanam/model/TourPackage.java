package com.payanam.model;

public class TourPackage {
    private int id;
    private String destination;
    private String duration;
    private double price;
    private double rating;
    private String category;
    private String description;
    private String imageUrl;
    private String inclusions;

    public TourPackage() {}

    public TourPackage(int id, String destination, String duration, double price, double rating, 
                       String category, String description, String imageUrl, String inclusions) {
        this.id = id;
        this.destination = destination;
        this.duration = duration;
        this.price = price;
        this.rating = rating;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.inclusions = inclusions;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getInclusions() { return inclusions; }
    public void setInclusions(String inclusions) { this.inclusions = inclusions; }
}
