import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
    private ServerSocket server = null;
    private Map<String, String> users = new HashMap<>();
    private static final String USER_FILE = "passwd.txt";

    public Server(int port) {
        loadUsers(USER_FILE);
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");

            while (true) {
                System.out.println("Waiting for a client...");
                Socket socket = server.accept();
                System.out.println("Client accepted: " + socket.getInetAddress());
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    private void loadUsers(String file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.split(",");
                if (f.length == 4) {
                    users.put(f[0].trim(), f[1].trim());
                }
            }
        } catch (IOException i) {
            System.out.println("Could not read " + file + ": " + i);
        }
    }

    private void handleClient(Socket socket) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            boolean valid = false;

            for (int attempt = 1; attempt <= 3 && !valid; attempt++) {
                String user = in.readUTF();
                String pass = in.readUTF();

                if (pass.equals(users.get(user))) {
                    valid = true;
                    out.writeUTF("Welcome to CMSC445-Comp Networking class");
                    out.writeUTF("You are invited to use Your Own Name Machine");
                    System.out.println(user + " logged in");
                } else {
                    out.writeUTF("Only for ESU CPSC Students taking CMSC445");
                    out.writeUTF("You are not yet invited yet");
                    System.out.println("Failed attempt " + attempt + " for " + user);
                }
            }

            if (valid) {
                String line = "";
                while (!line.equals("Over")) {
                    line = in.readUTF();
                    System.out.println(line);
                }
            }

            System.out.println("Closing connection");
            socket.close();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) {
        new Server(5001);
    }
}