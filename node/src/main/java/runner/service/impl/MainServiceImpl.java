package runner.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import runner.dao.AppUserDAO;
import runner.dao.JokeDAO;
import runner.dao.RawDataDAO;
import runner.entity.AppUser;
import runner.entity.RawData;
import runner.entity.Joke;
import runner.service.MainService;
import runner.service.ProducerService;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static runner.entity.enums.UserState.BASIC_STATE;
import static runner.service.enums.ServiceCommands.*;

@Service
@Log4j
public class MainServiceImpl implements MainService{
    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final JokeDAO jokeDAO;

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerService producerService, AppUserDAO appUserDAO, JokeDAO jokeDAO) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
        this.jokeDAO = jokeDAO;
    }
    private AppUser findOrSaveAppUser(Update update){
        var telegramUser = update.getMessage().getFrom();
        AppUser persistentAppUser = appUserDAO.findAppUsersByTelegramUserId(telegramUser.getId());
        if (persistentAppUser == null){
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .userName(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(true)
                    .state(BASIC_STATE)
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return persistentAppUser;
    }

    @Override
    public void proccessTextMessage(Update update){
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "Что-то пошло не так...";
        output = processServiceCommand(update, text);

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    private void sendAnswer(String output, Long chatId) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.produceAnswer(sendMessage);
    }

    private String processServiceCommand(Update update, String cmd) {
        String text = update.getMessage().getText();

        if (HELP.equals(cmd)){
            return help();
        } else if (START.equals(cmd)) {
            return "Доброго времени суток! Чтобы посмотреть список команд, введите /help";
        } else if (text.contains("/post")) {
            return postjoke(update);
        } else if (text.contains("/put")) {
            return putjoke(update);
        } else if (GETALL.equals(cmd)) {
            return getall(update);
        } else if (text.contains("/get")) {
            return getjoke(update);
        } else if (text.contains("/delete")) {
            return deletejoke(update);
        } else {
            return "Неизвестная команда! Чтобы посмотреть список доступных команд, введите /help";
        }
    }

    private String deletejoke(Update update) {
        try{
        String idjoke = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        Optional <Joke> joke = jokeDAO.findById(Long.parseLong(idjoke));
            if(joke.isPresent()) {
                jokeDAO.delete(joke.get());
                return "Шутка успешно удалена";
            } else {
                return "Шутка с указанным id не найдена";
            }
        }
        catch (NumberFormatException e){
            return "Произошла ошибка!\nУбедитесь, что написали в команде лишь 1 пробел";
        }
    }

    private String getjoke(Update update) {
        try{
        String id = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        Optional <Joke> joke = jokeDAO.findById(Long.parseLong(id));
        var changeDate = joke.get().getChangeDate();
        if (changeDate == null) return joke.get().getText().toString() + "\nДата создания " +  joke.get().getCreationDate() + "\nДата изменения ---";
        else return joke.get().getText().toString() + "\nДата создания " +  joke.get().getCreationDate() + "\nДата изменения " + joke.get().getChangeDate();}
        catch (NumberFormatException e){
            return "Произошла ошибка!\nУбедитесь, что написали в команде лишь 1 пробел";
        }
    }

    private String getall(Update update) {
        var chatId = update.getMessage().getChatId();
        List<Joke> jokes = jokeDAO.findAll();
        for (Joke joke : jokes){
            var changeDate = joke.getChangeDate();
            if (changeDate == null) sendAnswer("Id: " + joke.getId().toString() + "\n" + joke.getText().toString() + "\nДата создания " + joke.getCreationDate().toString() + "\nДата изменения ---", chatId);
            else sendAnswer("Id: " + joke.getId().toString() + "\n" + joke.getText().toString() + "\nДата создания " + joke.getCreationDate().toString() + "\nДата изменения " + joke.getChangeDate().toString(), chatId);
        }
        return "\nВыведены все шутки";
    }

    private String putjoke(Update update) {
        try {
            String[] data = update.getMessage().getText().split(" ");
            if(data.length < 3) {
                return "Произошла ошибка!\nУбедитесь, что в команде есть id и новый текст шутки, разделенные пробелом";
            }
            Long id = Long.parseLong(data[1]);
            String newText = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ", update.getMessage().getText().indexOf(" ") + 1) + 1);

            Optional<Joke> jokeOptional = jokeDAO.findById(id);
            if(jokeOptional.isPresent()) {
                Joke joke = jokeOptional.get();
                joke.setText(newText);
                joke.setChangeDate(LocalDate.now());
                jokeDAO.save(joke);

                return "Шутка с id " + id + " успешно изменена";
            } else {
                return "Шутка с id " + id + " не найдена";
            }
        } catch (NumberFormatException e) {
            return "Произошла ошибка!\nУбедитесь, что написали в команде id числом";
        }
    }

    private String postjoke(Update update) {
        var text = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        if (text.contains("/post")) return "Вы не ввели шутку!";
        LocalDate creationDate = LocalDate.now();
        return "Ваша шутка: " + text + "\nId шутки: " + saveJoke(text).toString() + "\nДата создания " + creationDate;
    }

    private String help() {
        return "Список доступных команд:\n"
                + "/post <шутка> - добавить шутку\n"
                + "/get <id шутки> - вывести шутку по id\n"
                + "/getall - вывести все шутки\n"
                + "/put <id шутки> <Изменённая шутка>- изменить шутку\n"
                + "/delete <id шутки> - удалить шутку";
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDAO.save(rawData);
    }
    private Long saveJoke(String text){
        Joke joke = Joke.builder()
                .text(text)
                .creationDate(LocalDate.now())
                .changeDate(null)
                .build();
        jokeDAO.save(joke);
        return joke.getId();
    }
}
