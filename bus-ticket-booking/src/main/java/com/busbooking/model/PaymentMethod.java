package com.busbooking.model;

/** Which option the rider picked. No card details are ever sent to or stored by the backend. */
public enum PaymentMethod {
    CARD, APPLE, GOOGLE, PAYPAL,
    /** MoMo / ZaloPay */
    EWALLET,
    /** VietQR bank transfer */
    VIETQR,
    /** Apple Pay or Google Pay, offered as one option on the Vietnam site */
    DIGITAL_WALLET
}
