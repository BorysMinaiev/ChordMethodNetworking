import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * Created by Borys Minaiev on 26.03.2015.
 */
public class Main {
    static Info info;

    public static InetAddress sendGetIPRequest(InetAddress addr, int sha1) throws IOException {
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.GET_IP);
        for (int i = 0; i < 4; i++) {
            int what = (sha1 >> ((3 - i) * 8)) & 255;
            out.write(what);
        }
        out.flush();
        InputStream input = sendSocket.getInputStream();
        int res = input.read();
        if (res != Codes.OK) {
            return null;
        }
        InetAddress result = Utils.readIPFromStream(input);
        sendSocket.close();
        return result;
    }

    private static int readInt(InputStream stream) throws IOException {
        int res = 0;
        for (int i = 0; i < 4; i++) {
            int tmp = stream.read();
            res = (res << 8) | tmp;
        }
        return res;
    }

    private static String sendGetDataRequest(InetAddress addr, int sha1) throws IOException {
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.GET_DATA);
        for (int i = 0; i < 4; i++) {
            int what = (sha1 >> ((3 - i) * 8)) & 255;
            out.write(what);
        }
        out.flush();
        InputStream input = sendSocket.getInputStream();
        int res = input.read();
        if (res != Codes.OK) {
            return null;
        }
        int length = readInt(input);
        String result = "";
        for (int i = 0; i < length; i++) {
            result = result + new String(new char[]{(char)input.read()});
        }
        sendSocket.close();
        return result;
    }

    private static String getValueForEntry(String entry) {
        int hash = Utils.sha1(entry);
        try {
            InetAddress address = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.myIp), hash);
            InetAddress position = sendGetIPRequest(address, hash);
            String value = sendGetDataRequest(position, hash);
            return value;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean sendDataToSomeone(InetAddress address, int sha1) throws IOException {
        if (address == null) {
            return false;
        }
        Socket sendSocket = new Socket(address, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.ADD_ENTRY);
        for (int i = 0; i < 4; i++) {
            int what = (sha1 >> ((3 - i) * 8)) & 255;
            out.write(what);
        }
        out.write(info.myIp);
        out.flush();
        InputStream input = sendSocket.getInputStream();
        int res = input.read();
        sendSocket.close();
        if (res != Codes.OK) {
            return false;
        }
        return true;
    }

    private static boolean putData(String entry, String value) {
        info.map.put(entry, value);
        int hash = Utils.sha1(entry);
        try {
            InetAddress address = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.myIp), hash);
            return sendDataToSomeone(address, hash);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        final byte[] myAddress = Utils.getIpAddress();
        if (Settings.DEBUG) {
            System.out.println("my ip = " + Utils.ipToString(myAddress));
        }
        info = new Info();
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
                    String currentValue = getValueForEntry(entry);
                    if (currentValue == null) {
                        System.out.println("put " + (putData(entry, value) ? " ok" : " not ok"));
                    } else {
                        System.out.println("failed to put, already exist, sorry.");
                    }
                } else {
                    if (cmd.equals("get_entry")) {
                        String entry = scanner.next();
                        String value = getValueForEntry(entry);
                        if (value == null) {
                            System.out.println("failed to load data, it doesn't exist.");
                        } else {
                            System.out.println("value = " + value);
                        }
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
