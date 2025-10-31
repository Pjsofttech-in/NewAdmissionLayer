package com.newadmission.Service;

import com.newadmission.DTO.PeriodAssignmentRequest;
import com.newadmission.Entity.AdmissionTimetable;

import java.time.LocalDate;
import java.util.List;

public interface AdmissionTimetableService {
    AdmissionTimetable createTimetableWithAssignments(String weekday,
                                                      Long classRoomId,
                                                      List<PeriodAssignmentRequest> assignments,
                                                      String role,
                                                      String email);
    List<AdmissionTimetable> getAllTimetables(String role, String email, String branchCode);
    AdmissionTimetable getTimetableById(int id, String role, String email);
    AdmissionTimetable updateTimetableAssignments(int id,
                                                  List<PeriodAssignmentRequest> assignments,
                                                  String role,
                                                  String email);
    void deleteTimetable(int id, String role, String email);
    List<AdmissionTimetable> getTimetablesByClassroom(Integer classRoomId, String role, String email);


    String markPeriodOff(String role, String email, Integer timetableId, Integer periodId);




}