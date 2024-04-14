package runner.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface MainService {
    void proccessTextMessage(Update update);
}
