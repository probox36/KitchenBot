package OldBot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;

public class OldBot extends TelegramLongPollingBot {

    private enum Mode {
        IDLE, WORKING, ARGS_INPUT
    }
    private Mode mode = Mode.IDLE;
    private boolean isFirstMessage = true;
    private User user;
    private Message message;
    private static InlineKeyboardMarkup keyboard;
    private final Boolean lock = true;

    @Override
    public String getBotUsername() {
        return "uselessProboxBot";
    }

    @Override
    public String getBotToken() {
        return "6075519402:AAGjazWyfYAuiHgETsHo_Qa0JFe00g-q-QM";
    }

    private void refresh(String text) { // отправляет пустое сообщение, чтобы не ждать ввода от пользователя
        Message message = new Message();
        message.setText(text);
        Update update = new Update();
        update.setMessage(message);
        onUpdateReceived(update);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            synchronized (lock) {
                if (user != null) {
                    System.out.println("Мы вошли в onUpdate в режиме " + mode.toString() + ", UpdateQuery " + (update.hasCallbackQuery() ? "есть" : "нет"));
                }
                if (mode == Mode.IDLE) { // если бот находится в бездействии
                    message = update.getMessage();
                    System.out.println("Обновился message: " + message.getText());
                    if (message != null && message.getText().equals("/start")) {
                        if (isFirstMessage) { // приветствуем пользователя, если это первое сообщение
                            user = message.getFrom();
                            System.out.println("К нам пришел " + user.getUserName());
                            sendText(user.getId(), "Привет, " + user.getFirstName());
                            isFirstMessage = false;
                        }
                        mode = Mode.WORKING;
                        System.out.println("Теперь mode = WORKING");
                        refresh("");
                    }
                } else if (mode == Mode.WORKING) { // если бот взаимодействует с пользователем
                    if (update.hasCallbackQuery()) { // если была нажата кнопочка на клавиатуре
                        AnswerCallbackQuery answer = new AnswerCallbackQuery(update.getCallbackQuery().getId());
                        answer.setShowAlert(false);
                        execute(answer);
                        String query = update.getCallbackQuery().getData();
                        if (query.equals("exit")) {
                            mode = Mode.IDLE;
                            sendText(user.getId(), "Пиши /start если захочешь посчитать еще что-нибудь");
                            System.out.println("Переходим в IDLE и рефрешимся");
                            refresh("");
                        } else {
                            mode = Mode.ARGS_INPUT;
                            new Thread(new Calculator(query)).start();
                        }
                    } else { // иначе, выдает клавиатуру
                        keyboard = getKeyboard();
                        sendMenu(user.getId(), "Выбери вариант:", keyboard);
                    }
                } else if (mode == Mode.ARGS_INPUT) { // если бот принимает аргументы для калькулятора
                    if (update.hasCallbackQuery()) {
                        refresh("");
                    } else {
                        message = update.getMessage() == null ? new Message() : update.getMessage();
                        System.out.println("Обновился message: " + message.getText());
                        lock.notify();
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void sendMenu(Long who, String txt, InlineKeyboardMarkup kb){
        SendMessage sendMessage = SendMessage.builder().chatId(who.toString())
                .parseMode("HTML").text(txt)
                .replyMarkup(kb).build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public InlineKeyboardMarkup getKeyboard() { // возвращает клавиатуру
        InlineKeyboardButton one = InlineKeyboardButton.builder()
                .text("1").callbackData("btn1").build();

        InlineKeyboardButton two = InlineKeyboardButton.builder()
                .text("2").callbackData("btn2").build();

        InlineKeyboardButton three = InlineKeyboardButton.builder()
                .text("3").callbackData("btn3").build();

        InlineKeyboardButton four = InlineKeyboardButton.builder()
                .text("4").callbackData("btn4").build();

        InlineKeyboardButton five = InlineKeyboardButton.builder()
                .text("5").callbackData("btn5").build();

        InlineKeyboardButton six = InlineKeyboardButton.builder()
                .text("6").callbackData("btn6").build();

        InlineKeyboardButton seven = InlineKeyboardButton.builder()
                .text("7").callbackData("btn7").build();

        InlineKeyboardButton cancel = InlineKeyboardButton.builder()
                .text("Выход").callbackData("exit").build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(one, two, three))
                .keyboardRow(List.of(four, five, six))
                .keyboardRow(List.of(seven, cancel))
                .build();
    }

    private class Calculator implements Runnable { // считает выражения
        public Calculator(String btnID) { this.btnID = btnID; }
        public void setBtnID(String btnID) { this.btnID = btnID; }
        private String btnID;

        private void waitTillInput() { // освобождает монитор и ждет, пока не обновится сообщение
            try {
                lock.notify();
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public double parseMessage() { // пытается парсить сообщения пока пользователь не введет нормальное число
            double result;
            synchronized (lock) {
                while (true) {
                    String str = message.getText();
                    try {
                        result = Double.parseDouble(str);
                        break;
                    } catch (NumberFormatException ignored) {
                        sendText(user.getId(), "Попробуй ввести величину еще раз: ");
                        waitTillInput();
                    }
                }
            }
            return result;
        }

        @Override
        public synchronized void run() {
            if (btnID == null) { throw new RuntimeException("btnID can't be empty"); }
            System.out.println("Новый калькулятор запущен!");
            try {
                synchronized (lock) {
                    System.out.println("Вошел в синхронизированный блок и начал спать");
                    switch (btnID) {
                        case "btn1" -> {
                            sendText(user.getId(), "A:");
                            waitTillInput();
                            double a = parseMessage();
                            sendText(user.getId(), "B:");
                            waitTillInput();
                            double b = parseMessage();
                            sendText(user.getId(), "C:");
                            waitTillInput();
                            double c = parseMessage();
                            sendText(user.getId(), "N:");
                            waitTillInput();
                            double n = parseMessage();
                            sendText(user.getId(), "X:");
                            waitTillInput();
                            double x = parseMessage();
                            sendText(user.getId(), "Выражение = " + ((5 * Math.pow(a, n * x)) / (b + c) - Math.sqrt(Math.abs(Math.cos(x * x * x)))));
                        }
                        case "btn2" -> {
                            sendText(user.getId(), "A:");
                            waitTillInput();
                            double a = parseMessage();
                            sendText(user.getId(), "W:");
                            waitTillInput();
                            double w = parseMessage();
                            sendText(user.getId(), "X:");
                            waitTillInput();
                            double x = parseMessage();
                            sendText(user.getId(), "Y:");
                            waitTillInput();
                            double y = parseMessage();
                            sendText(user.getId(), "Выражение = " + (Math.abs(x - y) / Math.pow(1 + 2 * x, a) - Math.pow(Math.E, Math.sqrt(1 + w))));
                        }
                        case "btn3" -> {
                            sendText(user.getId(), "A0:");
                            waitTillInput();
                            double a0 = parseMessage();
                            sendText(user.getId(), "A1:");
                            waitTillInput();
                            double a1 = parseMessage();
                            sendText(user.getId(), "A2:");
                            waitTillInput();
                            double a2 = parseMessage();
                            sendText(user.getId(), "X:");
                            waitTillInput();
                            double x = parseMessage();
                            sendText(user.getId(), "Выражение = " + (Math.sqrt(a0 + a1*x + a2*Math.pow(Math.abs(Math.sin(x)), 1/3f))));
                        }
                        case "btn4" -> {
                            sendText(user.getId(), "A:");
                            double a = parseMessage();
                            waitTillInput();
                            sendText(user.getId(), "X:");
                            double x = parseMessage();
                            waitTillInput();
                            sendText(user.getId(), "Выражение = " + (Math.log(Math.abs(Math.pow(a, 7)))+Math.atan(x*x)+Math.PI/Math.sqrt(Math.abs(a+x))));
                        }
                        case "btn5" -> {
                            sendText(user.getId(), "A:");
                            double a = parseMessage();
                            waitTillInput();
                            sendText(user.getId(), "B:");
                            double b = parseMessage();
                            waitTillInput();
                            sendText(user.getId(), "C:");
                            double c = parseMessage();
                            waitTillInput();
                            sendText(user.getId(), "D:");
                            double d = parseMessage();
                            waitTillInput();
                            sendText(user.getId(), "X:");
                            double x = parseMessage();
                            sendText(user.getId(), "Выражение = " + (Math.pow(Math.pow(a+b, 2)/(c+d) + Math.pow(Math.E, Math.sqrt(x+1)), 1/5f)));
                        }
                        case "btn6" -> {
                            sendText(user.getId(), "X:");
                            waitTillInput();
                            double x = parseMessage();
                            sendText(user.getId(), "Выражение = " + (Math.pow(Math.E, (2*Math.sin(4*x) + Math.pow(Math.cos(x*x), 2))/(3*x))));
                        }
                        case "btn7" -> {
                            sendText(user.getId(), "X:");
                            waitTillInput();
                            double x = parseMessage();
                            sendText(user.getId(), "Выражение = " + (0.25*((1+x*x)/(1-x) + 0.5*Math.tan(x))));
                        }
                        default -> sendText(user.getId(), "Отменяю текущее действие");
                    }
                    mode = Mode.WORKING;
                    System.out.println("Текущий режим - IDLE");
                    System.out.println("Передаю управление треду main");
                    lock.notify();
                    refresh("");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new OldBot());
    }

}