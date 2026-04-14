package it.polimi.mae.musiclabeling.controllers;

import it.polimi.mae.musiclabeling.beans.Label;
import it.polimi.mae.musiclabeling.beans.Song;
import it.polimi.mae.musiclabeling.beans.User;
import it.polimi.mae.musiclabeling.dao.LabelsDAOImpl;
import it.polimi.mae.musiclabeling.dao.SongsDAOImpl;
import it.polimi.mae.musiclabeling.utils.ConnectionHandler;
import it.polimi.mae.musiclabeling.utils.ProjectConstants;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@MultipartConfig
@WebServlet("/DeleteSongLabel")
public class DeleteSongLabel extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int userSongLabelId;
        ProjectConstants constants = ProjectConstants.getProjectConstants();

        try {
            userSongLabelId = Integer.parseInt(request.getParameter("user_song_label_id"));
        } catch (NumberFormatException | NullPointerException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error while parsing user_song_label_id.");
            return;
        }

        if(userSongLabelId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Incorrect song id value.");
            return;
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        LabelsDAOImpl labelsDAO = new LabelsDAOImpl(connection);

        try {
            if (!labelsDAO.userCanDeleteSong(userSongLabelId, user.getUserId())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("This user cannot delete the selected label.");
                return;
            }
            labelsDAO.deleteSongLabel(userSongLabelId);
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
