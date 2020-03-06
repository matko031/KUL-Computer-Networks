import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Handler implements Runnable {
    Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        List<String> HTTPcommands = new ArrayList<String>(Arrays.asList("GET", "POST", "PUT", "DELETE")); // define all supported http commands


        while (true) {

            try {
                DataOutputStream outToClient = new DataOutputStream(this.socket.getOutputStream());
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String clientSentence = inFromClient.readLine();

                //String[] arguments = clientSentence.split(" "); // split user input on every space

                //if(arguments.length != 4){
                    outToClient.writeBytes("HTTP/1.1 200 OK\n" +
                            "Date: Sun, 10 Oct 2010 23:26:07 GMT\n" +
                            "Server: Apache/2.2.8 (Ubuntu) mod_ssl/2.2.8 OpenSSL/0.9.8g\n" +
                            "Last-Modified: Sun, 26 Sep 2010 22:04:35 GMT\n" +
                            "ETag: \"45b6-834-49130cc1182c0\"\n" +
                            "Accept-Ranges: bytes\n" +
                            "Content-Length: 12\n" +
                            "Connection: close\n" +
                            "Content-Type: text/html\n" +
                            "\n" +
                            "Hello world!\n");
                //}
            }
            catch (IOException e) {
                ;
            }
        }
    }
}