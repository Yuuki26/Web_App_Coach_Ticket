package com.busbooking.service;

import com.busbooking.dto.QuoteResponse;
import com.busbooking.model.Trip;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * The single place prices are calculated, so the browser can never set its own total.
 * total = fare (seat price + legroom surcharge) - promo discount + tax.
 */
@Service
public class PricingService {

    static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    /** Promo code -> discount rate. Move to a database table when codes need managing. */
    private static final Map<String, BigDecimal> PROMO_CODES = Map.of(
            "FIRSTRIDE", new BigDecimal("0.10")
    );

    public QuoteResponse quote(Trip trip, Collection<String> seats, String promoCode) {
        BigDecimal fare = BigDecimal.ZERO;
        for (String seat : seats) {
            fare = fare.add(trip.getPrice());
            if (SeatLayout.hasExtraLegroom(seat)) fare = fare.add(SeatLayout.LEGROOM_SURCHARGE);
        }
        fare = money(fare);

        String code = promoCode == null ? "" : promoCode.trim().toUpperCase(Locale.ROOT);
        BigDecimal rate = PROMO_CODES.get(code);
        boolean applied = rate != null;

        BigDecimal discount = applied ? money(fare.multiply(rate)) : money(BigDecimal.ZERO);
        BigDecimal tax = money(fare.subtract(discount).multiply(TAX_RATE));
        BigDecimal total = fare.subtract(discount).add(tax);

        String message = null;
        if (applied) {
            message = "Code applied: " + rate.movePointRight(2).stripTrailingZeros().toPlainString() + "% off";
        } else if (!code.isEmpty()) {
            message = "Invalid code. Try FIRSTRIDE";
        }
        return new QuoteResponse(fare, discount, tax, total, applied, applied ? code : null, message);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
