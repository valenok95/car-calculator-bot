package ru.wallentos.carcalculatorbot.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CarPriceResultData {
    double price;
    double firstPriceInUsd;
    double firstPriceInRubles;
    boolean isSanctionCar;
}