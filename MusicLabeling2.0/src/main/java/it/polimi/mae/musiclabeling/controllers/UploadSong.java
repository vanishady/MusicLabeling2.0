package it.polimi.mae.musiclabeling.controllers;

import it.polimi.mae.musiclabeling.beans.User;
import it.polimi.mae.musiclabeling.dao.SongsDAOImpl;
import it.polimi.mae.musiclabeling.utils.ConnectionHandler;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

@MultipartConfig(fileSizeThreshold=1024*1024*10,    // 10 MB
        maxFileSize=1024*1024*200,          // 200 MB
        maxRequestSize=1024*1024*250)      // 250 MB
@WebServlet("/UploadSong")
public class UploadSong extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (!user.isAdmin()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You need admin privileges for this.");
            return;
        }

        Integer userId = null;
        String userIdString = request.getParameter("userId");
        String songName = request.getParameter("songName");
        String artist = request.getParameter("artist");
        Part songFilePart = request.getPart("songFile");

        try {
            userId = Integer.parseInt(userIdString);
        } catch (NumberFormatException | NullPointerException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error while parsing user id.");
            return;
        }
        if (songName.isEmpty() || songName.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Please set a valid song name.");
            return;
        }
        if (artist.isEmpty() || artist.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Please set a valid artist.");
            return;
        }

        // Validate file type
        String contentType = songFilePart.getContentType();
        if (!contentType.equals("audio/mpeg") && !contentType.equals("audio/wav") && !contentType.equals("audio/x-wav")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid file type. Only MP3 and WAV are allowed.");
            return;
        }

        // Generate a unique file name (considering file extension)
        String extension = contentType.equals("audio/mpeg") ? ".mp3" : ".wav";
        String fileName = java.util.UUID.randomUUID() + extension;
        String storageDir = System.getenv("AUDIO_STORAGE_PATH");
        String savePath = storageDir + File.separator + fileName;

        // Ensure storage directory exists
        Files.createDirectories(Paths.get(storageDir));

        // Save the file using stream copy (more reliable across different filesystems)
        try (java.io.InputStream in = songFilePart.getInputStream()) {
            Files.copy(in, Paths.get(savePath));
        } catch (IOException e) {
            Files.deleteIfExists(Paths.get(savePath));
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Unable to upload file on server.");
            return;
        }

        // Now save songName, artist, and savePath to your database
        Connection connection = ConnectionHandler.getConnection(getServletContext());
        try {
            SongsDAOImpl songsDAO = new SongsDAOImpl(connection);
            songsDAO.addSongToUser(userId, songName, artist, fileName);
        } catch (SQLException e) {
            e.printStackTrace();
            Files.deleteIfExists(Paths.get(savePath));
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("DB error while uploading song.");
            return;
        } finally {
            ConnectionHandler.closeConnection(connection);
        }

        response.getWriter().write("Song uploaded successfully!");
    }
}

