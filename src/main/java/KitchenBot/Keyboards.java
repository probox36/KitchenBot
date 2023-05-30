package KitchenBot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Arrays;
import java.util.List;

public class Keyboards {
    public static ReplyKeyboardMarkup getMenu(Bot.UserStatus status) {

        switch (status) {
            case REGULAR -> {
                KeyboardRow row1 = new KeyboardRow();
                row1.addAll(List.of("Получить визуализацию очереди"));

                KeyboardRow row2 = new KeyboardRow();
                row2.addAll(Arrays.asList("Запросить выход из очереди", "Настроить оповещения"));

                KeyboardRow row3 = new KeyboardRow();
                row3.addAll(Arrays.asList("Поменяться местами", "Узнать время дежурства"));

                KeyboardRow row4 = new KeyboardRow();
                row4.addAll(List.of("Закончить"));

                return ReplyKeyboardMarkup.builder()
                        .keyboardRow(row1)
                        .keyboardRow(row2)
                        .keyboardRow(row3)
                        .keyboardRow(row4)
                        .resizeKeyboard(true)
//                        .oneTimeKeyboard(true)
                        .build();
            }
            case ADMIN -> {
                KeyboardRow row1 = new KeyboardRow();
                row1.addAll(Arrays.asList("Получить визуализацию очереди", "Создать событие"));

                KeyboardRow row2 = new KeyboardRow();
                row2.addAll(Arrays.asList("Настроить оповещения", "Поменяться местами"));

                KeyboardRow row3 = new KeyboardRow();
                row3.addAll(Arrays.asList("Узнать время дежурства", "Удалить пользователя"));
//                row3.addAll(Arrays.asList("Узнать время дежурства", "Назначить ответственного"));

                KeyboardRow row4 = new KeyboardRow();
                row4.addAll(Arrays.asList("Назначить ответственного", "Разослать новость"));
//                row4.addAll(List.of("Разослать новость"));

                KeyboardRow row5 = new KeyboardRow();
                row5.addAll(List.of("Закончить"));

                return ReplyKeyboardMarkup.builder()
                        .keyboardRow(row1)
                        .keyboardRow(row2)
                        .keyboardRow(row3)
                        .keyboardRow(row4)
                        .keyboardRow(row5)
                        .resizeKeyboard(true)
//                        .oneTimeKeyboard(true)
                        .build();
            }
            default -> {
                KeyboardRow row1 = new KeyboardRow();
                row1.addAll(List.of("Получить визуализацию очереди"));

                KeyboardRow row2 = new KeyboardRow();
                row2.addAll(List.of("Отправить запрос"));

                KeyboardRow row3 = new KeyboardRow();
                row3.addAll(List.of("Закончить"));

                return ReplyKeyboardMarkup.builder()
                        .keyboardRow(row1)
                        .keyboardRow(row2)
                        .keyboardRow(row3)
                        .resizeKeyboard(true)
//                        .oneTimeKeyboard(true)
                        .build();
            }
        }
    }
}
