import org.jetbrains.annotations.NotNull;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;
import java.lang.*;
import java.nio.file.Paths;

class Client  {

    private static List<String> HTTPCommands = new ArrayList<String>(Arrays.asList("GET", "POST", "PUT", "HEAD")); // define all supported http commands;

    private static String URIPatternString = "^(https?:\\/\\/)?(((www)\\.)?((.*?)\\.)+?(.+?))(\\/.*)?$"; // regex to test if the URI is valid, not 100% waterproof
    private static Pattern URIPatternRegex = Pattern.compile(URIPatternString); // compile regex

    private static String imgPatternString = "(<img.+?src\\s*?=\\s*?\")(.+?)(\")"; // regex to find all the img tags in the html file
    private static Pattern imgPatternRegex = Pattern.compile(imgPatternString); // compile regex

    private static String isIntPatternString = "\\d+"; //regex to test if the given port is an int
    private static Pattern isIntRegex = Pattern.compile(isIntPatternString); // compile regex

    private DataOutputStream outToServer; // used to write to server
    private BufferedInputStream bytesFromServer; // used to receive info from the server
    private Socket clientSocket; // used to connect with the server
    private BufferedWriter bw; // used to save html text in file
    private BufferedOutputStream ibw; // used to store image bytes in a file

    private String HTTPCommand; // HTTP command typed by the user
    private String host; //  host typed by the user
    private String port; // port typed by the user, default value of 80
    private String path; // path to the file specified by the user
    private String htmlFilename; // part of the host after www, used to create the html file, e.g., www.google.com will be saved in google.html
    private String htmlFileRelativePath; // path of the htmlFile
    private String lang; // language specified by the user, default en
    private String encoding = ""; // encoding specified in the http response headers
    private String contentType = ""; // content type specified in the response headers
    private byte[] buffer = new byte[0]; // buffer used to save the incoming body bytes
    private int bytesToRead = 0; // number of bytes to read specified in the response
    public boolean argumentsCorrect = true; // specifies if all the arguments given by the user are correct


    public void Client() {

    }



    /*
    Takes in the list of arguments and check if they are all valid
    The order of arguments is HTTPCommand - URI - port - language
    Returns a boolean
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

            Matcher uriMatcher = URIPatternRegex.matcher(arguments[1]);  // create a matcher that will match the given URI against the pattern
            uriMatcher.find(); // apply the Matcher to the URI and extract the host and file path groups
            // save found groups in their respective variables
            this.host = uriMatcher.group(2);
            this.path = uriMatcher.group(8) == null ? "/" : uriMatcher.group(6);
            this.htmlFilename = uriMatcher.group(6);

            this.port = arguments.length > 2 ? arguments[2] : "80"; // if port is given save it to its respective variable
            this.lang = arguments.length > 3 ? arguments[3] : "en"; // same for language

            this.htmlFileRelativePath = "websites" + File.separator + htmlFilename + File.separator + htmlFilename + ".html"; // location of the html file
            createFile(htmlFileRelativePath);
            this.bw = new BufferedWriter(new FileWriter(htmlFileRelativePath));
        }

        else {
            this.argumentsCorrect = false;
        }
    }


    /*
    Creates a file at the given location together with all the parent dirs needed
     */
    private boolean createFile(String filePath) throws IOException{
        boolean made_file;
        File f = new File(filePath); // creates a file object with the given path
        if (f.getParentFile() != null) { // checks if there is parent folder given in the filePath, e.g., 'foo/bar.png' would return true and 'bar.png' would not
             f.getParentFile().mkdirs(); // creates the parent dirs
        }
        return f.createNewFile(); // creates the file at the filePath location and returns whether or not it succeeded in doing so
    }


