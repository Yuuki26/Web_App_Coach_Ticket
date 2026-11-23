package com.busbooking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "routes", uniqueConstraints = @UniqueConstraint(columnNames = {"origin", "destination"}))
public class Route extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    private int durationMinutes;

    /** Cheapest fare on this route; individual trips add to it. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fromPrice;

    /** Shown in the "Popular routes" section of the home page. */
    private boolean popular;

    protected Route() {}

    public Route(String origin, String destination, int durationMinutes, BigDecimal fromPrice, boolean popular) {
        this.origin = origin;
        this.destination = destination;
        this.durationMinutes = durationMinutes;
        this.fromPrice = fromPrice;
        this.popular = popular;
    }

    public Long getId() { return id; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public int getDurationMinutes() { return durationMinutes; }
    public BigDecimal getFromPrice() { return fromPrice; }
    public boolean isPopular() { return popular; }
}
