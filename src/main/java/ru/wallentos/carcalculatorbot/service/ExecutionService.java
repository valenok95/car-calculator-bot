package ru.wallentos.carcalculatorbot.service;

import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.KRW;
import static ru.wallentos.carcalculatorbot.configuration.ConfigDatapool.USD;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.wallentos.carcalculatorbot.configuration.ConfigDatapool;
import ru.wallentos.carcalculatorbot.model.CarPriceResultData;
import ru.wallentos.carcalculatorbot.model.UserCarInputData;

@Service
public class ExecutionService {
    private RestService restService;

    @Autowired
    public ExecutionService(RestService restService, ConfigDatapool configDatapool) {
        this.restService = restService;
        restService.refreshExchangeRates();
        double rub = restService.getConversionRatesMap().get("RUB");
        restService.getConversionRatesMap().forEach((key, value) -> {
            ConfigDatapool.manualConversionRatesMapInRubles.put(key,
                    rub * configDatapool.coefficient / value);
        });
    }

    public CarPriceResultData executeCarPriceResultData(UserCarInputData userCarInputData) {
        CarPriceResultData resultData = new CarPriceResultData();
        userCarInputData.setSanctionCar(isSanctionCar(userCarInputData.getPrice()));
        resultData.setPrice(userCarInputData.getPrice());
        resultData.setSanctionCar(isSanctionCar(userCarInputData.getPrice()));
        resultData.setFirstPriceInRubles(calculateFirstCarPriceInRublesByUserCarData(userCarInputData));
        if (!resultData.isSanctionCar()) {
            resultData.setFirstPriceInUsd(userCarInputData.getPrice() / restService.getCbrUsdKrwMinus20());
        }

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
}
