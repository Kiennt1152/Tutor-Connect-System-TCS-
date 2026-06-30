package com.tcs.module.platform.mapper;

import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;

public record UserProfileBundle(
        PlatformAdmin platformAdmin, Tutor tutor, TutorCenter tutorCenter, Client client) {

    public static UserProfileBundle of(
            PlatformAdmin platformAdmin, Tutor tutor, TutorCenter tutorCenter, Client client) {
        return new UserProfileBundle(platformAdmin, tutor, tutorCenter, client);
    }
}
