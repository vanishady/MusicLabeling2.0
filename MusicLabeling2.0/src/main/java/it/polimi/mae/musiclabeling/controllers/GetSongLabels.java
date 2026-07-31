package it.polimi.mae.musiclabeling.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.mae.musiclabeling.beans.Label;
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
@WebServlet("/GetSongLabels")
public class GetSongLabels extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int songId;
        List<Label> labels;

        try {
            songId = Integer.parseInt(request.getParameter("song_id"));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Song id is not an integer number.");
            return;
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        Connection connection = ConnectionHandler.getConnection(getServletContext());
        try {
            LabelsDAOImpl labelsDAO = new LabelsDAOImpl(connection);
            if (!user.isAdmin()){
                labels = labelsDAO.getLabelsFromUserAndSong(user.getUserId(), songId);
            }
            else{
                labels = labelsDAO.getLabelsForSong(songId);
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while retrieving labels from database.");
            return;
        } finally {
            ConnectionHandler.closeConnection(connection);
        }

        if (labels == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while loading labels.");
            return;
        }

        Gson gson = new GsonBuilder().setDateFormat("yyyy MM dd").create();
        String json = gson.toJson(labels);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}