//package OldBot;
//
//import org.telegram.telegrambots.bots.TelegramLongPollingBot;
//import org.telegram.telegrambots.meta.TelegramBotsApi;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.api.objects.User;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
//import java.util.HashMap;
//
//public class EchoBot extends TelegramLongPollingBot {
//
//
//    public enum UserStatus {
//        REGULAR, ADMIN
//    }
//
//    private boolean initialized = false;
//    private HashMap<Long, Bot.UserStatus> userList;
//    private HashMap<Long, EchoUserHandler> userHandlers;
//
//    public void init() {
//        initialized = true;
//        userList = new HashMap<>();
//        userHandlers = new HashMap<>();
//    }
//
//    public void onUpdateReceived(Update update) {
//
//        if (!initialized) { init(); }
//
//        System.out.println("Нам написал " + update.getMessage().getFrom().getUserName() + ": " + update.getMessage().getText());
//        User user = update.getMessage().getFrom();
//        EchoUserHandler handler;
//
//        try {
//            if (userHandlers.containsKey(user.getId())) {
//                handler = userHandlers.get(user.getId());
//                System.out.println("У нас есть такой user! Отправляем сообщение в его handler");
//                handler.pass(update);
//            } else if (update.getMessage().getText().equalsIgnoreCase("/start")) {
//                if (!userList.containsKey(user.getId())) {
//                    userList.put(user.getId(), Bot.UserStatus.REGULAR);
//                    System.out.println("У нас такого user'а нет");
//                }
//                System.out.println("Выдаем ему handler");
//                handler = new EchoUserHandler(this, user, userList.get(user.getId()));
//                userHandlers.put(user.getId(), handler);
//                handler.pass(update);
//            }
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//    }
//
//    public void turnHandlerOff(EchoUserHandler handler) {
//        if (userHandlers.containsValue(handler)) {
//            userHandlers.remove(handler.getUser().getId());
//        } else {
//            throw new RuntimeException("Такого handler'a нет");
//        }
//    }
//
//
//    public String getBotUsername() {
//        return "uselessProboxBot";
//    }
//
//
//    public String getBotToken() {
//        return "6075519402:AAGjazWyfYAuiHgETsHo_Qa0JFe00g-q-QM";
//    }
//
//    public void sendText(Long who, String what){
//        SendMessage sendMessage = SendMessage.builder()
//                .chatId(who.toString())
//                .text(what).build();
//        try {
//            execute(sendMessage);
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static void main(String[] args) throws TelegramApiException {
//        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
//        botsApi.registerBot(new EchoBot());
//    }
//
//}
