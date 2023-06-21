package ru.wallentos.carcalculatorbot.service;

import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.KRW;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.USD;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.manualConversionRatesMapInRubles;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.wallentos.carcalculatorbot.configuration.ConfigDatapool;
import ru.wallentos.carcalculatorbot.model.CarPriceResultData;
import ru.wallentos.carcalculatorbot.model.UserCarInputData;

@Service
public class ExecutionService {
    private RestService restService;
    private GoogleService googleService;
    private ConfigDatapool configDatapool;

    @Autowired
    public ExecutionService(RestService restService, ConfigDatapool configDatapool,
                            GoogleService googleService) {
        this.restService = restService;
        this.googleService = googleService;
        this.configDatapool = configDatapool;
        restService.refreshExchangeRates();
        double rub = restService.getConversionRatesMap().get("RUB");
        restService.getConversionRatesMap().forEach((key, value) -> {
            ConfigDatapool.manualConversionRatesMapInRubles.put(key,
                    rub / value);
        });
    }

    public CarPriceResultData executeCarPriceResultData(UserCarInputData userCarInputData) {
        CarPriceResultData resultData = new CarPriceResultData();
        userCarInputData.setSanctionCar(isSanctionCar(userCarInputData.getPrice()));
        resultData.setPrice(userCarInputData.getPrice());
        resultData.setSanctionCar(isSanctionCar(userCarInputData.getPrice()));
        double firstPriceInRubles = calculateFirstCarPriceInRublesByUserCarData(userCarInputData);
        resultData.setFirstPriceInRubles(firstPriceInRubles);
        resultData.setRubleCrypto(firstPriceInRubles * 0.99);
        resultData.setFee(firstPriceInRubles * 0.01);
        if (!resultData.isSanctionCar()) {
            resultData.setFirstPriceInUsd(userCarInputData.getPrice() / restService.getCbrUsdKrwMinus20());
            resultData.setPaymentType("Инвойс");
        } else {
            resultData.setPaymentType("Наличные");
        }
        appendLogGoogleData(resultData);
        return resultData;
    }

    private double calculateFirstCarPriceInRublesByUserCarData(UserCarInputData userCarInputData) {
        if (!userCarInputData.isSanctionCar()) {
            return (userCarInputData.getPrice() / restService.getCbrUsdKrwMinus20())
                    * ConfigDatapool.manualConversionRatesMapInRubles.get(USD);
        } else {
            return userCarInputData.getPrice() * ConfigDatapool.manualConversionRatesMapInRubles.get(KRW);
        }
    }

    private boolean isSanctionCar(double priceInKrw) {
        return priceInKrw / restService.getCbrUsdKrwMinus20() > 50_000;
    }

    //Рубль крипта	Курс	Комиссия	Клиенты	Платежный способ	Тип выдачи
    private void appendLogGoogleData(CarPriceResultData carPriceResultData) {
        int currentIndex = googleService.getCurrentIndex(configDatapool.getLogSpreedSheetId());
        List<List<Object>> inputValues =
                Arrays.asList(
                        Arrays.asList(LocalDate.now().toString(), currentIndex + 1, "",
                                carPriceResultData.getPrice(),
                                carPriceResultData.getFirstPriceInUsd(),
                                carPriceResultData.getFirstPriceInRubles(), carPriceResultData.getRubleCrypto(),
                                manualConversionRatesMapInRubles.get(KRW), carPriceResultData.getFee(),
                                "KOREX", carPriceResultData.getPaymentType(),
                                "Наличные"));
        try {
            //  googleService.getValues(configDatapool.getLogSpreedsheetId());
            googleService.appendValues(configDatapool.getLogSpreedSheetId(), "A1:L1", "USER_ENTERED",
                    inputValues);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
