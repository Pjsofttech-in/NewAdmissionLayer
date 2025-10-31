package com.newadmission.Repository;

import com.newadmission.Entity.PeriodAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodAssignmentRepository extends JpaRepository<PeriodAssignment, Integer> {

    // Find a date-specific override for a period in a timetable
    @Query("SELECT p FROM PeriodAssignment p " +
            "WHERE p.timetable.id = :timetableId " +
            "AND p.period.id = :periodId " +
            "AND p.date = :date")
    Optional<PeriodAssignment> findByTimetableIdAndPeriodIdAndDate(
            @Param("timetableId") Integer timetableId,
            @Param("periodId") Integer periodId,
            @Param("date") LocalDate date
    );

    // Find all date-specific assignments for a timetable
    @Query("SELECT pa FROM PeriodAssignment pa " +
            "WHERE pa.timetable.id = :timetableId " +
            "AND pa.date = :date")
    List<PeriodAssignment> findAllByTimetableIdAndDate(
            @Param("timetableId") Integer timetableId,
            @Param("date") LocalDate date
    );
}