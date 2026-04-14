package it.polimi.mae.musiclabeling.dao;

import it.polimi.mae.musiclabeling.beans.Song;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SongsDAOImpl implements SongsDAO{
    private Connection connection;

    public SongsDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void addSong(String songName, String artist, String filePath) throws SQLException {
        String sql = "INSERT INTO songs (song_name, artist, file_path) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, songName);
            statement.setString(2, artist);
            statement.setString(3, filePath);
            statement.executeUpdate();
        }
    }

    @Override
    public void addSongToUser(int userId, String songName, String artist, String filePath) throws SQLException {
        String insertSong = "INSERT INTO Songs (song_name, artist, file_path) VALUES (?, ?, ?)";
        String linkSongToUser = "INSERT INTO user_songs (user_id, song_id) VALUES (?, ?)";
        PreparedStatement stmtSong = null;
        PreparedStatement stmtLink = null;
        ResultSet generatedKeys = null;
        Integer songId = null;

        connection.setAutoCommit(false); // Start transaction

        // Insert the new song
        stmtSong = connection.prepareStatement(insertSong, Statement.RETURN_GENERATED_KEYS);
        stmtSong.setString(1, songName);
        stmtSong.setString(2, artist);
        stmtSong.setString(3, filePath);
        int affectedRows = stmtSong.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Creating song failed, no rows affected.");
        }

        generatedKeys = stmtSong.getGeneratedKeys();
        if (generatedKeys.next()) {
            songId = generatedKeys.getInt(1);
        } else {
            throw new SQLException("Creating song failed, no ID obtained.");
        }

        // Link the song to the user
        stmtLink = connection.prepareStatement(linkSongToUser);
        stmtLink.setInt(1, userId);
        stmtLink.setInt(2, songId);
        stmtLink.executeUpdate();

        connection.commit(); // Commit transaction
        connection.setAutoCommit(true);
    }

    @Override
    public Song getSong(int songId) throws SQLException {
        String sql = "SELECT * FROM songs WHERE song_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Song song = new Song();
                    song.setSongId(resultSet.getInt("song_id"));
                    song.setSongName(resultSet.getString("song_name"));
                    song.setArtist(resultSet.getString("artist"));
                    song.setFilePath(resultSet.getString("file_path"));
                    return song;
                }
            }
        }
        return null;
    }

    @Override
    public void deleteSong(int songId) throws SQLException {
        String del_from_user_song_labels = "DELETE FROM user_song_labels WHERE song_id = ?";
        String del_from_user_songs = "DELETE FROM user_songs WHERE song_id = ?";
        String del_from_songs = "DELETE FROM songs WHERE song_id = ?";

        try
        {
            PreparedStatement statement = connection.prepareStatement(del_from_user_song_labels);
            statement.setInt(1, songId);
            statement.executeUpdate();

            statement = connection.prepareStatement(del_from_user_songs);
            statement.setInt(1, songId);
            statement.executeUpdate();

            statement = connection.prepareStatement(del_from_songs);
            statement.setInt(1, songId);
            int rowsDeleted = statement.executeUpdate();

            if (rowsDeleted == 0) {
                throw new SQLException("Failed to delete the song with ID: " + songId);
            }
        }
        catch (SQLException e) {
            // Handle any SQL exceptions
            //console.log("Error deleting song: " + e.getMessage());
            throw e; // Re-throw the exception to be handled by the caller
        }
    }

    @Override
    public boolean checkUserAccessToSong(int userId, int songId) throws SQLException {
        String query = "SELECT EXISTS (" +
                "SELECT 1 FROM user_songs WHERE user_id = ? AND song_id = ?" +
                ") AS AccessGranted";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, songId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("AccessGranted");
            }
        }
        return false;
    }


    @Override
    public void updateSong(Song song) throws SQLException {
        String sql = "UPDATE songs SET song_name = ?, artist = ?, file_path = ? WHERE song_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, song.getSongName());
            statement.setString(2, song.getArtist());
            statement.setString(3, song.getFilePath());
            statement.setInt(4, song.getSongId());
            statement.executeUpdate();
        }
    }

    @Override
    public List<Song> getAllSongs(boolean filePathIncluded) throws SQLException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT * FROM songs ORDER BY song_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Song song = new Song();
                    song.setSongId(resultSet.getInt("song_id"));
                    song.setSongName(resultSet.getString("song_name"));
                    song.setArtist(resultSet.getString("artist"));
                    if (filePathIncluded)
                        song.setFilePath(resultSet.getString("file_path"));
                    else
                        song.setFilePath("");
                    songs.add(song);
                }
            }
        }
        return songs;
    }

    @Override
    public List<Song> getAllSongsOfUser(int userId, boolean filePathIncluded) throws SQLException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.* FROM songs s JOIN user_songs us ON s.song_id = us.song_id WHERE us.user_id = (?) ORDER BY song_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Song song = new Song();
                    song.setSongId(resultSet.getInt("song_id"));
                    song.setSongName(resultSet.getString("song_name"));
                    song.setArtist(resultSet.getString("artist"));
                    if (filePathIncluded)
                        song.setFilePath(resultSet.getString("file_path"));
                    else
                        song.setFilePath("");
                    songs.add(song);
                }
            }
        }
        return songs;
    }
}
