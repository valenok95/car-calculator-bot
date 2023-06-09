package ru.wallentos.carcalculatorbot.cache;

import ru.wallentos.carcalculatorbot.model.BotState;
import ru.wallentos.carcalculatorbot.model.UserCarInputData;

public interface DataCache {
    void setUsersCurrentBotState(long userId, BotState botState);

    BotState getUsersCurrentBotState(long userId);

    UserCarInputData getUserCarData(long userId);

    void saveUserCarData(long userId, UserCarInputData userCarInputData);

    void deleteUserCarDataByUserId(long userId);
}
