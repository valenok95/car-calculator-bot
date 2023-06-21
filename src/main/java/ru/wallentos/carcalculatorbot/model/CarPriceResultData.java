package ru.wallentos.carcalculatorbot.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarPriceResultData {
    private double price;
    private double firstPriceInUsd;
    private double firstPriceInRubles;
    private double rubleCrypto;
    private double fee;
    private boolean isSanctionCar;
    private String paymentType;
}