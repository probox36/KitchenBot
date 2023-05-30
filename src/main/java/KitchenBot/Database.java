package KitchenBot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

public class Database {

    private final String url = "jdbc:mysql://localhost:3306/bot_db";
    private final String adminUserName = "root";
    private final String password = "#Hoover_Dam";
    private final Connection connection = DriverManager.getConnection(url, adminUserName, password);
    private final Statement statement = connection.createStatement();

    public Database() throws SQLException {
    }

    public boolean addUser (long userId, String userName, Bot.UserStatus status) {
        try {
            statement.executeUpdate(String.format("""
                        INSERT Users(telegram_id, user_name, user_status)
                         	VALUES\s
                            (%d, '%s', (SELECT id FROM user_status WHERE status_name = '%s'));"""
                    , userId, userName, status.toString()));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean removeUser (long userId) {
        try {
            statement.executeUpdate("SET FOREIGN_KEY_CHECKS=0;");
            statement.executeUpdate(String.format("delete from Users where telegram_id = %d;", userId));
            statement.executeUpdate("SET FOREIGN_KEY_CHECKS=1;");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean updateStatus (long userId, Bot.UserStatus status) {
        try {
            statement.executeUpdate(String.format("""
                    update users set user_status =\s
                    (select id from user_status where status_name = '%s')
                    where telegram_id = %d;""", status.toString(), userId));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean updateQueue (LinkedList<Long> queue) {
        try {
            statement.executeUpdate("SET FOREIGN_KEY_CHECKS=0;");
            statement.executeUpdate("delete from queue;");
            statement.executeUpdate("SET FOREIGN_KEY_CHECKS=1;");
            for (int i = 0; i < queue.size(); i++) {
                statement.executeUpdate(String.format("""
                    INSERT Queue(position, user_id)
                     	VALUES\s
                        (%d, %d);""", i, queue.get(i)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean setDelay (long userId, int delay) {
        try {
            statement.executeUpdate(String.format("""
                    INSERT Delays (user_id, delay)\s
                    VALUES (%d, %d)\s
                    ON DUPLICATE KEY\s
                    UPDATE user_id = %d, delay = %d;""", userId, delay, userId, delay));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
