package ru.wallentos.carcalculatorbot.service;

import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.CNY;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.KRW;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.LAST_FEE_RATE;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.RUB;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.USD;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.feeRateMap;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.wallentos.carcalculatorbot.configuration.ConfigDatapool;
import ru.wallentos.carcalculatorbot.model.CarPriceResultData;
import ru.wallentos.carcalculatorbot.model.UserCarInputData;

@Service
public class ExecutionService {
    private RestService restService;
    private ConfigDatapool configDatapool;

    @Autowired
    public ExecutionService(RestService restService, ConfigDatapool configDatapool) {
        this.restService = restService;
        this.configDatapool = configDatapool;
        restService.refreshExchangeRates();
        double rub = restService.getConversionRatesMap().get("RUB");
        restService.getConversionRatesMap().forEach((key, value) -> {
            ConfigDatapool.manualConversionRatesMapInRubles.put(key,
                    rub * configDatapool.coefficient / value);
        });
    }

    /**
     * Сбор за таможенные операции.
     * Первая составляющая для конечного рассчёта.
     *
     * @param rawCarPriceInEuro
     * @return
     */
    private double getFeeRateFromCarPriceInRubles(double rawCarPriceInEuro) {
        double carPriceInRubles = convertMoneyFromEuro(rawCarPriceInEuro, RUB);
        int resultFeeRate = LAST_FEE_RATE;
        for (Map.Entry<Integer, Integer> pair : feeRateMap.entrySet()) {
            if (carPriceInRubles < pair.getKey()) {
                resultFeeRate = pair.getValue();
                break;
            }
        }
        return resultFeeRate;
    }

    public CarPriceResultData executeCarPriceResultData(UserCarInputData userCarInputData) {
        CarPriceResultData resultData = new CarPriceResultData();
        resultData.setAge(userCarInputData.getAge());
        if (KRW.equals(userCarInputData.getCurrency())) {
            userCarInputData.setSanctionCar(isSanctionCar(userCarInputData.getPrice()));
        }
        resultData.setAge(userCarInputData.getAge());
        resultData.setFeeRate(getFeeRateFromCarPriceInRubles(userCarInputData.getPriceInEuro()));
        resultData.setFirstPriceInRubles(calculateFirstCarPriceInRublesByUserCarData(userCarInputData));
//валютная надбавка  и рублёвая надбавка (Брокерские расходы, СВХ, СБКТС)
        double extraPayAmountRublePart = executeRubExtraPayAmountInRublesByUserCarData(userCarInputData);
        double extraPayAmountCurrencyPart =
                executeValuteExtraPayAmountInRublesByUserCarData(userCarInputData);
        resultData.setExtraPayAmountInRubles(extraPayAmountRublePart);
        resultData.setExtraPayAmountInCurrency(extraPayAmountCurrencyPart);

        resultData.setExtraPayAmount(extraPayAmountRublePart + extraPayAmountCurrencyPart);
        resultData.setStock(executeStock(userCarInputData.getCurrency()));
        resultData.setLocation(executeLocation(userCarInputData.getCurrency()));
        return resultData;
    }

    private double calculateFirstCarPriceInRublesByUserCarData(UserCarInputData userCarInputData) {
        String currentCurrency = userCarInputData.getCurrency();
        if (currentCurrency.equals(KRW) && !userCarInputData.isSanctionCar()) {
            return (userCarInputData.getPrice() / restService.getCbrUsdKrwMinus20())
                    * ConfigDatapool.manualConversionRatesMapInRubles.get(USD);
        } else {
            return userCarInputData.getPrice() * ConfigDatapool.manualConversionRatesMapInRubles.get(currentCurrency);
        }
    }

    private boolean isSanctionCar(double priceInKrw) {
        return priceInKrw / restService.getCbrUsdKrwMinus20() > 50_000;
    }

//стоимость которую ввел пользователь + extra pay 

    //пошлина


    public String executeStock(String currency) {
        switch (currency) {
            case KRW:
            case USD:
                return "Корея";
            default:
                return "Китай";
        }
    }

    public String executeLocation(String currency) {
        switch (currency) {
            case KRW:
            case USD:
                return "до Владивостока";
            default:
                return "до Уссурийска";
        }
    }

    /**
     * Рассчитываем доп взносы. Рублёвая часть. Брокерские расходы, СВХ, СБКТС.
     */
    private double executeRubExtraPayAmountInRublesByUserCarData(UserCarInputData userCarInputData) {
        switch (userCarInputData.getCurrency()) {
            case KRW:
            case USD:
                return configDatapool.EXTRA_PAY_AMOUNT_KOREA_RUB;
            default:
                return configDatapool.EXTRA_PAY_AMOUNT_CHINA_RUB;
        }
    }


    /**
     * Рассчитываем доп взносы. Валютная часть. если тачка дешевле 50 000 USD - то двойная
     * конвертация.
     */
    private double executeValuteExtraPayAmountInRublesByUserCarData(UserCarInputData userCarInputData) {
        switch (userCarInputData.getCurrency()) {
            case KRW:
                return (userCarInputData.isSanctionCar() ?
                        getExtraKrwPayAmountNormalConvertation() :
                        getExtraKrwPayAmountDoubleConvertation());
            case USD:
                return getExtraKrwPayAmountNormalConvertation();
            default:
                return configDatapool.EXTRA_PAY_AMOUNT_CHINA_CNY * configDatapool.manualConversionRatesMapInRubles.get(CNY);
        }
    }

    private double getExtraKrwPayAmountNormalConvertation() {
        return configDatapool.EXTRA_PAY_AMOUNT_KOREA_KRW * configDatapool.manualConversionRatesMapInRubles.get(KRW);
    }

    /**
     * Если эквивалент тачки стоит меньше, чем 50 000$ то (KRW для взносов делим на (курс KRW/USD по
     * ЦБ минус 20) и умножаем на ручной курс USD.
     */
    private double getExtraKrwPayAmountDoubleConvertation() {
        double usdAmount =
                configDatapool.EXTRA_PAY_AMOUNT_KOREA_KRW / restService.getCbrUsdKrwMinus20();
        return usdAmount * ConfigDatapool.manualConversionRatesMapInRubles.get(USD);
    }


    private double convertMoneyFromEuro(double count, String toCurrency) {
        return count * restService.getConversionRatesMap().get(toCurrency);
    }

    public double convertMoneyToEuro(double count, String fromCurrency) {
        return count / restService.getConversionRatesMap().get(fromCurrency);
    }
}
