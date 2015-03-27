import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Borys Minaiev on 28.03.2015.
 */
public class MyLogger implements Runnable {
    Info info;
    PrintWriter writer;

    MyLogger(Info info) {
        this.info = info;
    }

    @Override
    public void run() {
        while (true) {
            try {
                writer = new PrintWriter("log.log");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            writer.println("update time = " + new Date().toString());
            writer.println("CURRENT INFO:");
            writer.println("myIp = " + Utils.ipToString(info.myIp));
            writer.println("succ = " + Utils.ipToString(info.succ));
            writer.println("succ2 = " + Utils.ipToString(info.succ2));
            writer.println("prev = " + Utils.ipToString(info.prev));

            writer.close();
            try {
                Thread.sleep(Settings.SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
