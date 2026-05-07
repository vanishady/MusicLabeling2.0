package it.polimi.mae.musiclabeling.controllers;

import it.polimi.mae.musiclabeling.beans.User;
import it.polimi.mae.musiclabeling.utils.ConnectionHandler;
import it.polimi.mae.musiclabeling.utils.ProjectConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import it.polimi.mae.musiclabeling.beans.Song;
import it.polimi.mae.musiclabeling.dao.SongsDAOImpl;

@MultipartConfig
@WebServlet("/DeleteSong")
public class DeleteSong extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int songId;
        ProjectConstants constants = ProjectConstants.getProjectConstants();

        try {
            songId = Integer.parseInt(request.getParameter("song_id"));
        } catch (NumberFormatException | NullPointerException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error while parsing song_id.");
            return;
        }

        if(songId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Incorrect song id value.");
            return;
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (!user.isAdmin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().println("Only admin can delete songs.");
            return;
        }

        SongsDAOImpl songsDao = new SongsDAOImpl(connection);
        Song song;
        try {
            song = songsDao.getSong(songId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while handling database.");
            return;
        }

        if (song == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("Song not found.");
            return;
        }

        // Construct the file path
        String fileName = song.getFilePath();
        String filePath = System.getenv("AUDIO_STORAGE_PATH") + File.separator + fileName;

        // Create a File object representing the audio file
        File audioFile = new File(filePath);

        // Check if the file exists
        if (audioFile.exists()) {
            // Attempt to delete the file
            boolean deleted = audioFile.delete();
            if (!deleted) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println("Can't delete the audio file.");
                return;
            }
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("File not found.");
            return;
        }

        //Delete song information from database
        try {
            songsDao.deleteSong(songId);
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while handling database.");
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }

    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
