import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Server  {
    public static void main(String argv[]) throws Exception    {

        ServerSocket welcomeSocket = new ServerSocket(3000);

        while(true) {
            Socket clientSocket = welcomeSocket.accept();

            if (clientSocket != null)   {
                Handler h = new Handler(clientSocket);
                Thread thread = new Thread(h);
                thread.start();
            }
        }
    }
}


