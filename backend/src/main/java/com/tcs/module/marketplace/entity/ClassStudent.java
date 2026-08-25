package com.tcs.module.marketplace.entity;

import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.profile.entity.ChildProfile;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "class_students")
@Getter
@Setter
@NoArgsConstructor
public class ClassStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_student_id")
    private Long classStudentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private TutoringClass tutoringClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_profile_id")
    private ChildProfile childProfile;

    // Nguoi ghi danh & tra hoc phi (client). Dung cho lop CENTER; NULL khi center import.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_by_user_id")
    private User enrolledByUser;

    @Column(name = "student_name", length = 100, nullable = false)
    private String studentName;

    @Column(name = "student_phone", length = 15)
    private String studentPhone;

    @Column(name = "student_email", length = 100)
    private String studentEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ClassStudentStatus status = ClassStudentStatus.ENROLLED;

    @CreationTimestamp
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Long getClassStudentId() { return classStudentId; }
    public void setClassStudentId(Long classStudentId) { this.classStudentId = classStudentId; }
    public TutoringClass getTutoringClass() { return tutoringClass; }
    public void setTutoringClass(TutoringClass tutoringClass) { this.tutoringClass = tutoringClass; }
    public ChildProfile getChildProfile() { return childProfile; }
    public void setChildProfile(ChildProfile childProfile) { this.childProfile = childProfile; }
    public User getEnrolledByUser() { return enrolledByUser; }
    public void setEnrolledByUser(User enrolledByUser) { this.enrolledByUser = enrolledByUser; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentPhone() { return studentPhone; }
    public void setStudentPhone(String studentPhone) { this.studentPhone = studentPhone; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public ClassStudentStatus getStatus() { return status; }
    public void setStatus(ClassStudentStatus status) { this.status = status; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
