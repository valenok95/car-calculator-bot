package ru.wallentos.carcalculatorbot.controller;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class TelegramBot extends TelegramWebhookBot {

    @Value("${bot.uri}")
    private String botUri;

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.key}")
    private String botKey;
    private UpdateProcessor updateProcessor;

    @Autowired
    public TelegramBot(UpdateProcessor updateProcessor) {
        this.updateProcessor = updateProcessor;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Старт"));
        listofCommands.add(new BotCommand("/currencyrates", "Актуальный курс оплаты"));
        listofCommands.add(new BotCommand("/settingservice", "Сервис"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        updateProcessor.registerBot(this);
        try {
            var setWebhook = SetWebhook.builder()
                    .url(botUri)
                    .build();
            this.setWebhook(setWebhook);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
         return botName;
    }

    @Override
    public String getBotToken() {
         return botKey;
    }

    public void sendAnswerMessage(SendMessage message) {
        if (Objects.nonNull(message)) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("thrown exception:", e);
            }
        }
    }

    public void sendAnswerEditMessage(EditMessageText message) {
        if (Objects.nonNull(message)) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("thrown exception:", e);
            }
        }
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return null;
    }

    @Override
    public String getBotPath() {
        return "/update";
    }
}
