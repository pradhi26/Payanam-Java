package com.payanam;

import com.payanam.model.TourPackage;
import org.junit.Test;

import static org.junit.Assert.*;

public class TourPackageTest {

    @Test
    public void testTourPackageConstructorAndGetters() {

        TourPackage tour = new TourPackage(
                1,
                "Goa",
                "3 Days",
                15000.0,
                4.5,
                "Beach",
                "Goa beach tour",
                "goa.jpg",
                "Hotel, Food, Transport"
        );

        assertEquals(1, tour.getId());
        assertEquals("Goa", tour.getDestination());
        assertEquals("3 Days", tour.getDuration());
        assertEquals(15000.0, tour.getPrice(), 0.01);
        assertEquals(4.5, tour.getRating(), 0.01);
        assertEquals("Beach", tour.getCategory());
        assertEquals("Goa beach tour", tour.getDescription());
        assertEquals("goa.jpg", tour.getImageUrl());
        assertEquals("Hotel, Food, Transport", tour.getInclusions());
    }

    @Test
    public void testSetters() {

        TourPackage tour = new TourPackage();

        tour.setId(2);
        tour.setDestination("Kerala");
        tour.setDuration("5 Days");
        tour.setPrice(20000.0);
        tour.setRating(4.8);
        tour.setCategory("Nature");
        tour.setDescription("Kerala tour");
        tour.setImageUrl("kerala.jpg");
        tour.setInclusions("Hotel, Food");

        assertEquals(2, tour.getId());
        assertEquals("Kerala", tour.getDestination());
        assertEquals("5 Days", tour.getDuration());
        assertEquals(20000.0, tour.getPrice(), 0.01);
        assertEquals(4.8, tour.getRating(), 0.01);
        assertEquals("Nature", tour.getCategory());
        assertEquals("Kerala tour", tour.getDescription());
        assertEquals("kerala.jpg", tour.getImageUrl());
        assertEquals("Hotel, Food", tour.getInclusions());
    }
}