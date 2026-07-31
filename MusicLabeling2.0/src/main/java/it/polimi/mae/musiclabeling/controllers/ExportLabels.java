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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import java.net.URI;
@MultipartConfig
@WebServlet("/ExportLabels")
public class ExportLabels extends HttpServlet {
 private static final long serialVersionUID = 1L;

 protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
 HttpSession session = request.getSession();
 User user = (User) session.getAttribute("user");
 if (!user.isAdmin()) {
 response.setStatus(HttpServletResponse.SC_FORBIDDEN);
 response.getWriter().println("Only admin can export labels");
 return;
 }
 JsonObject root;
 Connection connection = ConnectionHandler.getConnection(getServletContext());
 try {
 LabelsDAOImpl labelsDAO = new LabelsDAOImpl(connection);
 root = labelsDAO.exportLabelsToFile();
 } catch (SQLException e) {
 response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 response.getWriter().println("Error while handling database: " + e.getMessage());
 return;
 } finally {
 ConnectionHandler.closeConnection(connection);
 }
 if (root == null) {
 response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 response.getWriter().println("No labels found.");
 return;
 }
 Gson gson = new GsonBuilder().setPrettyPrinting().create();
 String jsonString = gson.toJson(root);
 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
 String dateString = dateFormat.format(new Date());
 String fileName = dateString + ".json";
 // Save to local storage
 String storagePath = "/app/storage/" + fileName;
 try {
 Files.write(Paths.get(storagePath), jsonString.getBytes("UTF-8"));
 } catch (IOException e) {
 response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 response.getWriter().println("Error saving file to local storage: " + e.getMessage());
 return;
 }
 // Save to S3 Bucket
 try {
 uploadToS3(fileName, jsonString);
 } catch (Exception e) {
 response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 response.getWriter().println("Error saving to bucket: " + e.getMessage());
 return;
 }
 // Send response
 response.setContentType("application/json");
 response.setStatus(HttpServletResponse.SC_OK);
 JsonObject result = new JsonObject();
 result.addProperty("success", true);
 result.addProperty("fileName", fileName);
 result.addProperty("message", "Labels exported successfully to storage and bucket");
 response.getWriter().write(result.toString());
 }
 private void uploadToS3(String fileName, String jsonContent) throws Exception {
 String bucketName = System.getenv("BUCKET_NAME");
 String region = System.getenv("BUCKET_REGION");
 String endpoint = System.getenv("BUCKET_ENDPOINT");
 String accessKey = System.getenv("BUCKET_ACCESS_KEY");
 String secretKey = System.getenv("BUCKET_SECRET_KEY");
 
 if (bucketName == null || accessKey == null || secretKey == null) {
 throw new Exception("Bucket environment variables not configured");
 }
 
 AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
 
 S3Client s3Client = S3Client.builder()
 .credentialsProvider(StaticCredentialsProvider.create(credentials))
 .endpointOverride(new URI(endpoint))
 .region(Region.of(region != null ? region : "us-east-1"))
 .build();
 
 try {
 PutObjectRequest putRequest = PutObjectRequest.builder()
 .bucket(bucketName)
 .key(fileName)
 .build();
 
 s3Client.putObject(putRequest, RequestBody.fromString(jsonContent));
 } finally {
 s3Client.close();
 }
 }
}