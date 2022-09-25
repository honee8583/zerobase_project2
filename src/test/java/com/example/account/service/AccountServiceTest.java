package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.AccountRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

//@SpringBootTest // 실제구동처럼 모든 빈을 생성
@ExtendWith(MockitoExtension.class) // Mockito 확장팩을 테스트에 달아준다.
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;    // @Mock 어노테이션을 사용하면 가짜로 생성

    @Mock
    private AccountUserRepository accountUserRepository;

    // @SpringBootTest로 인해 모든 빈들이 생성되었으므로 Autowired를 이용해서 주입
    // @Mock으로 만든 가짜 의존성을 @InjectMocks 를 사용하여 주입
    @InjectMocks
    private AccountService accountService;

    // 각 테스트를 하기전에 수행하는 테스트 메소드
//    @BeforeEach
//    void init() {
//        accountService.createAccount();
//    }

    @Test
    @DisplayName("AccountService -> 계좌 생성 성공")
    void createAccountSuccess() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012").build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto =
                accountService.createAccount(1L, 1000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserID());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("AccountService -> 첫 계좌 생성")
    void createFirstAccount() {
        // given
        AccountUser user = AccountUser.builder()
                .id(15L)
                .name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());  // 기존에 계좌가 하나도 없을 경우
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto =
                accountService.createAccount(1L, 1000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserID());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("AccountService -> 해당 유저 없음(계좌 생성 실패)")
    void createAccount_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException =
                assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("AccountService -> 최대 계좌 개수 초과")
    void createAccount_maxAccountIs10() {
        AccountUser user = AccountUser.builder()
                        .id(12L)
                        .name("Pobi").build();

        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when
        AccountException exception =
                assertThrows(AccountException.class,
                        () -> accountService.createAccount(1L, 1000L));
        // then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("AccountService -> 계좌 해지 성공")
    void deleteAccountSuccess() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        // when
        AccountDto accountDto =
                accountService.deleteAccount(1L, "1234567890");

        // then
        verify(accountRepository, times(0)).save(any());
        assertEquals(12L, accountDto.getUserID());
    }

    @Test
    @DisplayName("AccountService -> 해당 유저 없음(계좌 해지 실패)")
    void deleteAccount_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("AccountService -> 계좌가 없을 경우")
    void deleteAccount_AccountNotFound() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("AccountService -> 계좌 소유주 다름")
    void deleteAccountFailed_userUnMatch() {
        // given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        AccountUser harry = AccountUser.builder()
                .id(13L)
                .name("Harry").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        // when
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("AccountService -> 이미 해지상태인 경우")
    void deleteAccountFailed_alreadyUnRegistered() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("1000000012").build()));

        // when
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("AccountService -> 해지시도 계좌 잔액 존재")
    void deleteAccountFailed_balanceNotEmpty() {
        // given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .balance(100L)
                        .accountNumber("1000000012").build()));

        // when
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("AccountService -> 계좌 정보 확인 성공")
    void successGetAccountsByUserId() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();

        List<Account> accountList =
                Arrays.asList(
                        Account.builder()
                                .accountUser(user)
                                .accountNumber("1234567890")
                                .balance(1000L)
                                .build(),
                        Account.builder()
                                .accountUser(user)
                                .accountNumber("2345678901")
                                .balance(2000L)
                                .build()
                );

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountUser(any()))
                .willReturn(accountList);
        // when
        List<AccountDto> accountDtoList =
                accountService.getAccountsByUserId(anyLong());

        // then
        assertEquals(2, accountDtoList.size());
        assertEquals("1234567890", accountDtoList.get(0).getAccountNumber());
        assertEquals(1000L, accountDtoList.get(0).getBalance());
        assertEquals("2345678901", accountDtoList.get(1).getAccountNumber());
        assertEquals(2000L, accountDtoList.get(1).getBalance());
    }

    @Test
    @DisplayName("AccountService -> 사용자 id가 없을 경우")
    void failedToGetAccounts() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}