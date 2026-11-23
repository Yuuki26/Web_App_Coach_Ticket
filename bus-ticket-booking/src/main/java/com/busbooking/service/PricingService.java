package com.busbooking.service;

import com.busbooking.dto.QuoteResponse;
import com.busbooking.model.PromoCode;
import com.busbooking.model.Trip;
import com.busbooking.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/**
 * The single place prices are calculated, so the browser can never set its own total.
 * total = fare (seat price + legroom surcharge) − promo discount + VAT. All amounts in whole VND.
 */
@Service
public class PricingService {

    static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PromoCodeRepository promoCodes;

    public PricingService(PromoCodeRepository promoCodes) {
        this.promoCodes = promoCodes;
    }

    /** A price breakdown plus the promo code that was applied (null if none). */
    public record Priced(QuoteResponse quote, PromoCode promo) {}

    /** For showing a price. Does not lock or use up the promo code. */
    public QuoteResponse quote(Trip trip, Collection<String> seats, String enteredCode) {
        return price(trip, seats, enteredCode, false).quote();
    }

    /**
     * Prices a booking. With {@code forBooking} the promo code row is locked until the transaction
     * ends, so two bookings can't both take the last use of a limited code.
     */
    public Priced price(Trip trip, Collection<String> seats, String enteredCode, boolean forBooking) {
        BigDecimal sum = BigDecimal.ZERO;
        for (String seat : seats) {
            sum = sum.add(trip.getPrice());
            if (SeatLayout.hasExtraLegroom(seat)) sum = sum.add(SeatLayout.LEGROOM_SURCHARGE);
        }
        BigDecimal fare = money(sum);

        String code = enteredCode == null ? "" : enteredCode.trim().toUpperCase(Locale.ROOT);
        PromoCode promo = null;
        String message = null;
        if (!code.isEmpty()) {
            Optional<PromoCode> found = forBooking ? promoCodes.findByCodeForUpdate(code) : promoCodes.findByCode(code);
            message = found.isEmpty() ? "Invalid code. Try FIRSTRIDE" : whyNotUsable(found.get(), fare);
            if (message == null) {
                promo = found.get();
                message = "Code applied: " + promo.getDiscountPercent() + "% off";
            }
        }

        BigDecimal discount = promo == null ? money(BigDecimal.ZERO)
                : money(fare.multiply(BigDecimal.valueOf(promo.getDiscountPercent())).movePointLeft(2));
        BigDecimal tax = money(fare.subtract(discount).multiply(TAX_RATE));
        BigDecimal total = fare.subtract(discount).add(tax);

        QuoteResponse quote = new QuoteResponse(fare, discount, tax, total, promo != null,
                promo == null ? null : promo.getCode(), message);
        return new Priced(quote, promo);
    }

    /** Null when the code can be used on this fare today, otherwise the reason it can't. */
    static String whyNotUsable(PromoCode p, BigDecimal fare) {
        LocalDate today = LocalDate.now();
        if (!p.isActive()) return "Invalid code. Try FIRSTRIDE";
        if (p.getValidFrom() != null && today.isBefore(p.getValidFrom())) {
            return "This code can be used from " + p.getValidFrom().format(DATE);
        }
        if (p.getValidTo() != null && today.isAfter(p.getValidTo())) {
            return "This code expired on " + p.getValidTo().format(DATE);
        }
        if (p.getUsageLimit() != null && p.getUsedCount() >= p.getUsageLimit()) {
            return "This code has been fully used";
        }
        if (p.getMinFare() != null && fare.compareTo(p.getMinFare()) < 0) {
            return "This code needs a fare of at least " + vnd(p.getMinFare());
        }
        return null;
    }

    static String vnd(BigDecimal amount) {
        return NumberFormat.getIntegerInstance(VI).format(amount) + " ₫";
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }
}
