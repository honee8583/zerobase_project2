package com.example.account.domain;

import com.example.account.dto.TransactionType;
import com.example.account.type.TransactionResultType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Transaction {
    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;    // 사용, 사용취소
    @Enumerated(EnumType.STRING)
    private TransactionResultType transactionResultType;    // 결과

    @ManyToOne
    private Account account;

    private Long amount;
    private Long balanceSnapShot;
    private String transactionId;   // pk 를 이용할 경우 보안상 위험
    private LocalDateTime transactedAt;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
