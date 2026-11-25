package com.busbooking.model;

import jakarta.persistence.*;

/** A bus stop (an OpenStreetMap platform node). One stop can be served by many lines. */
@Entity
@Table(name = "city_bus_stops", indexes = @Index(columnList = "lat, lng"))
public class CityBusStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long osmNodeId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    protected CityBusStop() {}

    public CityBusStop(Long osmNodeId, String name, double lat, double lng) {
        this.osmNodeId = osmNodeId;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public Long getId() { return id; }
    public Long getOsmNodeId() { return osmNodeId; }
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }

    public void update(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }
}
