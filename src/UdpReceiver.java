/**
 * Created by Borys Minaiev on 26.03.2015.
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class UdpReceiver implements Runnable {
    DatagramSocket socket;
    Info info;

    UdpReceiver(DatagramSocket socket, Info info) {
        this.socket = socket;
        this.info = info;
    }

    @Override
    public void run() {
        while (true) {
            waitForMessages(socket);
        }
    }

    private static int readInt(final byte[] arr, int from) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            int z = arr[i + from] & ((1 << 8) - 1);
            result = (result << 8) | z;
        }
        return result;
    }

    private static long readLong(final byte[] arr, int from) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            int z = arr[i + from] & ((1 << 8) - 1);
            result = (result << 8) | z;
        }
        return result;
    }

    private static String readStirng(final byte[] arr, int from) {
        String result = "";
        for (int i = 0; i + from < arr.length; i++) {
            int x = arr[i + from];
            if (x == 0) {
                break;
            }
            result += (char) x;
        }
        return result;
    }

    private void decodeMessage(byte[] message) {
        if (message[0] != Codes.INIT) {
            System.err.println("Somebody send message which doesn't start with not 0x7");
            return;
        }
        System.err.println("got init from somebody");
        byte[] ip = new byte[4];
        for (int i = 0; i < 4; i++) {
            ip[i] = message[i + 1];
        }
        System.err.println("ip = " + Utils.ipToString(ip));
//        info.succ = new byte[4];
//        for (int i = 0; i < info.succ.length; i++) {
//            info.succ[i] = message[i + 1];
//        }
//        System.err.println("succ ip = " + Utils.ipToString(info.succ));
    }

    private void waitForMessages(DatagramSocket socket) {
        final int bufLength = 1 << 10;
        byte[] buf = new byte[bufLength];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println("fail ");
        }
        decodeMessage(packet.getData());
    }

}
