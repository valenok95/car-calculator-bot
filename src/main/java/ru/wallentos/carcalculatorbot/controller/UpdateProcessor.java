package ru.wallentos.carcalculatorbot.controller;

import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.CNY;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.KRW;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.NEW_CAR;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.NORMAL_CAR;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.OLD_CAR;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.RESET_MESSAGE;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.TO_SET_CURRENCY_MENU;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.TO_START_MESSAGE;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.USD;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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
        if (callbackData.equals(RESET_MESSAGE) || callbackData.equals(TO_START_MESSAGE)) {
            startCommandReceived(update);
            return;
        }
        if (callbackData.equals(TO_SET_CURRENCY_MENU)) {
            setCurrencyCommandReceived(update);
            return;
        }
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
        var sendMessage = messageUtils.generateSendMessageWithText(update,
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
            case "/cbr":
                cbrCommandReceived(update);
                break;
            case "/currencyrates":
                currencyRatesCommandReceived(update);
                break;
            case "/settingservice":
                setCurrencyCommandReceived(update);
                break;
            default:
                handleMessage(receivedText, update);
                break;
        }
    }

    private void startCommandReceived(Update update) {
        restService.refreshExchangeRates();
        String message = String.format("""
                Здравствуйте, %s!
                        
                Для расчета автомобиля из южной Кореи выберите KRW, для автомобиля из Китая CNY.
                """, update.getMessage().getChat().getFirstName());
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton cnyButton = new InlineKeyboardButton(CNY);
        InlineKeyboardButton krwButton = new InlineKeyboardButton(KRW);
        cnyButton.setCallbackData(CNY);
        krwButton.setCallbackData(KRW);
        row.add(krwButton);
        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);
        setView(messageUtils.generateSendMessageWithText(update, message, inlineKeyboardMarkup));
        cache.setUsersCurrentBotState(update.getMessage().getChatId(), BotState.ASK_CURRENCY);
    }

    private void cbrCommandReceived(Update update) {
        restService.refreshExchangeRates();
        Map<String, Double> rates = restService.getConversionRatesMap();
        String message = """
                Курс валют ЦБ:
                                
                EUR %,.4fруб.
                USD %,.4fруб.
                CNY %,.4fруб.
                KRW %,.4fруб.
                                
                """.formatted(rates.get("RUB"),
                rates.get("RUB") / rates.get("USD"),
                rates.get("RUB") / rates.get("CNY"),
                rates.get("RUB") / rates.get("KRW"));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton reset = new InlineKeyboardButton(TO_START_MESSAGE);
        reset.setCallbackData(TO_START_MESSAGE);
        row1.add(reset);
        rows.add(row1);
        inlineKeyboardMarkup.setKeyboard(rows);
        setView(messageUtils.generateSendMessageWithText(update, message,
                inlineKeyboardMarkup));
    }

    private void currencyRatesCommandReceived(Update update) {
        //TO DO вынести в отдельный метод String get
        String message = String.format("""
                        Актуальный курс оплаты:
                                                    
                        KRW = %,.4f RUB
                        CNY = %,.4f RUB
                        USD = %,.4f RUB
                        USD = %,.2f KRW
                            
                            """,
                ConfigDatapool.manualConversionRatesMapInRubles.get(KRW),
                ConfigDatapool.manualConversionRatesMapInRubles.get(CNY),
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

        setView(messageUtils.generateSendMessageWithText(update, message, inlineKeyboardMarkup));
    }

    private void setCurrencyCommandReceived(Update update) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton cnyButton = new InlineKeyboardButton(CNY);
        InlineKeyboardButton usdButton = new InlineKeyboardButton(USD);
        InlineKeyboardButton krwButton = new InlineKeyboardButton(KRW);
        usdButton.setCallbackData(USD);
        cnyButton.setCallbackData(CNY);
        krwButton.setCallbackData(KRW);
        row.add(usdButton);
        row.add(krwButton);
        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);
        String message;
        message = String.format("""
                        Актуальный курс оплаты:
                                                    
                        KRW = %,.4f
                        CNY = %,.4f
                        USD = %,.4f
                            
                        Выберите валюту для ручной установки курса:
                            """,
                ConfigDatapool.manualConversionRatesMapInRubles.get(KRW),
                ConfigDatapool.manualConversionRatesMapInRubles.get(CNY),
                ConfigDatapool.manualConversionRatesMapInRubles.get(USD));

        setView(messageUtils.generateSendMessageWithText(update, message, inlineKeyboardMarkup));
        cache.setUsersCurrentBotState(update.getMessage().getChatId(), BotState.SET_CURRENCY_MENU);
    }

    private void handleMessage(String receivedText, Update update) {
        long chatId = update.getMessage().getChatId();
        BotState currentState = cache.getUsersCurrentBotState(chatId);
        try {
            switch (currentState) {
                case ASK_CURRENCY:
                    processCurrency(update, receivedText);
                    break;
                case ASK_PRICE:
                    processPrice(update, receivedText);
                    break;
                case ASK_ISSUE_DATE:
                    processIssueDate(update, receivedText);
                    break;
                case ASK_VOLUME:
                    processVolume(update, receivedText);
                    break;
                case SET_CURRENCY_MENU:
                    processChooseCurrencyToSet(update, receivedText);
                    break;
                case SET_CURRENCY:
                    processSetCurrency(update, receivedText);
                    break;
                default:
                    break;
            }
        } catch (IllegalArgumentException e) {
            setView(messageUtils.generateSendMessageWithText(update, "Некорректный формат данных, " +
                    "попробуйте ещё раз."));
            return;
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
        setView(messageUtils.generateSendMessageWithText(update, message, inlineKeyboardMarkup));
        cache.deleteUserCarDataByUserId(chatId);
    }

    private void processPrice(Update update, String receivedText) {
        long chatId = update.getMessage().getChatId();
        UserCarInputData data = cache.getUserCarData(chatId);
        int priceInCurrency = Integer.parseInt(receivedText);
        data.setPrice(priceInCurrency);
        data.setPriceInEuro(executionService.convertMoneyToEuro(priceInCurrency, data.getCurrency()));
        cache.saveUserCarData(chatId, data);
        cache.setUsersCurrentBotState(chatId, BotState.ASK_ISSUE_DATE);
        String text = "Выберите возраст автомобиля:";
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton newCar = new InlineKeyboardButton(NEW_CAR);
        InlineKeyboardButton normalCar = new InlineKeyboardButton(NORMAL_CAR);
        InlineKeyboardButton oldCar = new InlineKeyboardButton(OLD_CAR);
        newCar.setCallbackData(NEW_CAR);
        normalCar.setCallbackData(NORMAL_CAR);
        oldCar.setCallbackData(OLD_CAR);
        row.add(newCar);
        row.add(normalCar);
        row.add(oldCar);
        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);
        setView(messageUtils.generateSendMessageWithText(update, text, inlineKeyboardMarkup));
    }

    private void processIssueDate(Update update, String receivedText) {
        long chatId = update.getMessage().getChatId();
        UserCarInputData data = cache.getUserCarData(chatId);
        data.setAge(receivedText);
        cache.saveUserCarData(chatId, data);
        cache.setUsersCurrentBotState(chatId, BotState.ASK_VOLUME);
        String text = """
                Введите объем двигателя в кубических сантиметрах.
                                
                Пример: 1995""";

        setView(messageUtils.generateSendMessageWithText(update, text));
    }

    private void processVolume(Update update, String receivedText) {
        long chatId = update.getMessage().getChatId();
        UserCarInputData data = cache.getUserCarData(chatId);
        data.setVolume(Integer.parseInt(receivedText));
        cache.saveUserCarData(chatId, data);
        cache.setUsersCurrentBotState(chatId, BotState.DATA_PREPARED);
        String text = String.format("""
                Данные переданы в обработку ⏳
                 
                %s
                """, data);
        setView(messageUtils.generateSendMessageWithText(update, text));
        processExecuteResult(data, update);
    }

    private void processExecuteResult(UserCarInputData data, Update update) {
        long chatId = update.getMessage().getChatId();
        CarPriceResultData resultData = executionService.executeCarPriceResultData(data);
        cache.deleteUserCarDataByUserId(chatId);
        log.info("""
                        Данные рассчёта:
                        First price in rubles {},
                        Extra pay amount RUB {},
                        Extra pay amount curr {},
                        Extra pay amount {},
                        Fee rate {},
                        Duty {},
                        Recycling fee {}
                        """, resultData.getFirstPriceInRubles(), resultData.getExtraPayAmountInRubles(),
                resultData.getExtraPayAmountInCurrency(), resultData.getExtraPayAmount(),
                resultData.getFeeRate(), resultData.getDuty(), resultData.getRecyclingFee());
        String text;
        text = String.format("""
                %s
                        
                Что бы заказать авто - пиши менеджеру🔻
                        """, resultData);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton reset = new InlineKeyboardButton(RESET_MESSAGE);
        reset.setCallbackData(RESET_MESSAGE);
        row2.add(reset);
        rows.add(row2);
        inlineKeyboardMarkup.setKeyboard(rows);
        setView(messageUtils.generateSendMessageWithText(update, text, inlineKeyboardMarkup));
    }


    private void processCurrency(Update update, String currency) {
        long chatId = update.getMessage().getChatId();
        UserCarInputData data = cache.getUserCarData(chatId);
        data.setCurrency(currency);
        data.setStock(executionService.executeStock(currency));
        cache.saveUserCarData(chatId, data);
        String text =
                String.format("""
                                Тип валюты: %s 
                                                                
                                Теперь введите стоимость автомобиля в валюте.
                                """
                        , currency);
        setView(messageUtils.generateEditMessageText(text, update));
        cache.setUsersCurrentBotState(chatId, BotState.ASK_PRICE);
    }

    private void processChooseCurrencyToSet(Update update, String currency) {
        long chatId = update.getMessage().getChatId();
        String text =
                String.format("""
                                Вы выбрали тип валюты: %s 
                                                                
                                Теперь введите курс валюты к рублю.
                                                                
                                Например 1.234
                                В таком случае будет установлен курс 1 %s = 1.234 RUB
                                """
                        , currency, currency);
        setView(messageUtils.generateEditMessageText(text, update));
        UserCarInputData data = cache.getUserCarData(chatId);
        data.setCurrency(currency);
        cache.saveUserCarData(chatId, data);
        cache.setUsersCurrentBotState(chatId, BotState.SET_CURRENCY);
    }


}
