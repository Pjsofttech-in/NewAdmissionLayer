package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionTaskRepository extends JpaRepository<AdmissionTask, Long> {

    @Query("SELECT t FROM AdmissionTask t WHERE t.branchCode = :branchCode")
    List<AdmissionTask> findAllByBranchCode(@Param("branchCode") String branchCode);
}