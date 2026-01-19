package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionPaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdmissionPaymentTransactionRepository extends JpaRepository<AdmissionPaymentTransaction,Long>
{

    @Query("SELECT t FROM AdmissionPaymentTransaction t WHERE t.razorpayOrderId = :orderId")
    Optional<AdmissionPaymentTransaction> findByRazorpayOrderId(@Param("orderId") String orderId);
}
