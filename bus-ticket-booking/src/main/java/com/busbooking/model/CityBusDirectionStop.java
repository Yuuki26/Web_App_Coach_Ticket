package com.busbooking.model;

import jakarta.persistence.*;

/** The n-th stop of a direction. */
@Entity
@Table(name = "city_bus_direction_stops",
        indexes = {@Index(columnList = "direction_id, seq"), @Index(columnList = "stop_id")})
public class CityBusDirectionStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private CityBusDirection direction;

    @ManyToOne(optional = false)
    private CityBusStop stop;

    /** 1 = first stop. */
    @Column(nullable = false)
    private int seq;

    protected CityBusDirectionStop() {}

    public CityBusDirectionStop(CityBusDirection direction, CityBusStop stop, int seq) {
        this.direction = direction;
        this.stop = stop;
        this.seq = seq;
    }

    public CityBusDirection getDirection() { return direction; }
    public CityBusStop getStop() { return stop; }
    public int getSeq() { return seq; }
}
