import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;
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
            System.err.println("find pos for hash = " + hash);
            InetAddress address = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.myIp), hash);
            System.err.println("ip locatins in " + address);
            InetAddress position = sendGetIPRequest(address, hash);
            System.err.println("real data locates in " + position);
            if (position == null) {
                return null;
            }
            System.err.println(Utils.ipToString(position.getAddress()));
            String value = sendGetDataRequest(position, hash);
            return value;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    private static boolean putData(int sha1, String value) {
        info.map.put(sha1, value);
        try {
            InetAddress address = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.myIp), sha1);
            return Utils.sendDataToSomeone(address, sha1, info.myIp);
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
                if (cmd.startsWith("a")) {
                    String entry = scanner.next();
                    String value = scanner.next();
                    String currentValue = getValueForEntry(entry);
                    if (currentValue == null) {
                        System.out.println("put " + (putData(Utils.sha1(entry), value) ? " ok" : " not ok"));
                    } else {
                        System.out.println("failed to put, already exist, sorry.");
                    }
                } else {
                    if (cmd.startsWith("g")) {
                        String entry = scanner.next();
                        String value = getValueForEntry(entry);
                        if (value == null) {
                            System.out.println("failed to load data, it doesn't exist.");
                        } else {
                            System.out.println("value = " + value);
                        }
                    } else {
                        if (cmd.startsWith("e")) {
                            exit();
                        } else {
                            System.out.println("COMMAND NOT FOUND");
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static void exit() {
        for (Map.Entry<Integer, byte[]> entry : info.whereMap.entrySet()) {
            if (!Arrays.equals(info.myIp, info.succ)) {
                try {
                    Utils.sendDataToSomeone(InetAddress.getByAddress(info.succ), entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    e.printStackTrace();
                };
            }
         }
        System.exit(0);
    }
}
