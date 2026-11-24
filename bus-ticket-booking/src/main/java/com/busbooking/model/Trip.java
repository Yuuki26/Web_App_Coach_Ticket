package com.busbooking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A scheduled departure of one coach on a route. The operator and coach type are not stored here:
 * they come from the coach (keeps the table in 3NF).
 */
@Entity
@Table(name = "trips", indexes = {
        @Index(columnList = "route_id, departureTime"),
        @Index(columnList = "coach_id, departureTime"),
        @Index(columnList = "status")})
public class Trip extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Route route;

    @ManyToOne(optional = false)
    private Coach coach;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    /** Base price of one seat in VND, before legroom surcharge, promo and tax. */
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status = TripStatus.SCHEDULED;

    protected Trip() {}

    public Trip(Route route, Coach coach, LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal price) {
        this.route = route;
        this.coach = coach;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
    }

    public Operator getOperator() { return coach.getOperator(); }
    public CoachType getCoachType() { return coach.getType(); }

    public Long getId() { return id; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Coach getCoach() { return coach; }
    public void setCoach(Coach coach) { this.coach = coach; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
}
