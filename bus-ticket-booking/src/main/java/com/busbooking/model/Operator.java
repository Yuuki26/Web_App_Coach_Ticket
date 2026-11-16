package com.busbooking.model;

import jakarta.persistence.*;

/** A coach company that runs trips. */
@Entity
@Table(name = "operators")
public class Operator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Brand color (hex) used for the operator badge in the UI. */
    private String color;

    private double rating;

    protected Operator() {}

    public Operator(String name, String color, double rating) {
        this.name = name;
        this.color = color;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public double getRating() { return rating; }
}
