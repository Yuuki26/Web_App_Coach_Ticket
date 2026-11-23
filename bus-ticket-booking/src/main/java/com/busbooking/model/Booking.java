package com.busbooking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public reference shown on the e-ticket, e.g. HC-48213. */
    @Column(nullable = false, unique = true)
    private String bookingCode;

    @ManyToOne(optional = false)
    private Trip trip;

    /** The account that booked, or null for a guest booking (found again by code + email). */
    @ManyToOne(fetch = FetchType.LAZY)
    private User customer;

    @Column(nullable = false)
    private String passengerName;

    @Column(nullable = false)
    private String email;

    private String phone;

    /** Seat labels such as "3A". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_label", nullable = false)
    private List<String> seats = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    private String promoCode;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    protected Booking() {}

    public Booking(String bookingCode, Trip trip, String passengerName, String email, String phone,
                   List<String> seats, BigDecimal fare, BigDecimal discount, BigDecimal tax, BigDecimal total,
                   String promoCode, PaymentMethod paymentMethod) {
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
    public String getPromoCode() { return promoCode; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
}
