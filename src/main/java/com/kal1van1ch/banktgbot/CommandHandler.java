package com.kal1van1ch.banktgbot;

import java.util.Arrays;

public enum CommandHandler {
    START("/start"),
    UNKNOWN("/unkmown");

    private final String command;

    CommandHandler(String command){
        this.command = command;
    }

    public String getCommand(){
        return command;
    }

    public static CommandHandler knowCommand(String value){
        return Arrays.stream(values())
                .filter(cmd -> cmd.getCommand().equals(value))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
