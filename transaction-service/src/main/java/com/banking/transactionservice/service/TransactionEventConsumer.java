package com.banking.transactionservice.service;

import com.banking.transactionservice.config.RedisConfig;
import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final long OTP_EXPIRY_MINUTES = 5;

    /**
     * Consume verification.required
     * Generate OTP and ask user to verify
     * @param payload
     */
    public void consumeVerificationRequired(
            @Payload Map<String, Object> payload){

        try{

            String transactionId = (String) payload.get("transactionId");
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            log.info("Verification required - transaction: {} reason: {}",
                    transactionId, reason);

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException(
                            "Transaction not found " + transactionId
                    ));

            // Id
            if(transaction.getStatus() != TransactionStatus.PROCESSING){
                log.warn("Transaction {} not PROCESSING - skipping", transactionId);
                return;
            }

            // Generate 6 digit otp
            String otp = String.format("%06d", (int) (Math.random() * 900000)+1000000);

            //Store OTP in Redis - expires in 5 minutes
            String otpKey = "verification:otp" + transactionId;
            redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            // Update Status
            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transaction);

            log.info("OTP generated for transaction: {} expires in {} min",
                    transactionId, OTP_EXPIRY_MINUTES);

            // Notify user

        }catch (Exception e){

        }

    }

}
