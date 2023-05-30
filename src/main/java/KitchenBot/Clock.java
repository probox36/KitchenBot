package KitchenBot;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;

public class Clock implements Runnable {

    private final Bot bot;
    public Clock(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {

        int savedDayOfMonth = LocalDateTime.now().getDayOfMonth();
        System.out.println("Часы запущены");
        while (true) {
            try {

                // меняет сутки
                LocalDateTime now = LocalDateTime.now();
                if (now.getDayOfMonth() != savedDayOfMonth) {
                    bot.nextDay();
                    System.out.println("Date changed");
                }

                HashMap<Long, Integer> delays = bot.getDelays();
                LocalTime dutyTime = bot.getDutyTime();

                // предупреждает тех, у кого настроены уведомления
                for(long userId: delays.keySet()) {
                    LocalTime desired = dutyTime.minusMinutes(delays.get(userId));
                    if (desired.getHour() == now.getHour() && desired.getMinute() == now.getMinute()) {
                        bot.sendText(userId, "Ты сегодня дежуришь. До проверки " + delays.get(userId) + " минут");
                    }
                }

                // предупреждает дежурного
                LocalTime desired = dutyTime.minusMinutes(bot.getDefaultDelay());
                if (desired.getHour() == now.getHour() && desired.getMinute() == now.getMinute()
                        && bot.getToday().isRegular()) {
                    bot.sendText(((RegularDutyDay) bot.getToday()).getUserOnDuty(), "Ты сегодня дежуришь. До проверки "
                            + bot.getDefaultDelay() + " минут");
                }

                Thread.sleep(30000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
