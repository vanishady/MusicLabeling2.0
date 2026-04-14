package it.polimi.mae.musiclabeling.beans;
import java.io.Serializable;

public class Song implements Serializable {
    private int songId;
    private String songName;
    private String artist;
    private String filePath;
    private boolean hasLabels;

    public Song() {
    }

    public Song(int songId, String songName, String artist, String filePath) {
        this.songId = songId;
        this.songName = songName;
        this.artist = artist;
        this.filePath = filePath;
        this.hasLabels = false;
    }

    // Getters and Setters
    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean hasLabels() {
        return hasLabels;
    }

    public void setHasLabels(boolean hasLabels) {
        this.hasLabels = hasLabels;
    }
}
