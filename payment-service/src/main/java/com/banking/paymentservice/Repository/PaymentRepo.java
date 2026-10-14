package com.banking.paymentservice.Repository;

import com.banking.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, String> {

    Optional<Payment> findByRazorpayOrderId(String orderId);

}
