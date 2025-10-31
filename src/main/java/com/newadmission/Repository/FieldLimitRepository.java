//package com.newadmission.Repository;
//
//import com.newadmission.Entity.InquiryFieldLimit;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface FieldLimitRepository extends JpaRepository<InquiryFieldLimit,Long>
//{
//    @Query("SELECT f FROM FieldLimit f WHERE f.instituteEmail = :instituteEmail AND f.entityName = :entityName AND f.enabled = true")
//    List<InquiryFieldLimit> findEnabledFieldsByInstituteEmailAndEntityName(@Param("instituteEmail") String instituteEmail, @Param("entityName") String entityName);
//
//
//}
