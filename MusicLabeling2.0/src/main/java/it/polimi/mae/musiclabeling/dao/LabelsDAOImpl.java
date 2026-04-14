package it.polimi.mae.musiclabeling.dao;

import it.polimi.mae.musiclabeling.beans.Label;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class LabelsDAOImpl implements LabelsDAO {
    private Connection connection;

    public LabelsDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void addLabel(Label label) throws SQLException {
        String sql = "INSERT INTO labels (label_name) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, label.getLabelName());
            statement.executeUpdate();
        }

    }

    @Override
    public Label getLabel(int labelId) throws SQLException {
        String sql = "SELECT * FROM labels WHERE label_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, labelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Label label = new Label();
                    label.setLabelId(resultSet.getInt("label_id"));
                    label.setLabelName(resultSet.getString("label_name"));
                    return label;
                }
            }
        }
        return null;
    }

    @Override
    public void updateLabel(Label label) throws SQLException {
        String sql = "UPDATE labels SET label_name = ? WHERE label_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, label.getLabelName());
            statement.setInt(2, label.getLabelId());
            statement.executeUpdate();
        }
    }

    @Override
    public List<Label> getAllLabels() throws SQLException {
        String sql = "SELECT * FROM labels";
        List<Label> labels = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Label label = new Label();
                    label.setLabelId(result.getInt("label_id"));
                    label.setLabelName(result.getString("label_name"));
                    label.setLabelTiming(0);
                    labels.add(label);
                }
            }
        }
        return labels;
    }

    @Override
    public List<Label> getLabelsFromUserAndSong(int userId, int songId) throws SQLException {
        String sql = "SELECT * FROM labels l JOIN user_song_labels usl ON l.label_id = usl.label_id WHERE usl.user_id = ? AND usl.song_id = ?";
        List<Label> labels = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, songId);
            try (ResultSet result = statement.executeQuery();) {
                while(result.next()) {
                    Label label = new Label();
                    label.setLabelId(result.getInt("label_id"));
                    label.setLabelName(result.getString("label_name"));
                    label.setLabelTiming(result.getInt("timing"));
                    label.setUserSongLabelId(result.getInt("user_song_label_id"));
                    labels.add(label);
                }
            }
        }
        return labels;
    }

    @Override
    public List<Label> getLabelsForSong(int songId) throws SQLException {
        String sql = "SELECT * FROM labels l JOIN user_song_labels usl ON l.label_id = usl.label_id WHERE usl.song_id = ?";
        List<Label> labels = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            try (ResultSet result = statement.executeQuery();) {
                while(result.next()) {
                    Label label = new Label();
                    label.setLabelId(result.getInt("label_id"));
                    label.setLabelName(result.getString("label_name"));
                    label.setLabelTiming(result.getInt("timing"));
                    label.setUserSongLabelId(result.getInt("user_song_label_id"));
                    labels.add(label);
                }
            }
        }
        return labels;
    }

    @Override
    public JsonObject exportLabelsToFile() throws SQLException {
        String sqlQuery = "SELECT u.username, s.song_id, s.song_name, s.file_path, l.label_name, usl.timing " +
                "FROM user_song_labels usl " +
                "JOIN songs s ON usl.song_id = s.song_id " +
                "JOIN users u ON usl.user_id = u.user_id " +
                "JOIN labels l ON usl.label_id = l.label_id " +
                "ORDER BY u.username, s.song_id, usl.timing";

        JsonObject root = null;

        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            ResultSet resultSet = statement.executeQuery();

            root = new JsonObject();
            JsonArray usersArray = new JsonArray();
            root.add("users", usersArray);
            JsonObject currentUserObj = null;

            JsonArray songsArray = null;
            JsonObject currentSongObj = null;

            JsonArray labelsArray = null;

            String currentUser = "";
            String currentSongId = "";

            // Iterate over the result set to create the JSON structure
            while (resultSet.next()) {
                // Get the user, song name, path, and label with timing
                String userName = resultSet.getString("username");
                String songName = resultSet.getString("song_name");
                String songId = resultSet.getString("song_id");
                String filePath = resultSet.getString("file_path");
                String labelName = resultSet.getString("label_name");
                int timing = resultSet.getInt("timing");

                if (!userName.equals(currentUser)) {
                    // New user
                    currentUser = userName;
                    currentUserObj = new JsonObject();
                    currentUserObj.addProperty("userName", userName);

                    songsArray = new JsonArray();
                    currentUserObj.add("songs", songsArray);

                    usersArray.add(currentUserObj);
                }

                if (!songId.equals(currentSongId)) {
                    // New song
                    currentSongId = songId;
                    currentSongObj = new JsonObject();
                    currentSongObj.addProperty("id", songId);
                    currentSongObj.addProperty("name", songName);
                    currentSongObj.addProperty("path", filePath);

                    labelsArray = new JsonArray();
                    currentSongObj.add("labels", labelsArray);

                    songsArray.add(currentSongObj);
                }

                JsonObject labelObj = new JsonObject();
                labelObj.addProperty("label", labelName);
                labelObj.addProperty("timing", timing);
                labelsArray.add(labelObj);
            }
        }
        return root;
    }

    @Override
    public void addLabelToSong(int userId, int songId, int labelId, int timing) throws SQLException {
        String sql = "INSERT INTO user_song_labels (user_id, song_id, label_id, timing) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, songId);
            statement.setInt(3, labelId);
            statement.setFloat(4, timing);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteSongLabel(int userSongLabelId) throws SQLException {
        String sql = "DELETE user_song_labels FROM user_song_labels JOIN (SELECT timing FROM user_song_labels WHERE user_song_label_id = (?)) AS temp ON user_song_labels.timing >= temp.timing";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userSongLabelId);
            statement.executeUpdate();
        }
    }

    @Override
    public boolean userCanDeleteSong(int userSongLabelId, int userId) throws SQLException {
        String sql = "SELECT * FROM user_song_labels WHERE user_song_label_id = (?) AND user_id = (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userSongLabelId);
            statement.setInt(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void addLabelsToSong(int userId, int songId, List<Integer> labelIds, List<Integer> timings) throws SQLException {
        connection.setAutoCommit(false);
        for(int i = 0; i < labelIds.size(); i++) {
            addLabelToSong(userId, songId, labelIds.get(i), timings.get(i));
        }
        connection.commit();
        connection.setAutoCommit(true);
    }

    @Override
    public boolean labelCanBeAdded(int userId, int songId, int timing) throws SQLException {
        String sql = "SELECT * FROM user_song_labels WHERE user_id=(?) AND song_id=(?) AND timing >= (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, songId);
            statement.setInt(3, timing);
            try (ResultSet result = statement.executeQuery()) {
                if(result.next()) {
                    return false;
                }
            }
        }
        return true;
    }
}
