package KitchenBot;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class UserHandler {

    private final Bot bot;
    private final User user;
    private final Bot.UserStatus userStatus;
    private Update update;
    private boolean greeted = false;
    public User getUser() { return user; }
    private String messageText;
    private Mode mode = Mode.WAITING;
    private Event event;

    HashMap<Long, String> userNames;
    ArrayList<Long> ids;
    LinkedList<Long> queue;

    private enum Mode {
        SENDALL, WAITING, ENTERDELAY, SWAP, DELETEUSER, SETADMIN, CREATEEVENT
    }

    public UserHandler(Bot bot, User user, Bot.UserStatus userStatus) {
        this.bot = bot;
        this.user = user;
        this.userStatus = userStatus;
    }

    public void sendText(String what) {
        bot.sendText(user.getId(), what);
    }
    public void sendMenu(ReplyKeyboardMarkup keyboard, String text) {
        bot.sendMenu(user.getId(), text, keyboard);
    }

    public void sendUserList() {
        StringBuilder builder = new StringBuilder();
        userNames = bot.getUserNames();
        queue = bot.getQueue();
        int counter = 1;
        ids = new ArrayList<>(queue);
        ids.remove(user.getId());

        for (long userId: ids) {
            builder.append(counter)
                    .append(") ")
                    .append(userNames.get(userId))
                    .append("\n");
            counter++;
        }
        sendText(builder.toString());
    }

    public boolean swapPlaces() {
        if (bot.getQueue().contains(user.getId())) {
            sendText("С кем ты хочешь поменяться?");

            sendUserList();
            mode = Mode.SWAP;
            return true;
        } else {
            sendText("Тебя нет в очереди");
            return false;
        }

    }

    public void pass(Update update) throws SQLException {

        this.update = update;
        messageText = update.getMessage().getText();

        if (!greeted) {
            sendText("Привет, " + user.getUserName());
            sendMenu(Keyboards.getMenu(userStatus), "Что бы ты хотел сделать?");
            greeted = true;
            return;
        }

        switch (mode) {

            case CREATEEVENT -> {

                if (messageText.equalsIgnoreCase("Отмена")) {
                    mode = Mode.WAITING;
                    sendText("Что-то еще?");
                    return;
                }

                if (event == null) {
                    event = new Event(messageText);
                    event.setReason(messageText);
                    sendText("Когда состоится? (дд.мм)");
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                LocalDate date;
                try {
                    String dateString = messageText + "." + LocalDate.now().getYear();
                    date = LocalDate.parse(dateString, formatter);
                } catch (Exception e) {
                    System.out.println("Введи нормальную дату");
                    return;
                }
                bot.addEvent(date, event);
                sendText("Добавил событие");
                sendText("Что-то еще?");
                event = null;
                mode = Mode.WAITING;
            }

            case SETADMIN -> {
                if (messageText.equalsIgnoreCase("Отмена")) {
                    mode = Mode.WAITING;
                    sendText("Что-то еще?");
                    return;
                }

                int chosenNumber;
                try {
                    chosenNumber = Integer.parseInt(messageText);
                    if (chosenNumber < 1 || chosenNumber > ids.size()) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    sendText("Введи корректный номер");
                    break;
                }
                Long destination = ids.get(chosenNumber-1);

                bot.grantAdmin(user.getId(), destination);
                bot.sendText(destination, "Ты новый ответственный. Можешь убедиться, перезапустив бота");
                sendText("Права переданы. Сейчас я перезапущу твой handler");
                bot.turnHandlerOff(this);
            }

            case DELETEUSER -> {
                if (messageText.equalsIgnoreCase("Отмена")) {
                    mode = Mode.WAITING;
                    sendText("Что-то еще?");
                    return;
                }
                int chosenNumber;
                try {
                    chosenNumber = Integer.parseInt(messageText);
                    if (chosenNumber < 1 || chosenNumber > ids.size()) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    sendText("Введи корректный номер");
                    break;
                }
                Long destination = ids.get(chosenNumber-1);

                bot.sendText(destination, "Админ почему-то тебя удалил");
                bot.removeUser(destination);
                sendText("Готово");
                mode = Mode.WAITING;
            }

            case SWAP -> {
                if (messageText.equalsIgnoreCase("Отмена")) {
                    mode = Mode.WAITING;
                    sendText("Что-то еще?");
                    return;
                }
                int chosenNumber;
                try {
                    chosenNumber = Integer.parseInt(messageText);
                    if (chosenNumber < 1 || chosenNumber > ids.size()) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    sendText("Введи корректный номер");
                    break;
                }
                Long destination = ids.get(chosenNumber-1);
                sendText("Отправил твой запрос " + userNames.get(destination));

                LocalDate date1 = bot.getDutyDate(user.getId());
                LocalDate date2 = bot.getDutyDate(destination);
                System.out.println(date1);
                System.out.println(date2);
//
                bot.sendText(destination, userNames.get(user.getId()) + " хочет поменяться с тобой местами. Согласен? (да/нет)");
                bot.addModal(destination, (String message) -> {
                    if (message.equalsIgnoreCase("да")) {
                        bot.swapUsers(date1, date2);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
                        LocalDate date = bot.getDutyDate(destination);
                        bot.sendText(destination, "Теперь ты дежуришь " + date.format(formatter));

                        sendText("На том конце согласились.");
                        date = bot.getDutyDate(user.getId());
                        sendText("Теперь ты дежуришь " + date.format(formatter));
                        bot.removeModal(destination);
                    } else if (message.equalsIgnoreCase("нет")) {
                        bot.sendText(destination, "Так ему и передам");
                        sendText("На том конце отказались");
                        bot.removeModal(destination);
                    } else {
                        bot.sendText(destination, "Введи что-нибудь нормальное");
                    }

                });

                sendText("Что-то еще?");
                System.out.println(bot.getCalendar(20));
                mode = Mode.WAITING;

            }

            case SENDALL -> {
                if (!messageText.equalsIgnoreCase("Отмена")) {
                    if (bot.sendAll(messageText)) {
                        sendText("Готово!");
                    } else {
                        sendText("Не сработало");
                    }
                }
                sendText("Что-то еще?");
                mode = Mode.WAITING;
            }

            case ENTERDELAY -> {
                try {
                    int delay = Integer.parseInt(messageText);
                    if (delay < 0) {
                        throw new NumberFormatException();
                    } else if (delay > 1440) {
                        sendText("Это слишком много. Максимум - 1440 минут (сутки)");
                    } else {
                        Database db = new Database();
                        if (db.setDelay(user.getId(), delay)) {
                            sendText("Готово");
                        } else {
                            sendText("Не сработало");
                        }
                        sendText("Что-то еще?");
                        mode = Mode.WAITING;
                    }
                } catch (NumberFormatException e) {
                    sendText("Введи что-нибудь другое");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            default -> {

                switch (messageText) {
                    case "Получить визуализацию очереди" -> sendText(bot.getVisualization());
                    case "/quit", "Закончить" -> {
                        sendText("Вот и поговорили");
                        bot.turnHandlerOff(this);
                        return;
                    }
                    default -> {

                        switch (userStatus) {

                            case CANDIDATE -> {
                                if ("Отправить запрос".equals(messageText)) {
                                    Long adminId = bot.getAdminId();
                                    bot.sendText(adminId, "К нам хочет " + user.getUserName() + ". Принять? (да/нет)");
                                    bot.addModal(adminId, (String message) -> {
                                        if (message.equalsIgnoreCase("Да")) {
                                            try {
                                                bot.addUser(user.getId());
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            }
                                            bot.sendText(adminId, "Добавил");
                                            sendText("Твою заявку приняли");
                                            bot.removeModal(adminId);
                                        } else if (message.equalsIgnoreCase("Нет")) {
                                            bot.sendText(adminId, "Понял");
                                            sendText("К сожалению, админ не хочет тебя пускать");
                                            bot.removeModal(adminId);
                                        } else {
                                            bot.sendText(adminId, "Введи что-нибудь нормальное");
                                        }
                                    });
                                    sendText("Ваш запрос отправлен");
                                } else {
                                    sendText("Такой команды нет");
                                    return;
                                }
                            }

                            case REGULAR -> {
                                switch (messageText) {
                                    case "Запросить выход из очереди" -> {
                                        Long adminId = bot.getAdminId();
                                        bot.sendText(adminId, user.getUserName() + " хочет выйти. Пусть идет? (да/нет)");
                                        bot.addModal(adminId, (String message) -> {
                                            if (message.equalsIgnoreCase("Да")) {
                                                try {
                                                    bot.removeUser(user.getId());
                                                } catch (SQLException e) {
                                                    e.printStackTrace();
                                                }
                                                bot.sendText(adminId, "Удалил его");
                                                sendText("Теперь Добби свободен");
                                                bot.removeModal(adminId);
                                            } else if (message.equalsIgnoreCase("Нет")) {
                                                bot.sendText(adminId, "Понял");
                                                sendText("К сожалению, админ не хочет тебя отпускать");
                                                bot.removeModal(adminId);
                                            } else {
                                                bot.sendText(adminId, "Введи что-нибудь нормальное");
                                            }
                                        });
                                        sendText("Ваш запрос отправлен");
                                    }

                                    case "Настроить оповещения" -> {
                                        sendText("Введи промежуток времени в минутах за который тебя нужно уведомлять: ");
                                        mode = Mode.ENTERDELAY;
                                        return;
                                    }

                                    case "Поменяться местами" -> {
                                        System.out.println(bot.getCalendar(20));
                                        if (swapPlaces()) {
                                            return;
                                        }
                                    }

                                    case "Узнать время дежурства" -> {
                                        LocalDate date = bot.getDutyDate(user.getId());
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
                                        sendText(date.format(formatter) + ", " + bot.translate(date.getDayOfWeek()));
                                    }
                                    default -> {
                                        sendText("Такой команды нет");
                                        return;
                                    }
                                }
                            }

                            case ADMIN -> {
                                switch (messageText) {
                                    case "Настроить оповещения" -> {
                                        sendText("Введи промежуток времени в минутах за который тебя нужно уведомлять: ");
                                        mode = Mode.ENTERDELAY;
                                        return;
                                    }
                                    case "Поменяться местами" -> {
                                        System.out.println(bot.getCalendar(20));
                                        if (swapPlaces()) {
                                            return;
                                        }
                                    }

                                    case "Узнать время дежурства" -> {
                                        LocalDate date = bot.getDutyDate(user.getId());
                                        sendText(String.format("%d.%d, %s", date.getDayOfMonth(), date.getMonthValue(), bot.translate(date.getDayOfWeek())));
                                    }

                                    case "Удалить пользователя" -> {
                                        sendText("Кого хочешь удалить?");
                                        sendUserList();

                                        mode = Mode.DELETEUSER;
                                        return;
                                    }

                                    case "Назначить ответственного" -> {
                                        sendText("Кого хочешь назначить?");
                                        sendUserList();

                                        mode = Mode.SETADMIN;
                                        return;
                                    }

                                    case "Разослать новость" -> {
                                        sendText("Введи текст сообщения: ");
                                        mode = Mode.SENDALL;
                                        return;
                                    }

                                    case "Создать событие" -> {
                                        sendText("Введи название события: ");
                                        mode = Mode.CREATEEVENT;
                                        return;
                                    }

                                    default -> {
                                        sendText("Такой команды нет");
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
                sendText("Что-то еще?");
            }
        }
    }



}
