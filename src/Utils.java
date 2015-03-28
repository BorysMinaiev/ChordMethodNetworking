/**
 * Created by Borys Minaiev on 26.03.2015.
 */
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;

public class Utils {
    static byte[] getIpAddress() {
        try {
            System.out.println("Host addr: " + InetAddress.getLocalHost().getHostAddress());  // often returns "127.0.0.1"
//            return Inet4Address.getLocalHost().getAddress();
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            for (; n.hasMoreElements();)
            {
                NetworkInterface e = n.nextElement();
//                System.out.println("Interface: " + e.getName());
                Enumeration<InetAddress> a = e.getInetAddresses();
                for (; a.hasMoreElements();)
                {
                    InetAddress addr = a.nextElement();
                    System.out.println("  " + addr.getHostAddress() + " " + Arrays.toString(addr.getAddress()));
                    if (e.getName().equals("net6"))
                        if (addr instanceof Inet4Address) {
                            Inet4Address cur = (Inet4Address) addr;
                            return cur.getAddress();
                        }
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InetAddress readIPFromStream(InputStream stream) throws IOException {
        byte[] ip = new byte[4];
        for (int i = 0; i < ip.length; i++) {
            ip[i] = (byte) stream.read();
        }
        return InetAddress.getByAddress(ip);
    }

    public static InetAddress sendFindSuccessorRequest(InetAddress addr, int sha1) throws IOException {
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.FIND_SUCCESSOR);
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

    public static int intFromBytes(byte[] bytes) {
        int res = 0;
        for (int i = 0; i < bytes.length; i++) {
            int tmp = bytes[i];
            if (tmp < 0) {
                tmp += 256;
            }
            res = (res << 8) | tmp;
        }
        return res;
    }

    public static void sendNotify(InetAddress addr, Info info) throws IOException {
        if (Settings.DEBUG) {
            System.err.println("send notify to = " + ipToString(addr.getAddress()));
        }
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.NOTIFY);
        for (int i = 0; i < info.myIp.length; i++) {
            out.write(info.myIp[i]);
        }
        sendSocket.close();
    }


    public static void sendPredFail(InetAddress addr, Info info) throws IOException {
        if (Settings.DEBUG) {
            System.err.println("send notify to = " + ipToString(addr.getAddress()));
        }
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.PRED_FAILED);
        for (int i = 0; i < info.myIp.length; i++) {
            out.write(info.myIp[i]);
        }
        sendSocket.close();
    }


    // is value in [from; to) ?
    public static boolean insideInterval(int from, int to, int value) {
        if (from < to) {
            return value >= from && value < to;
        }
        return value >= from || value < to;
    }

    public static boolean sendDataToSomeone(InetAddress address, int sha1, byte[] ip) throws IOException {
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
        out.write(ip);
        out.flush();
        InputStream input = sendSocket.getInputStream();
        int res = input.read();
        sendSocket.close();
        if (res != Codes.OK) {
            return false;
        }
        return true;
    }

    static String ipToString(byte[] ip) {
        try {
            return InetAddress.getByAddress(ip).toString().split("/")[1] + ", sha1 = " + Utils.sha1(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    static int sha1(byte[] bytes) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            md.update(bytes);
            byte[] sha1hash = md.digest();
            final int COUNT_BYTES = 4;
            int res = 0;
            for (int i = 0; i < COUNT_BYTES; i++) {
                int tmp = sha1hash[sha1hash.length - COUNT_BYTES + i];
                if (tmp < 0) {
                    tmp += 1 << 8;
                }
                res = (res << 8) | tmp;
            }
            return res;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return 0;
    }

    int sha1(InetAddress addr) {
        return sha1(addr.getAddress());
    }

    static int sha1(String s) {
        try {
            return sha1(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int readInt(InputStream stream) throws IOException {
        int res = 0;
        for (int i = 0; i < 4; i++) {
            int tmp = stream.read();
            res = (res << 8) | tmp;
        }
        return res;
    }

    public static byte[] readIp(InputStream stream) throws  IOException {
        byte[] res = new byte[4];
        for (int i = 0; i < 4; i++) {
            res[i] = (byte) stream.read();
        }
        return res;
    }

    public static void writeInt(OutputStream output, int value) throws IOException {
        for (int i = 0; i < 4; i++) {
            int wr = (value >> ((3 - i) * 8)) & 255;
            output.write(wr);
        }
    }
}
