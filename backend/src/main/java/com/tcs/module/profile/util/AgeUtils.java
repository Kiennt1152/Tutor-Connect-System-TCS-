package com.tcs.module.profile.util;

import java.time.LocalDate;
import java.time.Period;

public final class AgeUtils {

    public static final int ADULT_AGE_THRESHOLD = 18;

    private AgeUtils() {}

    public static int ageAt(LocalDate dateOfBirth, LocalDate onDate) {
        if (dateOfBirth == null || onDate == null) {
            return -1;
        }
        return Period.between(dateOfBirth, onDate).getYears();
    }

    public static boolean isMinor(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return false;
        }
        return ageAt(dateOfBirth, LocalDate.now()) < ADULT_AGE_THRESHOLD;
    }

    public static boolean isAdult(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return false;
        }
        return ageAt(dateOfBirth, LocalDate.now()) >= ADULT_AGE_THRESHOLD;
    }
}
