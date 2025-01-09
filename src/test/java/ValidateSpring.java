import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ValidateSpring {

    public static boolean checkTestSuccess(String serverPath, String clientPath, String serverUrl, String clientUrl) {
        Process serverProcess = null;
        Process clientProcess = null;
        try {
            // Step 1: Compile the server application
            if (!compileApplication(serverPath)) {
                System.err.println("Failed to compile server application.");
                return false;
            }

            // Step 2: Start the server application
            serverProcess = startApplication(serverPath);
            if (!waitForApplicationToStart(serverUrl)) {
                System.err.println("Server application failed to start.");
                return false;
            }

            // Step 3: Compile the client application
            if (!compileApplication(clientPath)) {
                System.err.println("Failed to compile client application.");
                return false;
            }

            // Step 4: Start the client application
            clientProcess = startApplication(clientPath);
            if (!waitForApplicationToStart(clientUrl)) {
                System.err.println("Client application failed to start.");
                return false;
            }

            // Step 5: Verify communication between client and server
            if (!isClientServerCommunicationSuccessful(clientUrl)) {
                System.err.println("Client and server failed to communicate.");
                return false;
            }

            return true; // Test successful
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            // Cleanup: Stop both applications
            stopApplication(serverProcess);
            stopApplication(clientProcess);
        }
    }

    private static boolean compileApplication(String path) throws IOException, InterruptedException {
        Process compileProcess = new ProcessBuilder("mvn", "clean", "compile")
                .directory(new File(path))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        return compileProcess.waitFor() == 0;
    }

    private static Process startApplication(String path) throws IOException {
        return new ProcessBuilder("mvn", "spring-boot:run")
                .directory(new File(path))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
    }

    private static boolean waitForApplicationToStart(String url) {
        int retries = 5;
        int waitTime = 5000; // 2 seconds

        for (int i = 0; i < retries; i++) {
            System.out.println("Checking if application is running...");
            if (isApplicationRunning(url)) {
                return true;
            }
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static boolean isApplicationRunning(String url) {
        try {
            URL applicationUrl = new URL(url+"/actuator/health");
            HttpURLConnection connection = (HttpURLConnection) applicationUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Application is running at " + url+"/actuator/health");
                return true;
            } else {
                System.err.println("Unexpected response code: " + responseCode + " from " + url+"/actuator/health");
                return false;
            }
        } catch (IOException e) {
            System.err.println("Failed to connect to " + url+"/actuator/health" + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean isClientServerCommunicationSuccessful(String clientUrl) {
        try {
            URL url = new URL(clientUrl + "/test"); // Assuming an endpoint to test communication
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private static void stopApplication(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }
}
