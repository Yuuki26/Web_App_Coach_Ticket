package com.busbooking.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** One direction of a city bus line (an OpenStreetMap route relation): its path and stops in order. */
@Entity
@Table(name = "city_bus_directions")
public class CityBusDirection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private CityBusLine line;

    @Column(nullable = false, unique = true)
    private Long osmRelationId;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 150)
    private String fromName;

    @Column(length = 150)
    private String toName;

    /** Road path in Google's encoded polyline format (simplified to about 10 m). */
    @Lob
    @Column(nullable = false)
    private String polyline;

    /** Length of the path in metres. */
    @Column(nullable = false)
    private int lengthMeters;

    @OneToMany(mappedBy = "direction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq")
    private List<CityBusDirectionStop> stops = new ArrayList<>();

    protected CityBusDirection() {}

    public CityBusDirection(CityBusLine line, Long osmRelationId, int sortOrder) {
        this.line = line;
        this.osmRelationId = osmRelationId;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public CityBusLine getLine() { return line; }
    public Long getOsmRelationId() { return osmRelationId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }
    public String getPolyline() { return polyline; }
    public void setPolyline(String polyline) { this.polyline = polyline; }
    public int getLengthMeters() { return lengthMeters; }
    public void setLengthMeters(int lengthMeters) { this.lengthMeters = lengthMeters; }
    public List<CityBusDirectionStop> getStops() { return stops; }
}
