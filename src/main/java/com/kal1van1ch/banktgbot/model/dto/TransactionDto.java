package com.kal1van1ch.banktgbot.model.dto;

import com.kal1van1ch.banktgbot.model.Bank;
import com.kal1van1ch.banktgbot.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private User user;
    private Long amount;
    private String phoneNumber;
    private Bank bankFrom;
    private LocalDateTime date;
}
