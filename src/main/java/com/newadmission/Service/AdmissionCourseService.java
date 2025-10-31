package com.newadmission.Service;

import com.newadmission.Entity.AdmissionCourse;

import java.util.List;

public interface AdmissionCourseService {
    AdmissionCourse createCourse(AdmissionCourse course, String role, String email);
    List<AdmissionCourse> getAllCourses(String role, String email, String branchCode);
    AdmissionCourse updateCourse(Long id, AdmissionCourse course, String role, String email);
    void deleteCourse(Long id, String role, String email);
    AdmissionCourse getCourseById(Long id, String role, String email);
}