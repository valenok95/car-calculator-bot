package ru.wallentos.carcalculatorbot.configuration;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ConfigDatapool {
    public static final String EUR = "EUR";
    public static final String RUB = "RUB";
    public static final String USD = "USD";
    public static final String KRW = "KRW";
    public static final String CNY = "CNY";
    @Value("${ru.wallentos.car-calculator.log-spreedsheed-id}")
    public String logSpreedSheetId;
    @Value("${ru.wallentos.car-calculator.fee}")
    public double fee;
    public static Map<String, Double> manualConversionRatesMapInRubles = new HashMap<>();
    public static final String RESET_MESSAGE = "Расчёт платежа";
    public static final String TO_START_MESSAGE = "Расчёт платежа";
    public static final String TO_SET_CURRENCY_MENU = "Меню установки валюты";
    public static final String MANAGER_MESSAGE = "Связаться с менеджером";

}
