/**
 * Created by Borys Minaiev on 26.03.2015.
 */
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

public class Utils {
    static byte[] getIpAddress() {
        try {
//            System.out.println("Host addr: " + InetAddress.getLocalHost().getHostAddress());  // often returns "127.0.0.1"
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            for (; n.hasMoreElements();)
            {
                NetworkInterface e = n.nextElement();
//                System.out.println("Interface: " + e.getName());
                Enumeration<InetAddress> a = e.getInetAddresses();
                for (; a.hasMoreElements();)
                {
                    InetAddress addr = a.nextElement();
//                    System.out.println("  " + addr.getHostAddress() + " " + Arrays.toString(addr.getAddress()));
                    if (e.getName().equals("net6"))
                        if (addr instanceof Inet4Address) {
                            Inet4Address cur = (Inet4Address) addr;
                            return cur.getAddress();
                        }
                }
            }
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
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
//        System.err.println("ask succ for sha1 = " + sha1);
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.FIND_SUCCESSOR);
        for (int i = 0; i < 4; i++) {
            int what = (sha1 >> ((3 - i) * 8)) & 255;
            out.write(what);
        }
        InputStream input = sendSocket.getInputStream();
        int res = input.read();
        if (res != Codes.OK) {
//            System.err.println("failed to get succ");
            return null;
        }
        InetAddress result = Utils.readIPFromStream(input);
        sendSocket.close();
//        System.err.println("send result -> " + Utils.ipToString(result.getAddress()));
        return result;
    }

    public static void sendNotify(InetAddress addr, Info info) throws IOException {
//        System.err.println("send notify to = " + ipToString(addr.getAddress()));
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.NOTIFY);
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

    static String ipToString(byte[] ip) {
        try {
            return InetAddress.getByAddress(ip).toString().split("/")[1];
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

    int sha1(String s) {
        try {
            return sha1(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public interface PacketGenerator {
        void gen(DataOutputStream dos) throws IOException;
    }

    static DatagramPacket genPacket(PacketGenerator pg, InetAddress addr, int port) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        pg.gen(dos);
        byte[] bytes = bos.toByteArray();
        DatagramPacket packet = new DatagramPacket(bytes,
                bytes.length, addr, port);
        return packet;
    }
}
