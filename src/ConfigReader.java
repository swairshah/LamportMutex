import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by swair on 6/17/14.
 */
public class ConfigReader {
    public static HashMap<String,String> getLookup(String f) {
        HashMap<String,String> lookup = new HashMap<String,String>();
        try(BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line = null;
            while((line = r.readLine()) != null) {
                String[] parts = line.split(" ");
                String pid = parts[0];
                String ip_port = parts[1]+":"+parts[2];
                lookup.put(pid,ip_port);
            }
        } catch(IOException ex) {
            System.err.println("can't open file");
        }
        return lookup;
    }

    public static void main(String[] args) {
        //ConfigReader.getLookup("/tmp/config");
    }
}
