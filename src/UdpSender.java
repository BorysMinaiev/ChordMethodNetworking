import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * Created by Borys Minaiev on 26.03.2015.
 */
public class UdpSender implements Runnable {
    Info info;
    DatagramSocket socket;

    UdpSender(DatagramSocket socket, Info info) {
        this.info = info;
        this.socket = socket;
    }

    private byte[] createMessage() {
        byte[] result = new byte[5];
        result[0] = Codes.INIT;
        for (int i = 0; i < info.myIp.length; i++) {
            result[1 + i] = info.myIp[i];
        }
        return result;
    }


    @Override
    public void run() {
        final byte[] myMessage = createMessage();
        while (true) {
            DatagramPacket packet = null;
            try {
                packet = new DatagramPacket(myMessage, myMessage.length,
                        InetAddress.getByName("255.255.255.255"), Settings.PORT);
            } catch (UnknownHostException e2) {
                e2.printStackTrace();
            }
            try {
                if (Arrays.equals(info.succ, info.myIp) && Arrays.equals(info.prev, info.myIp)) {
                    System.err.println("send init");
                    socket.send(packet);
                }
            } catch (IOException e1) {
                System.out.println("error sending a message");
            }
            try {
                Thread.sleep(Settings.SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
