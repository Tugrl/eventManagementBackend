package com.dada.eventmanagement.bank.service;

import com.dada.eventmanagement.bank.repository.BankTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class BankTransactionService {
    private final BankTransactionRepository repository;

    public BankTransactionService(BankTransactionRepository repository) {
        this.repository = repository;
    }
}
