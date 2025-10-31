package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionPeriod;
import com.newadmission.Entity.AdmissionTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionTimetableRepository extends JpaRepository<AdmissionTimetable, Integer> {
    List<AdmissionTimetable> findAllByBranchCode(String branchCode);
    AdmissionTimetable findByClassRoom_Id(Long classRoomId);

    boolean existsByClassRoomIdAndWeekday(Long classRoomId, String weekday);

    List<AdmissionTimetable> findByClassRoomId(Long classRoomId);
}