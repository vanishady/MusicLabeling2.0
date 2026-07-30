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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;

@MultipartConfig
@WebServlet("/ExportLabels")
public class ExportLabels extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (!user.isAdmin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().println("Only admin can export labels");
            return;
        }

        LabelsDAOImpl labelsDAO = new LabelsDAOImpl(connection);
        JsonObject root = null;
        try {
            root = labelsDAO.exportLabelsToFile();
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error while handling database: " + e.getMessage());
            return;
        }

        if (root == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("No labels found.");
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(root);

        // Define the format for the date string in the filename
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateString = dateFormat.format(new Date());
        String fileName = dateString + ".json";

        // Save the JSON file to the configured storage path
        try {
            String storagePath = System.getenv("AUDIO_STORAGE_PATH") + java.io.File.separator + fileName;
            Files.write(Paths.get(storagePath), jsonString.getBytes());
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error saving file to storage: " + e.getMessage());
            return;
        }

        // Send the JSON as a file download to the browser
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(jsonString);
    }

    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}