package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionPeriod;
import com.newadmission.Entity.AdmissionTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionPeriodRepository extends JpaRepository<AdmissionPeriod, Integer> {
    List<AdmissionPeriod> findAllByBranchCode(String branchCode);


}
