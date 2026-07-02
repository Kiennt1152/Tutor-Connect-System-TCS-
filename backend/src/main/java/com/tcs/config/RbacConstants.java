package com.tcs.config;

/**
 * Role names for Spring Security ({@code hasRole} adds {@code ROLE_} prefix automatically).
 */
public final class RbacConstants {

    public static final String CLIENT = "CLIENT";
    public static final String TUTOR = "TUTOR";
    public static final String TUTOR_CENTER = "TUTOR_CENTER";
    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    public static final String[] BUSINESS_ROLES = {CLIENT, TUTOR, TUTOR_CENTER, PLATFORM_ADMIN};

    private RbacConstants() {}
}
