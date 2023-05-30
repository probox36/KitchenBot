//package OldBot;
//
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.api.objects.User;
//
//public class EchoUserHandler {
//
//    private final EchoBot bot;
//    private final User user;
//    private final Bot.UserStatus userStatus;
//    private Update update;
//    private boolean greeted = false;
//
//    public User getUser() { return user; }
//
//    public EchoUserHandler(EchoBot bot, User user, Bot.UserStatus userStatus) {
//        this.bot = bot;
//        this.user = user;
//        this.userStatus = userStatus;
//    }
//
//    public void sendText(String what) {
//        bot.sendText(user.getId(), what);
//    }
//    public void pass(Update update) {
//
//        this.update = update;
//        if (!greeted) {
//            sendText("Привет, " + user.getUserName());
//            sendText("Я буду зеркалить твои сообщения, когда надоест - пиши /quit");
//            greeted = true;
//            return;
//        }
//        String message = update.getMessage().getText();
//        if (message.equalsIgnoreCase("/quit")) {
//            sendText("Вот и поговорили");
//            bot.turnHandlerOff(this);
//            return;
//        }
//        sendText(message);
//    }
//
//}
