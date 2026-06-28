package com.kal1van1ch.banktgbot.repository;

import com.kal1van1ch.banktgbot.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
