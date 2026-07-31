package it.polimi.mae.musiclabeling.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.mae.musiclabeling.beans.Song;
import it.polimi.mae.musiclabeling.beans.User;
import it.polimi.mae.musiclabeling.dao.SongsDAOImpl;
import it.polimi.mae.musiclabeling.utils.ConnectionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@MultipartConfig
@WebServlet("/GetSongInfo")
public class GetSongInfo extends HttpServlet {
    private static final long serialVersionUID = 1L;

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

        Connection connection = ConnectionHandler.getConnection(getServletContext());
        try {
            SongsDAOImpl songsDAO = new SongsDAOImpl(connection);
            if (!user.isAdmin() && !songsDAO.checkUserAccessToSong(user.getUserId(), songId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().println("You do not have permission to access this song");
                return;
            }
            song = songsDAO.getSong(songId);
            // Remove file path for security reasons
            song.setFilePath("");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while handling database.");
            return;
        } finally {
            ConnectionHandler.closeConnection(connection);
        }

        Gson gson = new GsonBuilder().setDateFormat("yyyy MM dd").create();
        String json = gson.toJson(song);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}