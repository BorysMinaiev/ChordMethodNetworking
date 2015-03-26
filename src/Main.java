import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Borys Minaiev on 26.03.2015.
 */
public class Main {
    public static void main(String[] args) {
        final byte[] myAddress = Utils.getIpAddress();
        System.out.println("my ip = " + Utils.ipToString(myAddress));
        final Info info = new Info();
        try {
            final DatagramSocket socket = new DatagramSocket(Settings.PORT);
            new Thread(new UdpReceiver(socket, info)).start();
            new Thread(new UdpSender(socket, info)).start();
            new Thread(new TcpReceiver(info)).start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
