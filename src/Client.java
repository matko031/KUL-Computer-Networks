import java.util.*;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.*;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


class Client  {


    private static String imgPatternString = "(<img.+?src\\s*?=\\s*?\")(.+?)(\")"; // regex to find all the img tags in the html file
    private static Pattern imgPatternRegex = Pattern.compile(imgPatternString); // compile regex

    private DataOutputStream outToServer; // used to write to server
    private BufferedInputStream bytesFromServer; // used to receive info from the server
    private Socket clientSocket; // used to connect with the server





    void Client() {

    }

    /************************** UTILITIES **************************/


    /*
        Read the the given file and return the string with the content
         */
    String readFile(String fileName) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(fileName))); // encapsulate the FileInputStream with a BufferedReader
        String strLine;
        String fileText = "";
        while ( (strLine = reader.readLine()) != null){ // while there is something to read, read new line from the file
            fileText += strLine + "\r\n";
        }
        return fileText; // return the string with content
    }


    void writeFile(String filename, String text) throws IOException {
        // overwrite the html file with the adapted one from the sb
        File file = new File(filename);
        FileWriter fr = new FileWriter(file);
        fr.write(text);
        fr.flush();
    }


    /************************** REAL STUFF **************************/



    HttpRequest newRequest() throws IOException{
        return new HttpRequest();
    }

    /*
    Creates a socket object and connects with the host on the given port, both saved in the attributes of this object
     */
    void connectSocket(HttpRequest request) throws IOException {
        if ( clientSocket == null || !clientSocket.getInetAddress().getHostName().equals(request.getHost())) {
            clientSocket = new Socket(request.getHost(), Integer.parseInt(request.getPort())); // open a socket with the host
            outToServer = new DataOutputStream(clientSocket.getOutputStream()); // initialize output stream object
            bytesFromServer = new BufferedInputStream(clientSocket.getInputStream()); // initialize input stream object
        }
    }


    /*
   Sends the given command to the server in the form of HTTP request.
   Only used for POST and PUT requests
    */
    void sendRequest(HttpRequest request) throws IOException {
        outToServer.writeBytes(request.constructRequest());
    }

    void handleResponse(HttpRequest request) throws IOException {
        HttpResponse response = new HttpResponse(request, bytesFromServer);
        response.parseHeaders();
        response.readBody();
        response.saveResponse();
        if (response.getContentType().equals("text")) {
            System.out.println(new String(response.getBody()));
        }
    }



    /*
    Parse the html file, download all the images and change their src attribute in the html file to the local path of the downloaded image
     */
   public void getImgs(String filepath, String host) throws IOException{

        String currentDir = Paths.get(".").toAbsolutePath().normalize().toString(); // path to the website dir
        StringBuffer sb = new StringBuffer(); // string buffer which will contain the html file with the modified img src attributes
        String htmlFileText = readFile(filepath);
        Matcher imgMatcher = imgPatternRegex.matcher(htmlFileText);  // create a matcher that will match the given URI against the pattern

        // apply the Matcher to the URI and extract the host and file path groups
       String path;
       ArrayList<HttpRequest> requests = new ArrayList<HttpRequest>();
        while(imgMatcher.find()){ // loop through all img tags in the html file
            path = imgMatcher.group(2); // extract the path to the img from the img tag
            path = String.valueOf(path.charAt(0)).equals("/") ? path : "/" + path;
            String cleanPath = path.replaceAll("%20", ""); // cleanPath is the path without spaces

            HttpRequest req = new HttpRequest(host, "GET", path);
            sendRequest(req);
            requests.add(req);
            imgMatcher.appendReplacement(sb, "$1" + currentDir + File.separator + "websites" + File.separator + req.getHostDir() + File.separator + cleanPath + "$3"); // add the absolute path of the local website dir to the src attribute
        }

        for (HttpRequest req : requests){
            handleResponse(req);
        }

        imgMatcher.appendTail(sb); // add the remainder of the html file to the StringBufferabsoluteWebsiteDir

        // overwrite the html file with the adapted one from the sb
        writeFile(filepath, sb.toString());
    }

}


