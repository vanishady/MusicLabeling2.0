package it.polimi.mae.musiclabeling.beans;
import java.io.Serializable;

public class Label implements Serializable {
    private int labelId;
    private String labelName;
    private int labelTiming;
    private int userSongLabelId;

    public Label() {
    }

    public Label(int labelId, String labelName) {
        this.labelId = labelId;
        this.labelName = labelName;
    }

    // Getters and Setters
    public int getLabelId() {
        return labelId;
    }

    public void setLabelId(int labelId) {
        this.labelId = labelId;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }
    public int getLabelTiming() { return labelTiming; }
    public void setLabelTiming(int timing) { this.labelTiming = timing; }
    public int getUserSongLabelId() { return userSongLabelId; }
    public void setUserSongLabelId(int userSongLabelId) { this.userSongLabelId = userSongLabelId; }
}
