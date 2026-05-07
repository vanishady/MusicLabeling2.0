package it.polimi.mae.musiclabeling.controllers;

import it.polimi.mae.musiclabeling.beans.Label;
import it.polimi.mae.musiclabeling.beans.User;
import it.polimi.mae.musiclabeling.dao.LabelsDAOImpl;
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
import java.util.List;

@MultipartConfig
@WebServlet("/UpdateLabelTiming")
public class UpdateLabelTiming extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int userSongLabelId, newTimingMs;

        try {
            userSongLabelId = Integer.parseInt(request.getParameter("user_song_label_id"));
            newTimingMs = Integer.parseInt(request.getParameter("new_timing_ms"));
        } catch (NumberFormatException | NullPointerException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error while parsing parameters.");
            return;
        }

        if (userSongLabelId <= 0 || newTimingMs < 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid parameter values.");
            return;
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        LabelsDAOImpl labelsDAO = new LabelsDAOImpl(connection);
        ProjectConstants constants = ProjectConstants.getProjectConstants();

        try {
            if (!labelsDAO.userCanDeleteSong(userSongLabelId, user.getUserId()) && !user.isAdmin()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().println("You do not have permission to update this label.");
                return;
            }

            // Retrieve all labels for the same user/song to validate ordering
            // We need the song_id for the target label first
            // Use getLabelsFromUserAndSong requires userId and songId — fetch from DB via a targeted query
            // We'll call a dedicated lookup
            int songId = labelsDAO.getSongIdForLabel(userSongLabelId);
            if (songId < 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Label not found.");
                return;
            }

            List<Label> labels = user.isAdmin()
                    ? labelsDAO.getLabelsForSong(songId)
                    : labelsDAO.getLabelsFromUserAndSong(user.getUserId(), songId);

            // Find the position of the label being moved
            int pos = -1;
            for (int i = 0; i < labels.size(); i++) {
                if (labels.get(i).getUserSongLabelId() == userSongLabelId) {
                    pos = i;
                    break;
                }
            }
            if (pos < 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Label not found in song.");
                return;
            }

            int minTiming = pos == 0 ? 0 : labels.get(pos - 1).getLabelTiming() + constants.getMinTimeBetweenLabels();
            int maxTiming = (pos + 1 < labels.size())
                    ? labels.get(pos + 1).getLabelTiming() - constants.getMinTimeBetweenLabels()
                    : Integer.MAX_VALUE;

            if (newTimingMs < minTiming || newTimingMs > maxTiming) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("New timing violates ordering constraints.");
                return;
            }

            labelsDAO.updateLabelTiming(userSongLabelId, newTimingMs);
        } catch (SQLException e) {
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
