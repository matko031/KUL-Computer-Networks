import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;

class Handler implements Runnable {
    private Socket socket;
    private DataOutputStream outToClient;
    private BufferedInputStream bytesFromClient;
    private String HTTPCommand;
    private List<String> HTTPCommands = new ArrayList<String>(Arrays.asList("GET", "POST", "PUT", "HEAD")); // define all supported http commands
    private int responseCode;
    private String encoding;
    private int bytesToRead;
    private byte[] buffer;
    private String path;
    private BufferedWriter bw; // used to save html text in file


    HashMap<String, String> responseCodes = new HashMap<String, String>() {{
        put("200", "OK");
        put("304", "Not modified");
        put("400", "Bad Request");
        put("404", "Page not found");
        put("500", "Server error");
    }};


    public Handler(Socket socket) {
        this.socket = socket;
        try {
            outToClient = new DataOutputStream(this.socket.getOutputStream());
            bytesFromClient = new BufferedInputStream(this.socket.getInputStream());
        } catch (IOException e){
            System.out.println("Error connecting with the client: " + e );
        }
    }


    /*
    Reads one line from the buffered reader. Implemented in spite of readline() method already existing
    due to the possible bugs and inconsistencies when using a combination of read() and readline() with BufferedReader
     */
    public String readLine(BufferedInputStream inFromServer) throws IOException{
        int b;
        char c;
        String line = "";
        while(( b = inFromServer.read()) != -1){ // while there is something to read
            c = (char) b; // convert read byte to the char
            line += c; // and add it to the line

            if (b == 10){ // b'10' is the newline character indicating end of line
                break;
            }
        }
        return line;
    }


    /*
    Read specified number of bytes from the given BufferedInputStream, save them in a byte array and return it
     */
    public byte[] readBytes(BufferedInputStream in, int nbBytes) throws IOException {
        int b;
        byte[] buffer = new byte[nbBytes];
        int index = 0;
        while ( index < nbBytes) { // read one byte nbBytes number of times and save it at an incrementing index in the array
            b = in.read();
            buffer[index] = (byte) b;
            index ++;
        }

        return buffer;
    }


    /*
    Takes in the list of arguments and check if they are all valid
    The order of arguments is HTTPCommand - URI - port - language
    Returns a boolean
     */
    private int checkRequest(String[] arguments){
        if(arguments.length != 3 || ! HTTPCommands.contains(arguments[0]) || ! arguments[2].equals("HTTP/1.1\r\n") ){
            return 400;
        }
        else if( ! arguments[1].equals("/")){
            return 404;
        }
        return 200;
    }


    /*
       Creates a file at the given location together with all the parent dirs needed
        */
    private boolean createFile(String filePath) throws IOException{
        File f = new File(filePath); // creates a file object with the given path
        if (f.getParentFile() != null) { // checks if there is parent folder given in the filePath, e.g., 'foo/bar.png' would return true and 'bar.png' would not
            f.getParentFile().mkdirs(); // creates the parent dirs
        }
        return f.createNewFile(); // creates the file at the filePath location and returns whether or not it succeeded in doing so
    }


    public void readRequest() throws IOException{
        String[] arguments = readLine(bytesFromClient).split(" ");
        responseCode = checkRequest(arguments);
        if (responseCode == 200){
            HTTPCommand = arguments[0];
            path = arguments[1].substring(1);

            if (HTTPCommand.equals("POST") || HTTPCommand.equals("PUT")){
                createFile(path);
            }

        }
    }

    /*
    Reads the headers of the incoming http response
     */
    private void readHeaders() throws IOException{
        // initialize starting values
        this.encoding = "";
        this.buffer = new byte[0];
        this.bytesToRead = 0;
        String line; // initialize string where current line from the response is saved

        do {
            line = readLine(bytesFromClient); // read input from server

            // if encoding hasn't been specified yet, check if this line is specifying it and indeed yes, change the encoding argument to the correct encoding
            if ((HTTPCommand.equals("POST") || HTTPCommand.equals("PUT")) && encoding.equals("")) {
                if (line.contains("Content-Length")) {
                    String bytes = line.split(" ")[1];
                    this.bytesToRead = Integer.parseInt(bytes.substring(0, bytes.length() - 2)); //convert the string specifying number of bytes to integer, drop the last two chars which are \r\n
                    this.buffer = new byte[this.bytesToRead]; // immediately make the buffer array since the number of bytes is already specified
                }
            }
        // \r\n line indicated the end of headers and we break the loop
        } while (!line.equals("\r\n"));
    }



    /*
    Reads the body of the incoming HTTP response
     */
    public String readBody() throws IOException{
        String body;
        buffer = readBytes(bytesFromClient, bytesToRead); // if encoding is of content-length type, read the correct amount of bytes in the buffer
        body = new String(buffer); // and save it to the variable 'line'
        System.out.print(body); // and print it

        bytesToRead = 0;
        return body;
    }


    private void processRequest() throws IOException{
        if (bytesToRead != 0) {
            String body = readBody();
            bw = new BufferedWriter(new FileWriter(path, HTTPCommand.equals("POST")));
            bw.write(body); // write the body to the file
            bw.flush(); // flush the buffer at the end of the loop to effectively write to the file
            bw.close();
        }
    }

    private void sendResponse() throws IOException{
        outToClient.writeBytes("HTTP/1.1 " + responseCode + " " + HTTPCommands.get(responseCode)+ "\n" +
                "Date: Sun, 10 Oct 2010 23:26:07 GMT\n" +
                "Content-Type: text/html\n");

        if (HTTPCommand.equals("GET") ){
            outToClient.writeBytes("Content-Length: 35\n" +
                    "\n" +
                    "Hello world en nog wat random tekst!\n");
        }
    }


    @Override
    public void run() {

        try {
            readRequest();
            if (responseCode == 200){
                readHeaders();
                if (HTTPCommand.equals("POST") || HTTPCommand.equals("PUT")){
                    processRequest();
                }
                sendResponse();

            }

        }

        catch (IOException e){
            System.out.println(e);
        }
    }
}