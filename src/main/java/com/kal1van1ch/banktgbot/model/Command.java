package com.kal1van1ch.banktgbot.model;

import lombok.Getter;

@Getter
public enum Command {
    START("/start"),
    RESTART("/restart"),
    HELP("/help"),
    REGISTER("/register"),
    EDIT("/edit");

    private final String command;

    Command(String command){
        this.command = command;
    }
}
