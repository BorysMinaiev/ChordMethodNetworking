import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by Borys Minaiev on 26.03.2015.
 */
public class Info {
    byte[] succ;
    byte[] succ2;
    byte[] prev;
    byte[] myIp;
    InetAddress[] fingerTable;
    int[] start;
    long lastTimeReceivedKeepAlive;

    Info() {
        succ = succ2 = prev = myIp = Settings.myIP;
        fingerTable = new InetAddress[32];
        try {
            Arrays.fill(fingerTable, InetAddress.getByAddress(myIp));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        start = new int[32];
        lastTimeReceivedKeepAlive = System.currentTimeMillis();
        int mySha1 = Utils.sha1(myIp) - 1;
        for (int i = 0; i < start.length; i++) {
            start[i] = mySha1 + (1 << i);
        }
    }
}
