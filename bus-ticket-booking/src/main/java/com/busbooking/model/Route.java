package com.busbooking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** A direction of travel between two cities, e.g. TP. Hồ Chí Minh → Đà Lạt. */
@Entity
@Table(name = "routes", uniqueConstraints = @UniqueConstraint(columnNames = {"origin", "destination"}))
public class Route extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String origin;

    @Column(nullable = false, length = 80)
    private String destination;

    /** Bus station the coach leaves from, e.g. Bến xe Miền Đông mới. */
    @Column(nullable = false, length = 120)
    private String originStation;

    @Column(nullable = false, length = 120)
    private String destinationStation;

    @Column(nullable = false)
    private int distanceKm;

    @Column(nullable = false)
    private int durationMinutes;

    /** Cheapest fare on this route (VND); individual trips add to it. */
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal fromPrice;

    /** Shown in the "Popular routes" section of the home page. */
    @Column(nullable = false)
    private boolean popular;

    /**
     * Map position of each station (WGS84). Optional: used to ask Google Maps for live traffic. When
     * missing, the station name and city are sent as an address instead.
     */
    private Double originLat;
    private Double originLng;
    private Double destinationLat;
    private Double destinationLng;

    /** False = removed (soft delete): hidden from search, history kept. */
    @Column(nullable = false)
    private boolean active = true;

    protected Route() {}

    public Route(String origin, String destination, String originStation, String destinationStation,
                 int distanceKm, int durationMinutes, BigDecimal fromPrice, boolean popular) {
        this.origin = origin;
        this.destination = destination;
        this.originStation = originStation;
        this.destinationStation = destinationStation;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.fromPrice = fromPrice;
        this.popular = popular;
    }

    public Long getId() { return id; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getOriginStation() { return originStation; }
    public void setOriginStation(String originStation) { this.originStation = originStation; }
    public String getDestinationStation() { return destinationStation; }
    public void setDestinationStation(String destinationStation) { this.destinationStation = destinationStation; }
    public int getDistanceKm() { return distanceKm; }
    public void setDistanceKm(int distanceKm) { this.distanceKm = distanceKm; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getFromPrice() { return fromPrice; }
    public void setFromPrice(BigDecimal fromPrice) { this.fromPrice = fromPrice; }
    public boolean isPopular() { return popular; }
    public void setPopular(boolean popular) { this.popular = popular; }
    public Double getOriginLat() { return originLat; }
    public Double getOriginLng() { return originLng; }
    public Double getDestinationLat() { return destinationLat; }
    public Double getDestinationLng() { return destinationLng; }

    public boolean hasCoordinates() {
        return originLat != null && originLng != null && destinationLat != null && destinationLng != null;
    }

    public void setOriginPosition(Double lat, Double lng) {
        this.originLat = lat;
        this.originLng = lng;
    }

    public void setDestinationPosition(Double lat, Double lng) {
        this.destinationLat = lat;
        this.destinationLng = lng;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
