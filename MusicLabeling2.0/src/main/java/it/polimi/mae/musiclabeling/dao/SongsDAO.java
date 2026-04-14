package it.polimi.mae.musiclabeling.dao;
import it.polimi.mae.musiclabeling.beans.Song;
import java.sql.SQLException;
import java.util.List;

public interface SongsDAO {
    void addSong(String songName, String artist, String filePath) throws SQLException;
    void addSongToUser(int userId, String songName, String artist, String filePath) throws SQLException;
    Song getSong(int songId) throws SQLException;
    boolean checkUserAccessToSong(int userId, int songId) throws SQLException;
    void updateSong(Song song) throws SQLException;
    List<Song> getAllSongs(boolean filePathIncluded) throws SQLException;
    List<Song> getAllSongsOfUser(int userId, boolean filePathIncluded) throws SQLException;
    void deleteSong(int songId) throws SQLException;
}
