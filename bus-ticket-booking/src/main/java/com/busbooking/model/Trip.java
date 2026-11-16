package com.busbooking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A scheduled departure of one operator's coach on a route. */
@Entity
@Table(name = "trips", indexes = @Index(columnList = "route_id, departureTime"))
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Route route;

    @ManyToOne(optional = false)
    private Operator operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoachType coachType;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    /** Base price of one seat, before legroom surcharge, promo and tax. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected Trip() {}

    public Trip(Route route, Operator operator, CoachType coachType,
                LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal price) {
        this.route = route;
        this.operator = operator;
        this.coachType = coachType;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
    }

    public Long getId() { return id; }
    public Route getRoute() { return route; }
    public Operator getOperator() { return operator; }
    public CoachType getCoachType() { return coachType; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public BigDecimal getPrice() { return price; }
}
