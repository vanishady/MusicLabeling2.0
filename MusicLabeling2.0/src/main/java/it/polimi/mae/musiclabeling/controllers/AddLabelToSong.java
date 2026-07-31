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
@WebServlet("/AddLabelToSong")
public class AddLabelToSong extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int songId, labelId, labelTiming;
        float labelTimingFloat;
        ProjectConstants constants = ProjectConstants.getProjectConstants();

        try {
            songId = Integer.parseInt(request.getParameter("song_id"));
            labelId = Integer.parseInt(request.getParameter("label_id"));
            labelTimingFloat = Float.parseFloat(request.getParameter("label_timing"));
            labelTiming = Math.round(labelTimingFloat * 1000.0f); // Time is in milliseconds
        } catch (NumberFormatException | NullPointerException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error while parsing song and label ids.");
            return;
        }

        if(songId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Incorrect song id value.");
            return;
        }

        if(labelId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Incorrect label id value.");
            return;
        }

        if(labelTiming < 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Incorrect label timing value.");
            return;
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        Connection connection = ConnectionHandler.getConnection(getServletContext());
        try {
            LabelsDAOImpl labelsDAO = new LabelsDAOImpl(connection);
            SongsDAOImpl songsDAO = new SongsDAOImpl(connection);

            if (!user.isAdmin() && !songsDAO.checkUserAccessToSong(user.getUserId(), songId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().println("You do not have permission to add label to this song");
                return;
            }
            Song song = songsDAO.getSong(songId);
            if (song == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Song does not exist.");
                return;
            }
            Label label = labelsDAO.getLabel(labelId);
            if (label == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("One of the labels does not exist.");
                return;
            }
            if (!labelsDAO.labelCanBeAdded(user.getUserId(), songId, labelTiming - constants.getMinTimeBetweenLabels())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Cannot add label with timing greater than last.");
                return;
            }
            labelsDAO.addLabelToSong(user.getUserId(), songId, labelId, labelTiming);
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
