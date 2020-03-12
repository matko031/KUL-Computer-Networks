import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;
import java.lang.*;

class Client  {

    private static List<String> HTTPCommands = new ArrayList<String>(Arrays.asList("GET", "POST", "PUT", "HEAD")); // define all supported http commands;

    private static String URIPatternString = "^(https?:\\/\\/)?(((www)\\.)?((.*?)\\.)+?(.+?))(\\/.*)?$"; // regex to test if the URI is valid, not 100% waterproof
    private static Pattern URIPatternRegex = Pattern.compile(URIPatternString); // compile regex

    private static String isIntPatternString = "\\d+"; //regex to test if the given port is an int
    private static Pattern isIntRegex = Pattern.compile(isIntPatternString); // compile regex

    private DataOutputStream outToServer;
    private BufferedInputStream bytesFromServer;
    private Socket clientSocket;
    private BufferedWriter bw;
    private BufferedOutputStream ibw;

    private String HTTPCommand;
    private String host;
    private String port;
    private String path;
    private String filename;
    private String lang;
    private String encoding = "";
    private String contentType = "";
    private byte[] buffer = new byte[0];
    private int bytesToRead = 0;
    public boolean argumentsCorrect = true;


    public void Client() {

    }



    /*
    Takes in the list of arguments and check if they are all valid
    The order of arguments is HTTPCommand - URI - port - language
    Returns HTTPCommand, host, path, port, language
     */
    static boolean checkArguments(String[] arguments){
        if(arguments.length < 2 || arguments.length > 4){
            System.out.println("Wrong number of arguments"); // port and language are optional, therefore valid number of arguments is two, three or four
            return false;
        }

        String HTTPcommand = arguments[0];
        if( ! HTTPCommands.contains(HTTPcommand)){
            System.out.println("Wrong http command"); // checks if a supported http command is given
            return false;
        }

        Matcher uriMatcher = URIPatternRegex.matcher(arguments[1]);  // create a matcher that will match the given URI against the pattern
        if(! uriMatcher.matches()){
            System.out.println("Unsupported URI"); // checks if the URI matches the expected URI pattern
            return false;
        }

        if (arguments.length > 2) {
            String port = arguments[2];
            Matcher portMatcher = isIntRegex.matcher(port);  // initialise a matcher that will check if the given port is an int
            if (!portMatcher.matches()) {
                System.out.println("Port must be an integer"); // checks if the given port is an int
                return false;
            }
        }
        return true;
    }

    /*
    Prompts the user for the argument and returns them as a list of strings
     */
    public String[] readArguments(){
        Scanner myObj = new Scanner(System.in); // create scanner object
        String command = myObj.nextLine();  // read user input
        //String command = "GET www.google.com 80 en"; // used for debugging in order not to have to type the URI every time
        return command.split("\\s+"); // split user input on every space and return the array
    }



    /*
    Reads the arguments, checks them and saves them in their respective variables
     */
    public void setArguments() throws IOException{
        String[] arguments = readArguments();
        if (this.checkArguments(arguments)) {
            this.HTTPCommand = arguments[0];

            // save found groups in their respective variables
            Matcher uriMatcher = URIPatternRegex.matcher(arguments[1]);  // create a matcher that will match the given URI against the pattern
            uriMatcher.find(); // apply the Matcher to the URI and extract the host and file path groups
            this.host = uriMatcher.group(2);
            this.path = uriMatcher.group(8) == null ? "/" : uriMatcher.group(6);
            this.filename = uriMatcher.group(6);

            this.port = arguments.length > 2 ? arguments[2] : "80"; // if port is given save it to its respective variable
            this.lang = arguments.length > 3 ? arguments[3] : "en"; // same for language

            createFiles();
        }

        else {
            this.argumentsCorrect = false;
        }
    }

    public void createFiles() throws IOException {
        boolean made_dir = true;
        boolean made_file = true;
        String dir_path = "websites/" + filename;
        File website_dir = new File(dir_path);
        if (! website_dir.exists()){
            made_dir = website_dir.mkdir();
        }

        String file_path = "websites/" + filename + "/" + filename + ".html";
        File website_file = new File(file_path);
        if(! website_file.exists()) {
            made_file = website_file.createNewFile();
        }

        if ( made_dir && made_file) {
            this.bw = new BufferedWriter(new FileWriter("websites/" + filename + "/" + filename + ".html")); // initialise the buffered writer
        }
    }

    /*
    Creates a socket object and connects with the host on the given port, both saved in the attributes of this object
     */
    public void connectSocket() throws IOException {

        this.clientSocket = new Socket(this.host, Integer.parseInt(this.port)); // open a socket with the host
        this.outToServer = new DataOutputStream(clientSocket.getOutputStream()); // create output stream object
        this.bytesFromServer = new BufferedInputStream(clientSocket.getInputStream()); // create input stream object

    }

