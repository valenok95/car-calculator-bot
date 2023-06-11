package ru.wallentos.carcalculatorbot.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
public class MessageUtils {

    public SendMessage generateSendMessageWithText(Message message, String text, InlineKeyboardMarkup
            inlineKeyboardMarkup) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);
        sendMessage.setParseMode(ParseMode.HTML);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        return sendMessage;
    }

    public SendMessage generateSendMessageWithText(Message message, String text) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);
        sendMessage.setParseMode(ParseMode.HTML);
        return sendMessage;
    }

    /**
     * только на callBack
     */
    public EditMessageText generateEditMessageText(String text, Message message) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(message.getChatId());
        editMessage.setText(text);
        editMessage.setMessageId(message.getMessageId());
        return editMessage;
    }
}
