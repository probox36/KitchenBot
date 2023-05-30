package KitchenBot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class Bot extends TelegramLongPollingBot {

    public enum UserStatus {
        REGULAR, ADMIN, CANDIDATE
    }

    private boolean initialized = false;
    private HashMap<Long, String> userNames;
    private HashMap<Long, UserStatus> userStatuses;
    private LinkedList<Long> queue;
    private HashMap<Long, UserHandler> userHandlers;
    private HashMap<Long, Integer> delays;
    private final LocalTime dutyTime = LocalTime.of(10, 0);
    private final int defaultDelay = 240;
    private DutyDay today;
    private HashMap<LocalDate, DutyDay> exchange;
    private HashMap<Long, Passable> modals;

    public LinkedList<Long> getQueue() { return queue; }
    public HashMap<Long, String> getUserNames() { return userNames; }
    public HashMap<Long, UserStatus> getUserStatuses() { return userStatuses; }
    public void addModal(Long userId, Passable passable) { modals.put(userId, passable); }
    public void removeModal(Long userId) { modals.remove(userId); }
    public HashMap<Long, Integer> getDelays() { return delays; }
    public LocalTime getDutyTime() { return dutyTime; }
    public int getDefaultDelay() { return defaultDelay; }
    public DutyDay getToday() { return today; }

    public void swapUsers(LocalDate day1, LocalDate day2) {
        LocalDate now = LocalDate.now();
        if (day1.isAfter(now.plusDays(30)) || day2.isAfter(now.plusDays(30))) {
            throw new RuntimeException("Наш горизонт планирования равен месяцу, не более");
        }
        HashMap<LocalDate, DutyDay> calendar = getCalendar(30);
        RegularDutyDay user1 = (RegularDutyDay) calendar.get(day1);
        RegularDutyDay user2 = (RegularDutyDay) calendar.get(day2);
        exchange.put(day1, user2);
        exchange.put(day2, user1);
    }

    public boolean sendAll(String messageText) {
        try {
            for (long userId: userStatuses.keySet()) {
                if (userStatuses.get(userId) == UserStatus.REGULAR) {
                    sendText(userId, messageText);
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void nextDay() {
        LocalDate now = LocalDate.now();
        if (exchange.containsKey(now)) {
            DutyDay day = exchange.get(now);
            if (day.isRegular()) { // если кто-то поменялся местами
                today = day;
                rewind(queue);
            } else { // если запланировано событие
                if (((Event) day).isNotificationNeeded()) {
                    String message = "Сегодня " + ((Event) day).getReason().toLowerCase();
                    if (((Event) day).getMessage() != null) {
                        message += "\n" + ((Event) day).getMessage();
                    }
                    sendAll(message);
                }
                today = day;
            }
        } else {
            rewind(queue);
            today = new RegularDutyDay(queue.peek());
        }

        HashMap<Long, UserStatus> userStatusesClone = userStatuses;
        HashMap<Long, String> userNamesClone = userNames;
        for (Long userId: userStatuses.keySet()) { // убирает лишних "кандидатов"
            if (userStatuses.get(userId) == UserStatus.CANDIDATE) {
                userNamesClone.remove(userId);
                userStatusesClone.remove(userId);
            }
        }
        userStatuses = userStatusesClone;
        userNames = userNamesClone;
    }

//    public static HashMap<LocalDate, DutyDay> addEvent(LocalDate when, Event what, HashMap<LocalDate, DutyDay> exchange) {
    public void addEvent(LocalDate when, Event what) {
        try {
            HashMap<LocalDate, DutyDay> exchangeClone = new HashMap<>();
            for (LocalDate date: exchange.keySet()) {
                if (date.isAfter(when) || date.equals(when)) {
                    exchangeClone.put(date.plusDays(1), exchange.get(date));
                } else {
                    exchangeClone.put(date, exchange.get(date));
                }
            }
            exchangeClone.put(when, what);
            exchange = exchangeClone;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(exchange);
    }

//    public static HashMap<LocalDate, DutyDay> getCalendar(int days, LinkedList<Long> queue, HashMap<LocalDate, DutyDay> exchange) {
    public HashMap<LocalDate, DutyDay> getCalendar(int days) {
        LocalDate date = LocalDate.now();
        LinkedList<Long> queueClone = queue;

        HashMap<LocalDate, DutyDay> calendar = new HashMap<>();
        for (int day = 1; day <= days; day++) {
            if (exchange.containsKey(date)) {
                if (exchange.get(date).isRegular()) {
                    calendar.put(date, exchange.get(date));
                    rewind(queueClone);
                } else {
                    calendar.put(date, exchange.get(date));
                }
            } else {
                calendar.put(date, new RegularDutyDay(queueClone.peek()));
                rewind(queueClone);
            }
            date = date.plusDays(1);
        }
        return calendar;
    }

    public LocalDate getDutyDate(Long desiredUserId) {
        HashMap<LocalDate, DutyDay> calendar = getCalendar(30);
        ArrayList<LocalDate> dates = new ArrayList<>(calendar.keySet());
        Collections.sort(dates);
        for (LocalDate date: dates) {
            if (calendar.get(date).isRegular()) {
                RegularDutyDay day = (RegularDutyDay) calendar.get(date);
                if (day.getUserOnDuty() == desiredUserId) {
//                    return String.format("%d.%d, %s",
//                            date.getDayOfMonth(), date.getMonthValue(), translate(date.getDayOfWeek()));
                    return date;
                }
            }
        }
        throw new RuntimeException("Не нашел тебя в очереди на месяц");
    }

    public String getVisualization() {
        HashMap<LocalDate, DutyDay> calendar = getCalendar(userNames.size()+2);
        System.out.println(calendar);
        ArrayList<LocalDate> dates = new ArrayList<>(calendar.keySet());
        Collections.sort(dates);
        StringBuilder queueVis = new StringBuilder();

        for (LocalDate date: dates) {
            if (calendar.get(date).isRegular()) {
                RegularDutyDay day = (RegularDutyDay) calendar.get(date);
                queueVis.append(date.getDayOfMonth())
                        .append(", ")
                        .append(translate(date.getDayOfWeek()))
                        .append(": дежурит ")
                        .append(userNames.get(day.getUserOnDuty()))
                        .append("\n");
            } else {
                Event event = (Event) calendar.get(date);
                queueVis.append(date.getDayOfMonth())
                        .append(", ")
                        .append(translate(date.getDayOfWeek()))
                        .append(": ")
                        .append(event.getReason())
                        .append("\n");
            }
        }
        return queueVis.toString();
    }

    public Long getAdminId() {
        for (Long userId: userStatuses.keySet()) {
            if (userStatuses.get(userId) == UserStatus.ADMIN) {
                return userId;
            }
        }
        throw new RuntimeException("У нас нет админа...");
    }

    public static <T> void rewind(LinkedList<T> queue) {
        queue.add(queue.poll());
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!initialized) { init(); }

        User user = update.getMessage().getFrom();
        System.out.println("Нам написал " + user.getId() + ": " + update.getMessage().getText());
        UserHandler handler;

        try {
            if (modals.containsKey(user.getId())) {
                modals.get(user.getId()).pass(update.getMessage().getText());
                return;
            }

            if (userHandlers.containsKey(user.getId())) { // если такой пользователь уже обслуживается
                handler = userHandlers.get(user.getId());
                System.out.println("У нас есть такой user! Отправляем сообщение в его handler");
                handler.pass(update);
            } else if (update.getMessage().getText().equalsIgnoreCase("Начать") ||
                    update.getMessage().getText().equalsIgnoreCase("/start")) {
                if (!userStatuses.containsKey(user.getId())) { // если пользователь пришел в 1-й раз
                    System.out.println("У нас такого user'а нет");

                    Database db = new Database();
                    userStatuses.put(user.getId(), UserStatus.CANDIDATE);
                    userNames.put(user.getId(), user.getUserName());
                    db.addUser(user.getId(), user.getUserName(), UserStatus.CANDIDATE);
                }
                System.out.println("Выдаем ему handler"); // если пользователь есть в списке, но только пришел
                handler = new UserHandler(this, user, userStatuses.get(user.getId()));
                userHandlers.put(user.getId(), handler);
                handler.pass(update);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void init() {
        try {
            String adminUserName = "root";
            String password = "#Hoover_Dam";
            String url = "jdbc:mysql://localhost:3306/bot_db";
            Connection connection = DriverManager.getConnection(url, adminUserName, password);
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("""
                    select Users.telegram_id as user_id,\s
                    Users.user_name as username,\s
                    User_status.status_name as status from Users
                    left join User_status on Users.user_status = User_status.id;""");

            userStatuses = new HashMap<>();
            userNames = new HashMap<>();
            queue = new LinkedList<>();
            delays = new HashMap<>();
            userHandlers = new HashMap<>();
            modals = new HashMap<>();

            while (result.next()) {
                Long id = (long) result.getInt(1);
                String userName = result.getString(2);
                UserStatus status = switch (result.getString(3)) {
                    case "ADMIN" -> UserStatus.ADMIN;
                    case "REGULAR" -> UserStatus.REGULAR;
                    default -> UserStatus.CANDIDATE;
                };
                userNames.put(id, userName);
                userStatuses.put(id, status);
            }

            result = statement.executeQuery("select user_id from Queue order by position;");
            while (result.next()) {
                queue.add((long) result.getInt(1));
            }

            result = statement.executeQuery("select * from delays;");
            while (result.next()) {
                Long id = (long) result.getInt(1);
                int delay = result.getInt(2);
                delays.put(id, delay);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(userStatuses);
        System.out.println(userNames);
        System.out.println(queue);
        System.out.println(delays);

        exchange = new HashMap<>();
        exchange.put(LocalDate.of(2023, 6, 3), new Event("Уборка"));
        exchange.put(LocalDate.of(2023, 6, 2), new Event("Выходной"));

        new Thread(new Clock(this)).start();
        initialized = true;
    }

    public void turnHandlerOff(UserHandler handler) {
        if (userHandlers.containsValue(handler)) {
            userHandlers.remove(handler.getUser().getId());
        } else {
            throw new RuntimeException("Такого handler'a нет");
        }
    }

    @Override
    public String getBotUsername() {
        return "uselessProboxBot";
    }

    @Override
    public String getBotToken() {
        return "6075519402:AAGjazWyfYAuiHgETsHo_Qa0JFe00g-q-QM";
    }

    public void sendText(Long who, String what){
        SendMessage sendMessage = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMenu(Long who, String text, ReplyKeyboardMarkup keyboard){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(who.toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyMarkup(keyboard);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String translate(DayOfWeek day) {
        return switch (day) {
            case SUNDAY -> "Воскресенье";
            case MONDAY -> "Понедельник";
            case TUESDAY -> "Вторник";
            case WEDNESDAY -> "Среда";
            case THURSDAY -> "Четверг";
            case FRIDAY -> "Пятница";
            case SATURDAY -> "Суббота";
        };
    }

    public void grantAdmin(Long oldAdmin, Long newAdmin) throws SQLException {
        Database db = new Database();
        userStatuses.put(newAdmin, UserStatus.ADMIN);
        db.updateStatus(newAdmin, UserStatus.ADMIN);
        userStatuses.put(oldAdmin, UserStatus.REGULAR);
        db.updateStatus(oldAdmin, UserStatus.REGULAR);
        System.out.println("У нас новый админ: " + userNames.get(newAdmin));
    }

    public void addUser(long userId) throws SQLException {
        Database db = new Database();
        queue.add(userId);
        db.updateQueue(queue);
        userStatuses.put(userId, UserStatus.REGULAR);
        db.updateStatus(userId, UserStatus.REGULAR);
        System.out.println("У нас новый пользователь: " + userNames.get(userId));
    }

    public void removeUser(Long userId) throws SQLException {
        System.out.println(queue);
        Database db = new Database();
        userStatuses.remove(userId);
        System.out.println("Убрал пользователя: " + userNames.get(userId));
        userNames.remove(userId);
        db.removeUser(userId);
        queue.removeFirstOccurrence(userId);

        db.updateQueue(queue);
        System.out.println("-->");
        System.out.println(queue);
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new Bot());

//        LinkedList<Long> q = new LinkedList<>();
//        q.add(1L);
//        q.add(12L);
//        q.add(123L);
//        q.add(1234L);
//        q.add(12345L);
//        q.add(123456L);
//        HashMap<LocalDate, DutyDay> ex = new HashMap<>();
//        ex.put(LocalDate.of(2023, 5, 29), new RegularDutyDay(1234L));
//        ex.put(LocalDate.of(2023, 5, 31), new RegularDutyDay(12L));
//
//        System.out.println(getCalendar(20, q, ex));
    }

}