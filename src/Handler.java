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
    private String contentType;
    private String serverDir = "server";
    private String requestedFilePath;
    private String htmlText;


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
            htmlText = readFile("my_website/home.html");

        } catch (IOException e){
            System.out.println("Error connecting with the client: " + e );
        }
    }


    /************************** UTILITIES **************************/

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
        else if( ( arguments[0].equals("GET") || arguments[0].equals("HEAD") ) && !(arguments[1].equals("/") || arguments[1].equals("/my_website/spaceodyssey.png") )){
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


    /*
        Read the the given file and return the string with the content
         */
    public String readFile(String fileName) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(fileName))); // encapsulate the FileInputStream with a BufferedReader
        String strLine;
        StringBuilder fileText = new StringBuilder();
        while ( (strLine = reader.readLine()) != null){ // while there is something to read, read new line from the file
            fileText.append(strLine).append("\r\n");
        }
        return fileText.toString(); // return the string with content
    }


    /*
        Read the the given file and return the string with the content
         */
    public byte[] readImage(String fileName) throws IOException{
        BufferedInputStream imgStream = new BufferedInputStream(new FileInputStream(fileName));
        File img = new File("my_website/spaceodyssey.png");
        int imgSize = (int) img.length();
        return readBytes(imgStream, imgSize);
    }



    /************************** REAL STUFF **************************/



    public void readRequest() throws IOException{
        String request =  readLine(bytesFromClient);
        String[] arguments = request.split(" ");
        System.out.print(request);
        responseCode = checkRequest(arguments);
        if (responseCode == 200){
            HTTPCommand = arguments[0];
            path = arguments[1].substring(1);

            if (HTTPCommand.equals("POST") || HTTPCommand.equals("PUT")){
                createFile(serverDir + File.separator + path);
            }
        }
        else{
            sendResponse();
        }
    }

    /*
    Reads the headers of the incoming http response
     */
    public void readHeaders() throws IOException{
        // initialize starting values
        HashMap <String, String> headers = new HashMap<String, String>();
        this.encoding = "";
        this.buffer = new byte[0];
        this.bytesToRead = 0;
        String line; // current line from the response

        // \r\n line indicated the end of headers and we break the loop
        do {
            line = readLine(bytesFromClient); // read input from server
            System.out.print(line);
            if (line.contains(":")) {
                String[] splitLine = line.split(":");
                headers.put(splitLine[0], splitLine[1]);
            } else if (!line.equals("\r\n")){
                headers.put("response-code", line);
            }
        } while (!line.equals("\r\n"));

        if (headers.get("Transfer-Encoding") != null){
            this.encoding = "Transfer-Encoding";
        } else if (headers.get("Content-Length") != null) {
            this.encoding = "Content-Length";
            String bytes = headers.get("Content-Length").replace(" ", "");
            this.bytesToRead = Integer.parseInt(bytes.substring(0, bytes.length() - 2)); //convert the string specifying number of bytes to integer, drop the last two chars which are \r\n
            this.buffer = new byte[this.bytesToRead]; // immediately make the buffer array since the number of bytes is already specified
        }

        if (headers.get("Content-Type") != null) {
            String[] content = headers.get("Content-Type").split("/");
            String contentType = content[0].replace(" ", "");
            int endIndex = content[1].contains(";") ? content[1].indexOf(";") : content[1].length() - 1;
            String fileFormat = content[1].substring(0, endIndex);
            this.contentType = contentType;

            requestedFilePath = serverDir + File.separator + path;
            createFile(requestedFilePath);
            this.bw = new BufferedWriter(new FileWriter(requestedFilePath));
        }
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
        if ( HTTPCommand.equals("POST") || HTTPCommand.equals("PUT")) {
            String body = readBody();
            bw = new BufferedWriter(new FileWriter(path, HTTPCommand.equals("POST")));
            bw.write(body); // write the body to the file
            bw.flush(); // flush the buffer at the end of the loop to effectively write to the file
            bw.close();
        }
    }

    private void sendResponse() throws IOException{
        outToClient.writeBytes("HTTP/1.1 " + responseCode + " " + responseCodes.get(String.valueOf(responseCode)+ "\r\n"));

        if (responseCode == 200) {
            if ((HTTPCommand.equals("GET") || HTTPCommand.equals("HEAD"))) {
                if (path.equals("/") || path.equals("")) {
                    String htmlSize = String.valueOf(htmlText.length());
                    outToClient.writeBytes("Content-Type: text/html\r\n" +
                            "Content-Length: " + htmlSize + "\r\n" +
                            "\r\n" +
                            htmlText);
                } else if (path.equals("my_website/spaceodyssey.png")) {
                    File img = new File("my_website/spaceodyssey.png");
                    String imgSize = String.valueOf(img.length());
                    byte[] image = readImage("my_website/spaceodyssey.png");
                    String htmlSize = String.valueOf(htmlText.length());
                    outToClient.writeBytes("Content-Type: image/png\r\n" +
                            "Content-Length: " + imgSize + "\r\n" +
                            "\r\n");
                    outToClient.write(image);
                }
            }
        }
        else{
            outToClient.writeBytes("Content-Type: text/html\r\n" +
                    "Content-Length: 20" + "\r\n" +
                    "\r\n" +
                    "There was some error");
        }
    }


    @Override
    public void run() {
        while(true) {

            try {
                readRequest();
                if (responseCode == 200) {
                    readHeaders();
                    if (HTTPCommand.equals("POST") || HTTPCommand.equals("PUT")) {
                        processRequest();
                    }
                }
                sendResponse();

            }

            catch (IOException e){
                System.out.println(e);
            }
        }
    }
}