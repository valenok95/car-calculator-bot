package ru.wallentos.carcalculatorbot.controller;

import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.KRW;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.RESET_MESSAGE;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.TO_SET_CURRENCY_MENU;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.TO_START_MESSAGE;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.USD;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.manualConversionRatesMapInRubles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.wallentos.carcalculatorbot.cache.UserDataCache;
import ru.wallentos.carcalculatorbot.configuration.ConfigDatapool;
import ru.wallentos.carcalculatorbot.model.BotState;
import ru.wallentos.carcalculatorbot.model.CarPriceResultData;
import ru.wallentos.carcalculatorbot.model.UserCarInputData;
import ru.wallentos.carcalculatorbot.service.ExecutionService;
import ru.wallentos.carcalculatorbot.service.RestService;
import ru.wallentos.carcalculatorbot.utils.MessageUtils;

@Component
@Slf4j
public class UpdateProcessor {
    private TelegramBot telegramBot;
    @Autowired
    private ExecutionService executionService;
    @Autowired
    private MessageUtils messageUtils;
    @Autowired
    private UserDataCache cache;
    @Autowired
    private RestService restService;

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update) {
        if (Objects.isNull(update)) {
            log.error("received null update");
            return;
        }
        if (update.hasCallbackQuery()) {
            processCallback(update);
        }
        if (Objects.nonNull(update.getMessage())) {
            distributeMessagesByType(update);
        } else {
            log.error("Received unsupported message type {}", update);
        }
    }

    private void processCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        handleCallbackData(callbackData, update);
    }

    private void distributeMessagesByType(Update update) {
        var message = update.getMessage();
        if (Objects.nonNull(message.getText())) {
            processTextMessage(update);
        } else if (Objects.nonNull(message.getDocument())) {
            processDocMessage(update);
        } else if (Objects.nonNull(message.getPhoto())) {
            processPhotoMessage(update);
        } else {
            setUnsupportedMessageTypeView(update);
        }
    }

    private void setUnsupportedMessageTypeView(Update update) {
        var sendMessage = messageUtils.generateSendMessageWithText(update.getMessage(),
                "Неподдерживаемый тип сообщения!");
        setView(sendMessage);
    }

    private void setView(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }

    private void setView(EditMessageText editMessageText) {
        telegramBot.sendAnswerEditMessage(editMessageText);
    }

    private void processPhotoMessage(Update update) {

    }

    private void processDocMessage(Update update) {
    }

    /**
     * Рассчитать стоимость
     * Установить курс
     * Посмотреть курс
     */
    private void processTextMessage(Update update) {
        String receivedText = update.getMessage().getText();
        log.info("message received: " + receivedText);
        switch (receivedText) {
            case "/start":
                startCommandReceived(update);
                break;
            case "/currencyrates":
                currencyRatesCommandReceived(update);
                break;
            case "/settingservice":
                setCurrencyCommandReceived(update.getMessage());
                break;
            default:
                handleMessage(receivedText, update);
                break;
        }
    }

    private void startCommandReceived(Update update) {
        processReadPrice(update);
        restService.refreshExchangeRates();
    }

    private void currencyRatesCommandReceived(Update update) {
        String message = String.format("""
                        Актуальный курс оплаты:
                                                    
                        KRW = %,.4f RUB
                        USD = %,.4f RUB
                        USD = %,.2f KRW
                            
                            """,
                ConfigDatapool.manualConversionRatesMapInRubles.get(KRW),
                ConfigDatapool.manualConversionRatesMapInRubles.get(USD),
                restService.getCbrUsdKrwMinus20());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton reset = new InlineKeyboardButton(TO_START_MESSAGE);
        reset.setCallbackData(TO_START_MESSAGE);
        row1.add(reset);
        rows.add(row1);
        inlineKeyboardMarkup.setKeyboard(rows);
        setView(messageUtils.generateSendMessageWithText(update.getMessage(), message, inlineKeyboardMarkup));
        restService.refreshExchangeRates();
    }

    private void setCurrencyCommandReceived(Message message) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton usdButton = new InlineKeyboardButton(USD);
        InlineKeyboardButton krwButton = new InlineKeyboardButton(KRW);
        usdButton.setCallbackData(USD);
        krwButton.setCallbackData(KRW);
        row.add(usdButton);
        row.add(krwButton);
        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);
        String text = String.format("""
                        Актуальный курс оплаты:
                                                    
                        KRW = %,.4f
                        USD = %,.4f
                        USD/KRW = %,.2f
                            
                        Выберите валюту для ручной установки курса:
                            """,
                ConfigDatapool.manualConversionRatesMapInRubles.get(KRW),
                ConfigDatapool.manualConversionRatesMapInRubles.get(USD),
                restService.getCbrUsdKrwMinus20());

        setView(messageUtils.generateSendMessageWithText(message, text, inlineKeyboardMarkup));
        cache.setUsersCurrentBotState(message.getChatId(), BotState.SET_CURRENCY_MENU);
    }

    private void handleCallbackData(String callbackData, Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        BotState currentState = cache.getUsersCurrentBotState(chatId);
        switch (callbackData) {
            case TO_START_MESSAGE:
            case RESET_MESSAGE:
                startCommandReceived(update);
                return;
            case TO_SET_CURRENCY_MENU:
                setCurrencyCommandReceived(update.getCallbackQuery().getMessage());
                return;
        }
        try {
            switch (currentState) {
                case ASK_CURRENCY:
                    processReadPrice(update);
                    break;
                case SET_CURRENCY_MENU:
                    processChooseCurrencyToSet(update, callbackData);
                    break;
                default:
                    break;
            }
        } catch (IllegalArgumentException e) {
            setView(messageUtils.generateSendMessageWithText(update.getCallbackQuery().getMessage(),
                    "Некорректный формат " +
                            "данных, " +
                            "попробуйте ещё раз."));
        }
    }

    private void handleMessage(String receivedText, Update update) {
        long chatId = update.getMessage().getChatId();
        BotState currentState = cache.getUsersCurrentBotState(chatId);
        try {
            switch (currentState) {
                case ASK_PRICE:
                    processPrice(update, receivedText);
                    break;
                case SET_CURRENCY:
                    processSetCurrency(update, receivedText);
                    break;
                default:
                    break;
            }
        } catch (IllegalArgumentException e) {
            setView(messageUtils.generateSendMessageWithText(update.getMessage(), "Некорректный формат " +
                    "данных, " +
                    "попробуйте ещё раз."));
        }
    }

    private void processSetCurrency(Update update, String receivedText) {
        long chatId = update.getMessage().getChatId();
        String currency = cache.getUserCarData(chatId).getCurrency();
        receivedText = receivedText.replace(',', '.');
        ConfigDatapool.manualConversionRatesMapInRubles.put(currency, Double.valueOf(receivedText));
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton reset = new InlineKeyboardButton(TO_START_MESSAGE);
        InlineKeyboardButton toSetCurrencyMenu =
                new InlineKeyboardButton(TO_SET_CURRENCY_MENU);
        toSetCurrencyMenu.setCallbackData(TO_SET_CURRENCY_MENU);
        reset.setCallbackData(TO_START_MESSAGE);
        row1.add(toSetCurrencyMenu);
        row2.add(reset);
        rows.add(row1);
        rows.add(row2);
        inlineKeyboardMarkup.setKeyboard(rows);

        String message = String.format("Установлен курс: 1 %s = %s RUB", currency, receivedText);
        setView(messageUtils.generateSendMessageWithText(update.getMessage(), message, inlineKeyboardMarkup));
        cache.deleteUserCarDataByUserId(chatId);
        restService.refreshExchangeRates();
    }

    private void processPrice(Update update, String receivedText) {
        long chatId = update.getMessage().getChatId();
        UserCarInputData data = cache.getUserCarData(chatId);
        int priceInCurrency = Integer.parseInt(receivedText);
        data.setPrice(priceInCurrency);
        cache.saveUserCarData(chatId, data);
        CarPriceResultData resultData = executionService.executeCarPriceResultData(data);
        cache.deleteUserCarDataByUserId(chatId);
        if (resultData.isSanctionCar()) {
            processShowResultForNormalConvertation(update, resultData);
        } else {
            processShowResultForDoubleConvertation(update, resultData);
        }
    }

    private void processShowResultForNormalConvertation(Update update, CarPriceResultData resultData) {
        String firstMessage = String.format(Locale.FRANCE, """
                        #Наличные - тачка дороже 50к
                        %,.0f KRW
                        %,.0f RUB
                        """,
                resultData.getPrice(),
                resultData.getFirstPriceInRubles());
        String secondMessage = String.format(Locale.FRANCE, """
                        #Наличные - тачка дороже 50к
                        %,.0f KRW
                        %,.0f RUB
                                                
                        Рубль/Крипта %,.0f RUB
                        Комиссия %,.0f 
                        KRW/RUB %,.4f RUB
                        KRW/RUB (себес) %,.4f
                                                
                        """,
                resultData.getPrice(),
                resultData.getFirstPriceInRubles(),
                resultData.getFirstPriceInRubles() * 0.99,
                resultData.getFirstPriceInRubles() * 0.01,
                manualConversionRatesMapInRubles.get(KRW),
                manualConversionRatesMapInRubles.get(KRW) * 0.99);


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton reset = new InlineKeyboardButton(RESET_MESSAGE);
        reset.setCallbackData(RESET_MESSAGE);
        row2.add(reset);
        rows.add(row2);
        inlineKeyboardMarkup.setKeyboard(rows);
        setView(messageUtils.generateSendMessageWithText(update.getMessage(),
                firstMessage));
        setView(messageUtils.generateSendMessageWithText(update.getMessage(),
                secondMessage,
                inlineKeyboardMarkup));
    }

    private void processShowResultForDoubleConvertation(Update update,
                                                        CarPriceResultData resultData) {
        String firstMessage = String.format(Locale.FRANCE, """
                        #Инвойс - тачка дешевле 50к
                        %,.0d KRW
                        %,.0f USD
                        %,.0f RUB
                        """, resultData.getPrice(), resultData.getFirstPriceInUsd(),
                resultData.getFirstPriceInRubles());

        String secondMessage = String.format(Locale.FRANCE, """
                        #Инвойс - тачка дешевле 50к
                        %,.0d KRW
                        %,.0f USD
                        %,.0f RUB
                                                
                        Рубль/Крипта %,.0f RUB
                        Комиссия %,.0f 
                        KRW/USD %,.2f RUB
                        USD/RUB %,.4f RUB
                        USD/RUB (себес) %,.4f
                        """,
                resultData.getPrice(), resultData.getFirstPriceInUsd(),
                resultData.getFirstPriceInRubles(),
                resultData.getFirstPriceInRubles() * 0.99,
                resultData.getFirstPriceInRubles() * 0.01,
                restService.getCbrUsdKrwMinus20(),
                manualConversionRatesMapInRubles.get(USD),
                manualConversionRatesMapInRubles.get(USD) * 0.99);


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton reset = new InlineKeyboardButton(RESET_MESSAGE);
        reset.setCallbackData(RESET_MESSAGE);
        row2.add(reset);
        rows.add(row2);
        inlineKeyboardMarkup.setKeyboard(rows);
        setView(messageUtils.generateSendMessageWithText(update.getMessage(),
                firstMessage));
        setView(messageUtils.generateSendMessageWithText(update.getMessage(),
                secondMessage,
                inlineKeyboardMarkup));
    }

    /**
     * стартовый процесс
     */
    private void processReadPrice(Update update) {
        Message message = update.hasCallbackQuery() ? update.getCallbackQuery().getMessage() :
                update.getMessage();

        long chatId = message.getChatId();
        UserCarInputData data = cache.getUserCarData(chatId);
        cache.saveUserCarData(chatId, data);
        String text =
                String.format("""
                        Тип валюты: KRW
                                                        
                        Теперь введите стоимость автомобиля в валюте.
                        """);
        setView(messageUtils.generateSendMessageWithText(message, text));
        cache.setUsersCurrentBotState(chatId, BotState.ASK_PRICE);
    }

    private void processChooseCurrencyToSet(Update update, String currency) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String text =
                String.format("""
                                Вы выбрали тип валюты: %s 
                                                                
                                Теперь введите курс валюты к рублю.
                                                                
                                Например 1.234
                                В таком случае будет установлен курс 1 %s = 1.234 RUB
                                """
                        , currency, currency);
        setView(messageUtils.generateEditMessageText(text, update.getCallbackQuery().getMessage()));
        UserCarInputData data = cache.getUserCarData(chatId);
        data.setCurrency(currency);
        cache.saveUserCarData(chatId, data);
        cache.setUsersCurrentBotState(chatId, BotState.SET_CURRENCY);
    }
}
