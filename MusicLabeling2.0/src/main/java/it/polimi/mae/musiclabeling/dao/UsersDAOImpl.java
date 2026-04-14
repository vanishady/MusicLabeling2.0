package it.polimi.mae.musiclabeling.dao;

import it.polimi.mae.musiclabeling.beans.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsersDAOImpl implements UsersDAO {
    private Connection connection;

    public UsersDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            statement.executeUpdate();
        }
    }

    @Override
    public User getUser(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User();
                    user.setUserId(resultSet.getInt("user_id"));
                    user.setUsername(resultSet.getString("username"));
                    user.setPassword(resultSet.getString("password"));
                    user.setAdmin(resultSet.getBoolean("is_admin"));
                    return user;
                }
            }
        }
        return null;
    }

    @Override
    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, password = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setInt(3, user.getUserId());
            statement.executeUpdate();
        }
    }

    @Override
    public User checkCredentials(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, password, is_admin FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next())
                    return null;
                String storedHash = result.getString("password");
                if (!BCrypt.checkpw(password, storedHash))
                    return null;
                User user = new User();
                user.setUserId(result.getInt("user_id"));
                user.setUsername(result.getString("username"));
                user.setAdmin(result.getBoolean("is_admin"));
                return user;
            }
        }
    }

    @Override
    public List<User> getUsers(int userId) throws SQLException {
        User adminUser = getUser(userId);
        if(!adminUser.isAdmin()) {
            throw new SQLException("User is not admin");
        }
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username FROM users";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    User user = new User();
                    user.setUserId(result.getInt("user_id"));
                    user.setUsername(result.getString("username"));
                    user.setPassword("");
                    user.setAdmin(false);
                    users.add(user);
                }
            }
        }
        return users;
    }
}
