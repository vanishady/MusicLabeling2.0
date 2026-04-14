package it.polimi.mae.musiclabeling.dao;

import it.polimi.mae.musiclabeling.beans.Label;

import java.sql.SQLException;
import java.util.List;
import com.google.gson.JsonObject;

public interface LabelsDAO {
    void addLabel(Label label) throws SQLException;
    Label getLabel(int labelId) throws SQLException;
    void updateLabel(Label label) throws SQLException;
    List<Label> getAllLabels() throws SQLException;
    List<Label> getLabelsFromUserAndSong(int userId, int songId) throws SQLException;
    List<Label> getLabelsForSong(int songId) throws SQLException;
    void addLabelToSong(int userId, int songId, int labelId, int timing) throws SQLException;
    void deleteSongLabel(int userSongLabelId) throws SQLException;
    boolean userCanDeleteSong(int userSongLabelId, int userId) throws SQLException;
    void addLabelsToSong(int userId, int songId, List<Integer> labelIds, List<Integer> timings) throws SQLException;
    boolean labelCanBeAdded(int userId, int songId, int labelTiming) throws SQLException;
    JsonObject exportLabelsToFile() throws SQLException;
}
