package com.busbooking.model;

import jakarta.persistence.*;

/** A coach company. Owns coaches; its trips are the trips run on its coaches. */
@Entity
@Table(name = "operators")
public class Operator extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Brand color (#RRGGBB) used for the operator badge in the UI. */
    @Column(nullable = false, length = 7)
    private String color;

    /** Average rider rating, 1.0–5.0. */
    @Column(nullable = false)
    private double rating;

    @Column(length = 30)
    private String hotline;

    @Column(length = 150)
    private String email;

    /** False = removed (soft delete). Kept for the history of past trips. */
    @Column(nullable = false)
    private boolean active = true;

    protected Operator() {}

    public Operator(String name, String color, double rating, String hotline, String email) {
        this.name = name;
        this.color = color;
        this.rating = rating;
        this.hotline = hotline;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
