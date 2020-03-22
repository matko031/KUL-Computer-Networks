import java.io.IOException;

public class imgRequest implements Runnable{

    Client socket;

    imgRequest(Client socket){
        this.socket = socket;

    }



    @Override
    public void run() {
        try {
            socket.sendRequest(); // send the GET request to download the image
            socket.readHeaders(); // read the response headers
            if ( socket.responseCode == 200) {
                socket.readBody(); // read the body, a.k.a. download the actual image bytes
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
