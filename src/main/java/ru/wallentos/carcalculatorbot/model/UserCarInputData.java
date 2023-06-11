package ru.wallentos.carcalculatorbot.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCarInputData {
    boolean isSanctionCar;
    int price;
    String currency;
}