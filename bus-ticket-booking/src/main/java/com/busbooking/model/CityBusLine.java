package com.busbooking.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A Ho Chi Minh City bus line, e.g. "01 Ga Ba Son - Bến xe Chợ Lớn". Most lines run in two directions.
 * Imported from OpenStreetMap (route_master / route relations); the text fields keep OSM's values,
 * e.g. openingHours "Mo-Su 05:00-22:00", headway "00:08-00:20", fare "6000 VND".
 */
@Entity
@Table(name = "city_bus_lines",
        uniqueConstraints = @UniqueConstraint(columnNames = {"network", "ref"}),
        indexes = @Index(columnList = "ref"))
public class CityBusLine extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Line number as shown on the bus, e.g. 01, 60-3, D4. */
    @Column(nullable = false, length = 20)
    private String ref;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String network;

    /** Can name several companies, e.g. "A; B" (up to about 220 characters in OSM today). */
    @Column(length = 500)
    private String operator;

    @Column(length = 200)
    private String openingHours;

    /** Minutes between buses, as OSM writes it: "00:08-00:20" = every 8 to 20 minutes. */
    @Column(length = 50)
    private String headway;

    /** Single-ride fare as OSM writes it, e.g. "6000 VND". */
    @Column(length = 50)
    private String fare;

    /** Fare in whole VND when it could be read from {@link #fare}. */
    private Integer fareVnd;

    @Column(length = 10)
    private String colour;

    /** OpenStreetMap route_master relation id, when the line has one. */
    private Long osmMasterId;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<CityBusDirection> directions = new ArrayList<>();

    protected CityBusLine() {}

    public CityBusLine(String network, String ref) {
        this.network = network;
        this.ref = ref;
    }

    public Long getId() { return id; }
    public String getRef() { return ref; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNetwork() { return network; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public String getHeadway() { return headway; }
    public void setHeadway(String headway) { this.headway = headway; }
    public String getFare() { return fare; }
    public void setFare(String fare) { this.fare = fare; }
    public Integer getFareVnd() { return fareVnd; }
    public void setFareVnd(Integer fareVnd) { this.fareVnd = fareVnd; }
    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }
    public Long getOsmMasterId() { return osmMasterId; }
    public void setOsmMasterId(Long osmMasterId) { this.osmMasterId = osmMasterId; }
    public List<CityBusDirection> getDirections() { return directions; }
}
