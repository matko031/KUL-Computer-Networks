import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpResponse {

    HttpRequest request;

    BufferedInputStream bytesFromServer;

    HashMap<String, String> headers;

    byte[] body;

    private String encoding;
    private int bytesToRead;
    private int responseCode;
    private String contentType;
    private String fileFormat;
    private String websiteDir = "websites";

    HttpResponse(HttpRequest req, BufferedInputStream in){
        bytesFromServer = in;
        request = req;
    }


/******************************** GETTERS ********************************/
    HashMap<String, String> getHeaders(){
        return headers;
    }

    String getEncoding(){
        return encoding;
    }

    int getSentBytes(){
        return bytesToRead;
    }

    int getResponseCode(){
        return responseCode;
    }

    String getContentType(){
        return contentType;
    }

    byte[] getBody(){
        return body;
    }
/******************************** HELPERS ********************************/


    /*
    Reads one line from the buffered reader. Implemented in spite of readline() method already existing
    due to the possible bugs and inconsistencies when using a combination of read() and readline() with BufferedReader
     */
    String readLine(BufferedInputStream inFromServer) throws IOException{
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
    byte[] readBytes(BufferedInputStream in, int nbBytes) throws IOException {
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
 Creates a file at the given location together with all the parent dirs needed
  */
    void createFile(String filePath) throws IOException{
        File f = new File(filePath); // creates a file object with the given path
        if (f.getParentFile() != null) { // checks if there is parent folder given in the filePath, e.g., 'foo/bar.png' would return true and 'bar.png' would not
            f.getParentFile().mkdirs(); // creates the parent dirs
        }
        boolean success = f.createNewFile(); // creates the file at the filePath location and returns whether or not it succeeded in doing so
    }


    /*
        Read the the given file and return the string with the content
         */
    String readFile(String fileName) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(fileName))); // encapsulate the FileInputStream with a BufferedReader
        String strLine;
        StringBuilder fileText = new StringBuilder();
        while ( (strLine = reader.readLine()) != null){ // while there is something to read, read new line from the file
            fileText.append(strLine).append("\r\n");
        }
        return fileText.toString(); // return the string with content
    }


    void writeFile(String filename, String text) throws IOException {
        // overwrite the html file with the adapted one from the sb
        File file = new File(filename);
        FileWriter fr = new FileWriter(file);
        fr.write(text);
        fr.flush();
    }



    /*
       Reads the headers of the incoming http response
        */
    void readHeaders() throws IOException {
        String line;
        HashMap<String, String> headers = new HashMap<String, String>();
        // \r\n line indicated the end of headers and we break the loop
        do {
            line = readLine(bytesFromServer); // read input from server
            if (line.contains(":")) {
                String[] splitLine = line.split(":");
                headers.put(splitLine[0], splitLine[1]);
            } else if (!line.equals("\r\n")) {
                headers.put("response-code", line);
            }
            System.out.print(line);
        } while (!line.equals("\r\n"));
        this.headers = headers;
    }

    void setEncoding(){
        if (headers.get("Transfer-Encoding") != null) {
            encoding = "Transfer-Encoding";
        } else if (headers.get("Content-Length") != null) {
            encoding = "Content-Length";
            String bytes = headers.get("Content-Length").replace(" ", "").replace("\r\n", "");
            bytesToRead = Integer.parseInt(bytes); //convert the string specifying number of bytes to integer
        }
    }

    void setContentType(){
        if (headers.get("Content-Type") != null) {
            String[] content = headers.get("Content-Type").split("/");
            String contentType = content[0].replace(" ", "");
            int endIndex = content[1].contains(";") ? content[1].indexOf(";") : content[1].indexOf("\r");
            fileFormat = content[1].substring(0, endIndex);
            this.contentType = contentType;
        }
    }


    String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.UK);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }


    /******************************** REAL STUFF ********************************/

    void parseHeaders() throws IOException{
        readHeaders();
        responseCode = Integer.parseInt(headers.get("response-code").split(" ")[1]);
        setEncoding();
        setContentType();
    }

    /*
    Reads the body of the incoming HTTP response
     */
    void readBody() throws IOException{
        String line;
        if (this.encoding.equals("Content-Length")) {
            body = readBytes(this.bytesFromServer, this.bytesToRead); // if encoding is of content-length type, read the correct amount of bytes in the buffer

        } else {
            ByteArrayOutputStream tempBytes = new ByteArrayOutputStream( );
            do {
                line = readLine(bytesFromServer);
                // between every chung is an empty line which we skip because the Integer.parseInt would throw an error on it and we don't really need it
                if (!line.equals("\r\n")) {
                    bytesToRead = Integer.parseInt(line.replace("\r\n", ""), 16); // convert the string specifying the number of bytes to read to integer
                    tempBytes.write(readBytes(bytesFromServer, bytesToRead)); // read bytesToRead number of bytes from bytesFromServer
                }
            } while (! line.equals("0\r\n"));
            readLine(bytesFromServer);
            body = tempBytes.toByteArray();
        }
    }

    void saveResponse() throws IOException{
        String cleanPath = request.getPath().replace("%20", "");
        String fullPath = request.getPath().contains(".") ? websiteDir + File.separator + request.getHostDir() + cleanPath : websiteDir + File.separator + request.getHostDir() + File.separator + "file." + fileFormat;
        createFile(fullPath);

        if (contentType.equals("text") && request.getLang() != null){
            body = translateBody().getBytes();
        }

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fullPath));
        out.write(body);
        out.flush();
        out.close();
        updateLastModified();
        }

    void updateLastModified() throws IOException {
        String filePath = "last-modified/" + request.getHost();
        createFile(filePath);
        String fileContent = readFile(filePath);
        String regex = request.getPath()+";(.*?)\r\n";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(fileContent);
        String newText;
        if (matcher.find()){
            StringBuilder sb = new StringBuilder();
            matcher.appendReplacement(sb, request.getPath() + ";" + getTime() + "\n");
            matcher.appendTail(sb);
            newText = sb.toString();
        }
        else{
            newText = fileContent + request.getPath() + ";" + getTime() + "\n";
        }
        writeFile(filePath, newText);

    }


    HttpURLConnection setupHttpURLConnection(String API_KEY, String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        return con;
    }




     String translateBody() throws IOException {
        System.out.println("Start translation");
        String htmlText = new String(body);
        Document doc = Jsoup.parse(htmlText);

        ArrayList<String> elements= new ArrayList<String>();
        for (Element child : doc.body().getAllElements()) {
            if (!child.ownText().equals("")) {
                elements.add(child.ownText());
            }
        }

        String API_KEY = "trnsl.1.1.20200314T182311Z.d201cdaa9f1c8b73.d7dd33ae0a61a84c693f2b3911de8d09fd5da66e";
        String url = "https://translate.yandex.net/api/v1.5/tr.json/translate?lang=" + request.getLang() + "&key=" + API_KEY;
        HttpURLConnection con = setupHttpURLConnection(API_KEY, url);
        OutputStream os = con.getOutputStream();

        for (String e : elements ) {
            String textToTranslate = "text=" + e + "&";
            os.write((textToTranslate).getBytes());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        con.disconnect();

        String translation = response.toString();

        JSONObject json = new JSONObject(translation);
        JSONArray text = json.getJSONArray("text");

        int index = 0;
        for (Element child : doc.body().getAllElements()) {
            if (!child.ownText().equals("")) {
                child.text((String)text.get(index));
                index++;
            }
        }
        System.out.println("end translation");

        return doc.toString();
     }


}
