package com.tcs.module.marketplace.entity;

import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.profile.entity.TutorCenter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tutoring_classes")
@Getter
@Setter
@NoArgsConstructor
public class TutoringClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long classId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", length = 20, nullable = false)
    private ClassType classType = ClassType.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private TutorCenter center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private Grade grade;

    @Column(name = "learning_goal", length = 100)
    private String learningGoal;

    @Column(name = "tutor_requirement", length = 255)
    private String tutorRequirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_mode", length = 20, nullable = false)
    private LessonMode lessonMode = LessonMode.OFFLINE;

    @Column(name = "number_of_sessions", nullable = false)
    private Integer numberOfSessions = 1;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "tuition_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal tuitionFee = BigDecimal.ZERO;

    @Column(name = "budget", precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "min_students")
    private Integer minStudents;

    @Column(name = "enrollment_deadline")
    private LocalDate enrollmentDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurring_type", length = 20, nullable = false)
    private RecurringType recurringType = RecurringType.ONCE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TutoringClassStatus status = TutoringClassStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Thời điểm hết hạn hiển thị (đăng lớp + 30 ngày). Chỉ áp dụng cho lớp OPEN chưa ký hợp đồng.
     * Hết hạn -> job dọn dẹp sẽ hard-delete lớp. Null = không tính hạn (DRAFT/đã ghép...).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Grade getGrade() { return grade; }
    public void setGrade(Grade grade) { this.grade = grade; }
    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public TutoringClassStatus getStatus() { return status; }
    public void setStatus(TutoringClassStatus status) { this.status = status; }
    public LessonMode getLessonMode() { return lessonMode; }
    public void setLessonMode(LessonMode lessonMode) { this.lessonMode = lessonMode; }
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public Integer getMinStudents() { return minStudents; }
    public void setMinStudents(Integer minStudents) { this.minStudents = minStudents; }
    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }
    public LocalDate getEnrollmentDeadline() { return enrollmentDeadline; }
    public void setEnrollmentDeadline(LocalDate enrollmentDeadline) { this.enrollmentDeadline = enrollmentDeadline; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public ClassType getClassType() { return classType; }
    public void setClassType(ClassType classType) { this.classType = classType; }
    public TutorCenter getCenter() { return center; }
    public void setCenter(TutorCenter center) { this.center = center; }
    public Integer getNumberOfSessions() { return numberOfSessions; }
    public void setNumberOfSessions(Integer numberOfSessions) { this.numberOfSessions = numberOfSessions; }
    public RecurringType getRecurringType() { return recurringType; }
    public void setRecurringType(RecurringType recurringType) { this.recurringType = recurringType; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
