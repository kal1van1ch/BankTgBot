package com.kal1van1ch.banktgbot.repository;

import com.kal1van1ch.banktgbot.model.entity.Transaction;
import com.kal1van1ch.banktgbot.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Modifying
    @Query("delete from Transaction t where t.user = :user")
    void deleteByUser(@Param("user")User user);
}
