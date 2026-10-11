package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.enity.Account;
import com.banking.accountservice.enity.AccountStatus;
import com.banking.accountservice.enity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    private  static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(CreateAccountRequest request){
        log.info("Creating account for : {}", request.getEmail());

        if(accountRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("account already exists for this email"+ request.getEmail());
        }

        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionalLimit(request.getAccountType() == AccountType.SAVINGS
                        ? new BigDecimal("100000")
                        : new BigDecimal("500000")
                );

        Account savedAccount = accountRepository.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);
    }

    /**
     * Get account by account number
     * @param accountNumber
     * @return
     */
    public AccountResponse getAccount(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("account not found"));

        return mapToResponse(account);
    }

    /**
     * Get account balance
     * @param accountNumber
     * @return
     */
    public BigDecimal getBalance(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("account not found"));

        return account.getBalance();
    }

    /**
     * Block account - called by fraud detection service via kafka
     * @param accountNumber
     */
    public void blockAccount(String accountNumber){
        log.info("Blocking account for : {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account blocked : {} ", accountNumber);
    }

    /**
     * Deduct balance from sender account
     * Called by Transaction Service
     * @param accountNumber
     * @param amount
     */
    public void deductBalance(String accountNumber, BigDecimal amount){
        log.info("Deducting balance {} form account : {}", amount, accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if(account.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("account status is not ACTIVE"+accountNumber);
        }

        if(account.getBalance().compareTo(amount)<0){
            throw new RuntimeException("Insufficient funds for account balance "+accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        log.info("Balance updated. New Balance :  {} ", accountNumber);
    }

    /**
     * Credit balance
     * Called by Transaction Service via Kafka
     * @param accountNumber
     * @param amount
     */
    public void creditBalance(String accountNumber, BigDecimal amount){
        log.info("Credit balance {} form account : {}", amount, accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        log.info("Credit balance updated. New Balance :  {} ", account.getBalance());
    }

    // generate unique 12 digit account number
    private String generateAccountNumber(){

        String accountNumber;

        do{
            long number = secureRandom.nextLong(1_000_000_000_000L); //bound - we need - last 12 digital number -
            accountNumber = String.format("%012d", number);
        } while(accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;

    }

    private AccountResponse mapToResponse(Account account){
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getId());
        accountResponse.setAccountNumber(account.getAccountNumber());
        accountResponse.setAccountHolderName(account.getAccountHolderName());
        accountResponse.setEmail(account.getEmail());
        accountResponse.setPhone(account.getPhone());
        accountResponse.setAccountType(account.getAccountType());
        accountResponse.setStatus(account.getStatus());
        accountResponse.setBalance(account.getBalance());
        accountResponse.setDailyTransactionalLimit(account.getDailyTransactionalLimit());
        accountResponse.setCreatedAt(account.getCreatedAt());

        return accountResponse;
    }

}
