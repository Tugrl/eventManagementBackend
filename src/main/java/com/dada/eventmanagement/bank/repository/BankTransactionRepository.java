package com.dada.eventmanagement.bank.repository;

import com.dada.eventmanagement.bank.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
}