    /*
    Creates a socket object and connects with the host on the given port, both saved in the attributes of this object
     */
    public void connectSocket() throws IOException {

        this.clientSocket = new Socket(this.host, Integer.parseInt(this.port)); // open a socket with the host
        this.outToServer = new DataOutputStream(clientSocket.getOutputStream()); // initialize output stream object
        this.bytesFromServer = new BufferedInputStream(clientSocket.getInputStream()); // initialize input stream object

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
            this.outToServer.writeBytes("\r\n");
        }
        this.outToServer.writeBytes("\r\n"); // write the final newline to indicate the end of the request

    }


    /*
   Sends the given command to the server in the form of HTTP request.
   Only used for POST and PUT requests
    */
    public void sendPOSTPUTRequest () throws IOException
    {
    //TODO: implement this
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
    Reads the headers of the incoming http response
     */
    public void readHeaders() throws IOException{
        // initialize starting values
        this.encoding = "";
        this.buffer = new byte[0];
        this.bytesToRead = 0;
        String line; // initialize string where current line from the response is saved

        while (true) {
            line = readLine(bytesFromServer); // read input from server
            System.out.print(line);

            // if encoding hasn't been specified yet, check if this line is specifying it and indeed yes, change the encoding argument to the correct encoding
            if (this.encoding.equals("")) {
                if (line.contains("Transfer-Encoding: chunked")) {
                    this.encoding = "Transfer-Encoding";
                } else if (line.contains("Content-Length")) {
                    this.encoding = "Content-Length";
                    String bytes = line.split(" ")[1];
                    this.bytesToRead = Integer.parseInt(bytes.substring(0, bytes.length()-2)); //convert the string specifying number of bytes to integer, drop the last two chars which are \r\n
                    this.buffer = new byte[this.bytesToRead]; // immediately make the buffer array since the number of bytes is already specified

                }
            }

            if (line.contains("Content-Type")){
                if (line.contains("text/html")){
                    this.contentType = "html";
                }
                else if (line.contains("image")){
                    this.contentType = "img";
                }
            }


            if (line.equals("\r\n")) { // \r\n line indicated the end of headers and we break the loop
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

            if (this.contentType.equals("img")){ // if the body is an img, we have to use the BufferedOutputStream instead of a BufferedWriter
                ibw.write(buffer);
                ibw.flush();
            } else { // for html we use BufferedWriter
                line = new String(this.buffer); // and save it to the variable 'line'
                this.bw.write(line); // write the body to the file
                System.out.print(line); // and print it
            }

        } else { // contentType is transfer-encoding
            while (true) {
                // read a line from server and print is
                line = readLine(bytesFromServer);

                // break the loop on the last line which contains 0 and CL LF
                if(line.equals("0\r\n")){
                    line = readLine(bytesFromServer);
                    System.out.println(line);
                    break;
                }

                // between every chung is an empty line which we skip because the Integer.parseInt would throw an error on it and we don't really need it
                if (!line.equals("\r\n")) {
                    bytesToRead = Integer.parseInt(line.substring(0 , line.length()-2), 16); // convert the string specifying the number of bytes to read to integer
                    buffer = readBytes(bytesFromServer, bytesToRead); // read bytesToRead number of bytes from bytesFromServer
                    if(contentType.equals("img")){ // if the body is an img, use the ibw to write the bytes to file
                        ibw.write(buffer);
                        ibw.flush();
                    }
                    else { // otherwise convert the buffer to string and use bw to save it to a file
                        line = new String(buffer);
                        System.out.print(line);
                        bw.write(line);
                    }
                }
            }
        }
        bw.flush(); // flush the buffer at the end of the loop to effectively write to the file
    }


    /*
    Read the the given file and return the string with the content
     */
    public String readFile(String fileName) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(fileName))); // encapsulate the FileInputStream with a BufferedReader
        String strLine;
        String fileText = "";
        while ( (strLine = reader.readLine()) != null){ // while there is something to read, read new line from the file
            fileText += strLine;
        }
        return fileText; // return the string with content
    }

    /*
    Parse the html file, download all the images and change their src attribute in the html file to the local path of the downloaded image
     */
    public void getImgs() throws IOException{

        String websiteDir = Paths.get("websites").toAbsolutePath().normalize().toString(); // path to the website dir

        StringBuffer sb = new StringBuffer(); // string buffer which will contain the html file with the modified img src attributes

        String htmlFileText = readFile(htmlFileRelativePath);
        Matcher imgMatcher = imgPatternRegex.matcher(htmlFileText);  // create a matcher that will match the given URI against the pattern
        // apply the Matcher to the URI and extract the host and file path groups
        while(imgMatcher.find()){ // loop through all img tags in the html file
            path = imgMatcher.group(2); // extract the path to the img from the img tag
            path = String.valueOf(path.charAt(0)).equals("/") ? path : "/" + path;
            imgMatcher.appendReplacement(sb, "$1" + websiteDir + File.separator + htmlFilename + path + "$3"); // add the absolute path of the local website dir to the src attribute
            this.HTTPCommand = "GET";
            String imgFilePath = websiteDir + File.separator + htmlFilename + path; // absolute path of the image
            createFile(imgFilePath); // create the image file
            this.ibw = new BufferedOutputStream(new FileOutputStream(imgFilePath)); // create a BufferedOutputStream to the newly created img file

            sendGETHEADRequest(); // send the GET request to download the image
            readHeaders(); // read the response headers
            readBody(); // read the body, a.k.a. download the actual image bytes
        }
        imgMatcher.appendTail(sb); // add the remainder of the html file to the StringBuffer

        // overwrite the html file with the adapted one from the sb
        File file = new File(htmlFileRelativePath);
        FileWriter fr = new FileWriter(file);
        fr.write(sb.toString());
        fr.flush();
    }
}


