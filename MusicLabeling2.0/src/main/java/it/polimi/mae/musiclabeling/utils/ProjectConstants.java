package it.polimi.mae.musiclabeling.utils;

public class ProjectConstants {
    private static ProjectConstants singleton;

    private final int maxStringLength = 45;
    private final int maxSongDurationMs = 600000; // 10 Minutes
    private final int minTimeBetweenLabels = 500; // 0.5 seconds

    public static ProjectConstants getProjectConstants() {
        if(singleton == null)
            singleton = new ProjectConstants();
        return singleton;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public int getMaxSongDurationMs() {
        return maxSongDurationMs;
    }
    public int getMinTimeBetweenLabels() { return minTimeBetweenLabels; }
}
