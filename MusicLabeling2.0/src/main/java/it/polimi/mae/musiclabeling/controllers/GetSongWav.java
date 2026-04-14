package it.polimi.mae.musiclabeling.controllers;

import it.polimi.mae.musiclabeling.beans.Song;
import it.polimi.mae.musiclabeling.beans.User;
import it.polimi.mae.musiclabeling.dao.SongsDAOImpl;
import it.polimi.mae.musiclabeling.utils.ConnectionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;

@MultipartConfig
@WebServlet("/GetSongWav")
public class GetSongWav extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int songId;
        Song song;

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        try {
            songId = Integer.parseInt(request.getParameter("song_id"));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Song id is not an integer number.");
            return;
        }

        SongsDAOImpl songsDAO = new SongsDAOImpl(connection);
        try {
            if (!user.isAdmin() && !songsDAO.checkUserAccessToSong(user.getUserId(), songId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().println("You do not have permission to access this song");
                return;
            }
            song = songsDAO.getSong(songId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while handling database.");
            return;
        }

        // Serve the WAV file
        String fullFilePath = System.getenv("AUDIO_STORAGE_PATH") + File.separator + song.getFilePath();
        String fileExtension = getFileExtension(fullFilePath).toLowerCase();
        if (fileExtension.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Audio file does not have a file extension.");
            return;
        }
        if (fileExtension.equals("wav")) {
            response.setContentType("audio/wav");
        }
        else if (fileExtension.equals("mp3")) {
            response.setContentType("audio/mpeg");
        }
        else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Audio file does not have a valid file extension, was "+fileExtension+".");
            return;
        }
        try {
            ServletOutputStream out = response.getOutputStream();
            FileInputStream in = new FileInputStream(fullFilePath);
            BufferedInputStream bin = new BufferedInputStream(in);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = bin.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        } catch (FileNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("No existing files for this song.");
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while reading wav file.");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return ""; // Empty extension for files without an extension
        }
        return fileName.substring(lastIndexOfDot + 1);
    }
}