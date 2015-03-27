/**
 * Created by Borys Minaiev on 26.03.2015.
 */
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
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
            try {
                waitForMessages(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPickUpPacket(InetAddress addr) throws IOException {
//        System.err.println("send pick up to " + Utils.ipToString(addr.getAddress()));
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.PICK_UP);
        out.write(Arrays.equals(addr.getAddress(), info.succ) ? info.myIp : info.succ);
        sendSocket.close();
    }

    private void gotInitMessageFromSomebody(byte[] message) throws IOException {
        byte[] ip = new byte[4];
        for (int i = 0; i < 4; i++) {
            ip[i] = message[i + 1];
        }
        int sha1  = Utils.sha1(ip);
        if (sha1 == Utils.sha1(info.myIp)) {
            return;
        }
        System.err.println("inits.. from " + Utils.ipToString(ip));
        if (Utils.insideInterval(Utils.sha1(info.myIp), Utils.sha1(info.succ), sha1)) {
            if (Settings.DEBUG) {
                System.err.println(" I ' M GOING TO SEND PICK UP TO " + Utils.ipToString(ip));
            }
            sendPickUpPacket(InetAddress.getByAddress(ip));
        }
    }

    private void gotKeepAliveFromSomebody(byte[] message) {
        info.lastTimeReceivedKeepAlive = System.currentTimeMillis();
//        System.err.println("got keep alive from somebody");
    }

    private void decodeMessage(byte[] message) throws IOException {
        if (message[0] == Codes.INIT) {
            gotInitMessageFromSomebody(message);
            return;
        }
        if (message[0] == Codes.KEEP_ALIVE) {
            gotKeepAliveFromSomebody(message);
            return;
        }
        System.err.println("Somebody send message which doesn't start with not 0x7 and not 0xF");

//        info.succ = new byte[4];
//        for (int i = 0; i < info.succ.length; i++) {
//            info.succ[i] = message[i + 1];
//        }
//        System.err.println("succ ip = " + Utils.ipToString(info.succ));
    }

    private void waitForMessages(DatagramSocket socket) throws IOException {
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
