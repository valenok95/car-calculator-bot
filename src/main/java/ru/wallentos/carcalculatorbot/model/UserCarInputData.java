package ru.wallentos.carcalculatorbot.model;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCarInputData {
    boolean isSanctionCar;
    String currency;
    int price;
    double priceInEuro;
    LocalDate issueDate;
    int volume;
    String age;
    String stock;

    @Override
    public String toString() {
        return String.format("""
                Возраст: %s.
                Стоимость: %d %s\s
                Объем двигателя: %d с.с.""", age, price, currency, volume);
    }

}