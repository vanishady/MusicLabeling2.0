package it.polimi.mae.musiclabeling.dao;
import it.polimi.mae.musiclabeling.beans.User;
import java.sql.SQLException;
import java.util.List;

public interface UsersDAO {
    void addUser(User user) throws SQLException;
    User getUser(int userId) throws SQLException;
    void updateUser(User user) throws SQLException;
    User checkCredentials(String username, String password) throws SQLException;
    List<User> getUsers(int userId) throws SQLException;
}