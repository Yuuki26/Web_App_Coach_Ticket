package com.busbooking.model;

/**
 * What a user is allowed to do. Spring Security sees these as authorities "ROLE_CUSTOMER" etc.
 * <ul>
 *   <li>CUSTOMER – books and manages their own trips</li>
 *   <li>DRIVER – sees assigned trips and passenger lists</li>
 *   <li>MANAGER – manages routes, coaches, trips and promo codes</li>
 *   <li>ADMIN – everything a manager can, plus user management</li>
 * </ul>
 */
public enum Role { CUSTOMER, DRIVER, MANAGER, ADMIN }
