package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;
    // @RequiredArgsConstructor 를 사용한 경우 -->
    // public AccountService(AccountRepository accountRepository) {
    //      this.accountRepository = accountRepository;
    // }

    /**
     * 사용자가 존재하는지 조회
     * 계좌의 번호를 생성
     * 계좌를 생성하고 정보 전달
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        validateCreateAccount(accountUser);

        String accountNumber = createRandomAccountNumber();

        while (!isExistAccountNumber(accountNumber)) {
            accountNumber = createRandomAccountNumber();
        }

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
                .orElse("1000000000");

        return AccountDto.fromEntity(
                accountRepository.save(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountStatus(IN_USE)
//                                .accountNumber(newAccountNumber)
                                .accountNumber(accountNumber)
                                .balance(initialBalance)
                                .registeredAt(LocalDateTime.now())
                                .build()));
    }

    private boolean isExistAccountNumber(String accountNumber) {
        Optional<Account> result = accountRepository.findByAccountNumber(accountNumber);
        if (result != null) {
            return true;
        }
        return false;
    }

    private String createRandomAccountNumber() {
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        int randomInt = random.nextInt(1000000000);
        long randomInt2 = randomInt + 1000000000L;
        String accountNumber = String.valueOf(randomInt2);
//        System.out.println(accountNumber);

        return accountNumber;
    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) == 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public Account getAccount(Long id) {
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();    // findById는 Optional로 가져오게됨.
    }

    /**
     * 해당 유저가 없는경우 에러
     * 해당 계좌가 없는경우 에러
     *
     *
     */
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        // 사용자와 계좌의 소유주가 다른경우
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        // 계좌가 이미 해지상태인경우
        if (UNREGISTERED == account.getAccountStatus()) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }
        // 잔액이 있는경우
        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        List<Account> accounts =
                accountRepository.findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }
}
