import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * Created by Borys Minaiev on 26.03.2015.
 */
public class Main {
    public static void main(String[] args) {
        final byte[] myAddress = Utils.getIpAddress();
        if (Settings.DEBUG) {
            System.out.println("my ip = " + Utils.ipToString(myAddress));
        }
        final Info info = new Info();
        try {
            final DatagramSocket socket = new DatagramSocket(Settings.PORT);
            new Thread(new UdpReceiver(socket, info)).start();
            new Thread(new UdpSender(socket, info)).start();
            new Thread(new TcpReceiver(info)).start();
            new Thread(new KeepAliveSender(socket, info)).start();
            new Thread(new KeepAliveChecker(info)).start();
            new Thread(new Stabilizer(info)).start();
            new Thread(new FingerStabilizer(info)).start();
            new Thread(new MyLogger(info)).start();
            Scanner scanner = new Scanner(System.in);
            System.out.println("add_entry ENTRY_NAME ENTRY_VALUE");
            System.out.println("get_entry ENTRY_NAME");
            while (true) {
                String cmd = scanner.next();
                if (cmd.equals("add_entry")) {
                    String entry = scanner.next();
                    String value = scanner.next();

                } else {
                    if (cmd.equals("get_entry")) {
                        String entry = scanner.next();
                    } else {
                        System.out.println("COMMAND NOT FOUND");
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
