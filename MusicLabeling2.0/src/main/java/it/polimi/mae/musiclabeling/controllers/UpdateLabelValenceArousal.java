package it.polimi.mae.musiclabeling.controllers;

import it.polimi.mae.musiclabeling.beans.User;
import it.polimi.mae.musiclabeling.dao.LabelsDAOImpl;
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
@WebServlet("/UpdateLabelValenceArousal")
public class UpdateLabelValenceArousal extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int userSongLabelId;
        float valence, arousal;

        try {
            userSongLabelId = Integer.parseInt(request.getParameter("user_song_label_id"));
            valence = Float.parseFloat(request.getParameter("valence"));
            arousal = Float.parseFloat(request.getParameter("arousal"));
        } catch (NumberFormatException | NullPointerException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error while parsing parameters.");
            return;
        }

        if (userSongLabelId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid label id.");
            return;
        }

        if (valence < 0.0f || valence > 1.0f || arousal < 0.0f || arousal > 1.0f) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Valence and arousal must be in range [0, 1].");
            return;
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        Connection connection = ConnectionHandler.getConnection(getServletContext());
        try {
            LabelsDAOImpl labelsDAO = new LabelsDAOImpl(connection);
            if (!labelsDAO.userCanDeleteSong(userSongLabelId, user.getUserId()) && !user.isAdmin()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().println("You do not have permission to update this label.");
                return;
            }

            labelsDAO.updateLabelValenceArousal(userSongLabelId, valence, arousal);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while handling database.");
            return;
        } finally {
            ConnectionHandler.closeConnection(connection);
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}