    /*
    Sends the given command to the server in the form of HTTP request.
    Only used for GET and HEAD requests
     */
    public void sendGETHEADRequest() throws IOException{

        // construct the header strings and save them in an array
        String header1 = this.HTTPCommand + " " + this.path + " HTTP/1.1";
        String header2 = "host: " + this.host;
        String[] outputs =  new String[]{header1, header2};

        // loop over the header strings, write them to server and add newline at the end
        for (String output : outputs) {
            this.outToServer.writeBytes(output);
            this.outToServer.writeBytes("\n");
        }
        this.outToServer.writeBytes("\n"); // write the final newline to indicate the end of the request

    }

    /*
   Sends the given command to the server in the form of HTTP request.
   Only used for POST and PUT requests
    */
    public void sendPOSTPUTRequest()
    {

    }

    /*
    Sends the correct command to the server
     */
    public void sendCommand() {
        try {
            if (this.HTTPCommand.equals("GET") || this.HTTPCommand.equals("HEAD")) {
                this.sendGETHEADRequest();
            } else if (this.HTTPCommand.equals("POST") || this.HTTPCommand.equals("PUT")) {
                this.sendPOSTPUTRequest();
            }
        }
        catch (IOException e){
            System.out.println(e);
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

    public byte[] readBytes(BufferedInputStream in, int nbBytes) throws IOException {
        int b;
        byte[] buffer = new byte[nbBytes];
        int index = 0;
        while ( index < nbBytes) {
            b = in.read();
            buffer[index] = (byte) b;
            index ++;
        }

        return buffer;
    }

    /*
    Reads the headers of the incoming http response
     */
    public void readHeaders() throws IOException{
        // initialize starting values
        this.encoding = "";
        this.buffer = new byte[0];
        this.bytesToRead = 0;
        String line; // initialize string where current line from the response is saved

        while (true) {
            line = readLine(bytesFromServer); // read input from server,
            System.out.print(line); // print it

            // if encoding hasn't been specified yet, check if this line is specifying it and indeed yes, change the encoding argument to the correct encoding
            if (this.encoding.equals("")) {
                if (line.contains("Transfer-Encoding: chunked")) {
                    this.encoding = "Transfer-Encoding";
                } else if (line.contains("Content-Length")) {
                    this.encoding = "Content-Length";
                    String bytes = line.split(" ")[1];
                    this.bytesToRead = Integer.parseInt(bytes.substring(0, bytes.length()-2));
                    this.buffer = new byte[this.bytesToRead]; // in case of content-length encoding, immediately make the buffer array

                }
            }

            if (this.contentType.equals("")){
                if (line.contains("Content-Type")){
                    if (line.contains("text/html")){
                        this.contentType = "html";
                    }
                    else if (line.contains("image")){
                        this.contentType = "img";
                    }
                }
            }

            if (line.equals("\r\n")) { // \r\n line indicated the end of headers, buffer writer may be flushed and we break the loop
                bw.flush();
                break;
            }
        }
    }

    /*
    Reads the body of the incoming HTTP response
     */
    public void readBody() throws IOException{

        String line;

        if (this.encoding.equals("Content-Length")) {
            this.buffer = readBytes(this.bytesFromServer, this.bytesToRead); // if encoding is of content-length type, read the correct amount of bytes in the buffer

            if (this.contentType.equals("img")){
                this.ibw.write(buffer); // write the body to the file
                this.ibw.close();
            } else {
                line = new String(this.buffer); // and save it to the variable 'line'
                this.bw.write(line); // write the body to the file
                System.out.print(line); // and print it
            }

        } else {

            while (true) {

                // read a line from server and print is
                line = readLine(bytesFromServer);
                System.out.print(line);

                // last line contains 0 and CL LF
                if(line.equals("0\r\n")){
                    break;
                }

                if (!line.equals("\r\n")) {
                    bytesToRead = Integer.parseInt(line.substring(0 , line.length()-2), 16);
                    buffer = readBytes(bytesFromServer, bytesToRead);
                    line = new String(buffer);
                }

                System.out.print(line);
                bw.write(line);

            }
        }
        bw.close();
    }


    public String readFile(String fileName) throws IOException{
        FileInputStream stream = new FileInputStream("websites/"+fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String strLine;
        String fileText = "";
        while ( (strLine = reader.readLine()) != null){
            fileText += strLine;
        }
        return fileText;
    }

    public void getImgs() throws IOException{

        String URIPatternString = "<img.+?src\\s*?=\\s*?\"(.+?)\""; // regex to test if the URI is valid, not 100% waterproof
        Pattern URIPatternRegex = Pattern.compile(URIPatternString); // compile regex

        String htmlFile = readFile(filename + "/" + filename + ".html");
        Matcher uriMatcher = URIPatternRegex.matcher(htmlFile);  // create a matcher that will match the given URI against the pattern
        // apply the Matcher to the URI and extract the host and file path groups
        while(uriMatcher.find()){
            this.path = uriMatcher.group(1);
            this.HTTPCommand = "GET";
            this.ibw = new BufferedOutputStream(new FileOutputStream(filename+"/" + path));

            sendGETHEADRequest();
            readHeaders();
            readBody();
        }
    }
}


