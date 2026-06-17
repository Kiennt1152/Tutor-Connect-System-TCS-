package com.tcs.profile.entity;

import com.tcs.catalog.entity.Location;
import com.tcs.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tutor_center")
@Getter
@Setter
@NoArgsConstructor
public class TutorCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tutorcenter_id", length = 36, nullable = false, updatable = false)
    private UUID tutorCenterId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", length = 150, nullable = false)
    private String companyName;

    @Column(name = "license_no", length = 50, nullable = false, unique = true)
    private String licenseNo;

    @Column(name = "phone", length = 15, nullable = false)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT", nullable = false)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
