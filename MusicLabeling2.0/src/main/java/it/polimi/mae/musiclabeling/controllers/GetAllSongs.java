package it.polimi.mae.musiclabeling.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.mae.musiclabeling.beans.Song;
import it.polimi.mae.musiclabeling.beans.User;
import it.polimi.mae.musiclabeling.dao.LabelsDAOImpl;
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
import java.util.List;

@MultipartConfig
@WebServlet("/GetAllSongs")
public class GetAllSongs extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        List<Song> songs;

        Connection connection = ConnectionHandler.getConnection(getServletContext());
        try {
            SongsDAOImpl songsDAO = new SongsDAOImpl(connection);
            LabelsDAOImpl labelsDAO = new LabelsDAOImpl(connection);
            if (user.isAdmin())
            {
                songs = songsDAO.getAllSongs(false);
            }
            else
            {
                songs = songsDAO.getAllSongsOfUser(user.getUserId(), false);
            }
            for (Song song : songs) {
                if (!user.isAdmin()){
                    if (!labelsDAO.getLabelsFromUserAndSong(user.getUserId(), song.getSongId()).isEmpty()) {
                        song.setHasLabels(true);
                    }
                }
                else if (!labelsDAO.getLabelsForSong(song.getSongId()).isEmpty()){
                    song.setHasLabels(true);
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while retrieving songs from database.");
            return;
        } finally {
            ConnectionHandler.closeConnection(connection);
        }

        if (songs.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("No songs available in the database.");
            return;
        }

        Gson gson = new GsonBuilder().setDateFormat("yyyy MM dd").create();
        String json = gson.toJson(songs);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
