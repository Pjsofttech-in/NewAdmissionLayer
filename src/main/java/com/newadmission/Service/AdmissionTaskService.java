package com.newadmission.Service;

import com.newadmission.Entity.AdmissionTask;

import java.util.List;

public interface AdmissionTaskService {

    AdmissionTask createTask(AdmissionTask task, String role, String email);

    List<AdmissionTask> getAllTasks(String role, String email, String branchCode);

    AdmissionTask getTaskById(Long id, String role, String email);

    AdmissionTask updateTask(Long id, AdmissionTask task, String role, String email);

    void deleteTask(Long id, String role, String email);
}