package com.kal1van1ch.banktgbot.mapper;

import com.kal1van1ch.banktgbot.model.dto.TransactionDto;
import com.kal1van1ch.banktgbot.model.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public Transaction toEntity(TransactionDto t){
        Transaction trans = new Transaction();

        trans.setUser(t.getUser());
        trans.setAmount(t.getAmount());
        trans.setPhoneNumber(t.getPhoneNumber());
        trans.setBank(t.getBank());
        trans.setDate(t.getDate());

        return trans;
    }

    public TransactionDto toDto(Transaction t){
        TransactionDto trans = new TransactionDto();

        trans.setId(t.getId());
        trans.setUser(t.getUser());
        trans.setAmount(t.getAmount());
        trans.setPhoneNumber(t.getPhoneNumber());
        trans.setBank(t.getBank());
        trans.setDate(t.getDate());

        return trans;
    }
}
