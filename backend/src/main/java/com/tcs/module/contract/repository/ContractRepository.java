package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByContractNo(String contractNo);

    Optional<Contract> findByAssignment_AssignmentId(Long assignmentId);

    Optional<Contract> findByClassStudent_ClassStudentId(Long classStudentId);

    @Query("SELECT c FROM Contract c WHERE c.assignment.assignmentId = :assignmentId")
    Optional<Contract> findByAssignmentId(@Param("assignmentId") Long assignmentId);

    Optional<Contract> findByRecruitmentApplication_RecruitmentAppId(Long recruitmentAppId);

    @Query("SELECT c FROM Contract c WHERE c.status = :status")
    List<Contract> findByStatus(@Param("status") ContractStatus status);

    @Query("SELECT COUNT(c) FROM Contract c WHERE FUNCTION('DATE', c.createdAt) = FUNCTION('DATE', CURRENT_DATE)")
    long countTodayContracts();

    long countByStatus(ContractStatus status);

    @Query("SELECT DISTINCT c FROM Contract c "
            + "LEFT JOIN c.assignment a "
            + "LEFT JOIN a.tutor assignmentTutor "
            + "LEFT JOIN assignmentTutor.user assignmentTutorUser "
            + "LEFT JOIN a.application app "
            + "LEFT JOIN app.tutor appTutor "
            + "LEFT JOIN appTutor.user appTutorUser "
            + "LEFT JOIN app.tutoringClass tc "
            + "LEFT JOIN tc.creator tcCreator "
            + "LEFT JOIN c.recruitmentApplication ra "
            + "LEFT JOIN ra.tutor raTutor "
            + "LEFT JOIN raTutor.user raTutorUser "
            + "LEFT JOIN ra.recruitmentPost rp "
            + "LEFT JOIN rp.center rpCenter "
            + "LEFT JOIN rpCenter.user rpCenterUser "
            + "LEFT JOIN c.classStudent cs "
            + "LEFT JOIN cs.enrolledByUser csEnroller "
            + "LEFT JOIN cs.tutoringClass csClass "
            + "LEFT JOIN csClass.creator csCreator "
            + "WHERE assignmentTutorUser.userId = :userId "
            + "OR appTutorUser.userId = :userId OR tcCreator.userId = :userId "
            + "OR raTutorUser.userId = :userId OR rpCenterUser.userId = :userId "
            + "OR csEnroller.userId = :userId OR csCreator.userId = :userId")
    List<Contract> findContractsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT signature.contract FROM ContractSignature signature
            LEFT JOIN signature.signer signer
            WHERE signer.userId = :userId
               OR (:email IS NOT NULL AND LOWER(signature.email) = LOWER(:email))
            """)
    List<Contract> findBySignatureParty(
            @Param("userId") Long userId,
            @Param("email") String email);

    @Query("""
            SELECT DISTINCT c FROM Contract c
            WHERE c.assignment IS NOT NULL
              AND c.assignment.tutor.user.userId = :userId
            """)
    List<Contract> findByAssignment_Tutor_UserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT c FROM Contract c
            WHERE c.assignment IS NOT NULL
              AND c.assignment.application IS NOT NULL
              AND c.assignment.application.tutoringClass.creator.userId = :userId
            """)
    List<Contract> findByAssignment_ClassCreator_UserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT c FROM Contract c
            WHERE c.classStudent IS NOT NULL
              AND (
                    c.classStudent.tutoringClass.creator.userId = :userId
                    OR c.classStudent.enrolledByUser.userId = :userId
                  )
            """)
    List<Contract> findByClassStudent_UserId(@Param("userId") Long userId);

    /** BF-03: thỏa thuận hợp tác — phía GIA SƯ (người ký) nhìn thấy để ký. */
    @Query("""
            SELECT DISTINCT c FROM Contract c
            WHERE c.recruitmentApplication IS NOT NULL
              AND c.recruitmentApplication.tutor.user.userId = :userId
            """)
    List<Contract> findByRecruitmentApplication_Tutor_UserId(@Param("userId") Long userId);

    /** BF-03: thỏa thuận hợp tác — phía TRUNG TÂM (bên tạo/duyệt) theo dõi. */
    @Query("""
            SELECT DISTINCT c FROM Contract c
            WHERE c.recruitmentApplication IS NOT NULL
              AND c.recruitmentApplication.recruitmentPost.center.user.userId = :userId
            """)
    List<Contract> findByRecruitmentApplication_CenterUser_UserId(@Param("userId") Long userId);
}
