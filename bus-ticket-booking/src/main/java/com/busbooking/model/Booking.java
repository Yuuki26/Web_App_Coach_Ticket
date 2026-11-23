package com.busbooking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {
        @Index(columnList = "trip_id, status"),
        @Index(columnList = "email"),
        @Index(columnList = "created_at")})
public class Booking extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public reference shown on the e-ticket, e.g. HC-48213. */
    @Column(nullable = false, unique = true, length = 20)
    private String bookingCode;

    @ManyToOne(optional = false)
    private Trip trip;

    /** The account that booked, or null for a guest booking (found again by code + email). */
    @ManyToOne(fetch = FetchType.LAZY)
    private User customer;

    @Column(nullable = false, length = 100)
    private String passengerName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    /** Seat labels such as "3A". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_label", nullable = false)
    private List<String> seats = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal fare;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal discount;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal tax;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal total;

    /** The discount code used, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    private PromoCode promoCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.CONFIRMED;

    protected Booking() {}

    public Booking(String bookingCode, Trip trip, String passengerName, String email, String phone,
                   List<String> seats, BigDecimal fare, BigDecimal discount, BigDecimal tax, BigDecimal total,
                   PromoCode promoCode, PaymentMethod paymentMethod) {
        this.bookingCode = bookingCode;
        this.trip = trip;
        this.passengerName = passengerName;
        this.email = email;
        this.phone = phone;
        this.seats = new ArrayList<>(seats);
        this.fare = fare;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.promoCode = promoCode;
        this.paymentMethod = paymentMethod;
    }

    public Long getId() { return id; }
    public String getBookingCode() { return bookingCode; }
    public Trip getTrip() { return trip; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public String getPassengerName() { return passengerName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public List<String> getSeats() { return seats; }
    public BigDecimal getFare() { return fare; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getTotal() { return total; }
    public PromoCode getPromoCode() { return promoCode; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
}
