package com.busbooking.model;

import jakarta.persistence.*;

/**
 * One physical coach (bus). Seats are laid out in {@code seatRows} rows of A B | C D,
 * so capacity is seatRows × 4.
 */
@Entity
@Table(name = "coaches", indexes = @Index(columnList = "operator_id, status"))
public class Coach extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vietnamese licence plate, e.g. 51B-123.45. */
    @Column(nullable = false, unique = true, length = 20)
    private String plateNumber;

    @ManyToOne(optional = false)
    private Operator operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoachType type;

    @Column(nullable = false)
    private int seatRows;

    @Column(nullable = false)
    private int manufactureYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoachStatus status = CoachStatus.ACTIVE;

    protected Coach() {}

    public Coach(String plateNumber, Operator operator, CoachType type, int seatRows, int manufactureYear) {
        this.plateNumber = plateNumber;
        this.operator = operator;
        this.type = type;
        this.seatRows = seatRows;
        this.manufactureYear = manufactureYear;
    }

    public int capacity() { return seatRows * 4; }

    public Long getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }
    public CoachType getType() { return type; }
    public void setType(CoachType type) { this.type = type; }
    public int getSeatRows() { return seatRows; }
    public void setSeatRows(int seatRows) { this.seatRows = seatRows; }
    public int getManufactureYear() { return manufactureYear; }
    public void setManufactureYear(int manufactureYear) { this.manufactureYear = manufactureYear; }
    public CoachStatus getStatus() { return status; }
    public void setStatus(CoachStatus status) { this.status = status; }
}
