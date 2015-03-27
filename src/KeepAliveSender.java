import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by Borys Minaiev on 27.03.2015.
 */
public class KeepAliveSender implements Runnable {
    Info info;
    DatagramSocket socket;

    KeepAliveSender(DatagramSocket socket, Info info) {
        this.info = info;
        this.socket = socket;
    }

    @Override
    public void run() {
        while (true) {
            DatagramPacket packet = null;
            byte[] myMessage = new byte[]{Codes.KEEP_ALIVE};
            try {
                packet = new DatagramPacket(myMessage, myMessage.length,
                        InetAddress.getByAddress(info.prev), Settings.PORT);
            } catch (UnknownHostException e2) {
                e2.printStackTrace();
            }
            try {
                if (Settings.DEBUG) {
                    System.err.println("send keep alive");
                }
                socket.send(packet);
            } catch (IOException e1) {

            }
            try {
                Thread.sleep(Settings.SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
