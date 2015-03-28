import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by Borys Minaiev on 27.03.2015.
 */
public class Stabilizer implements Runnable {
    Info info;

    Stabilizer(Info info) {
        this.info = info;
    }

    private InetAddress sendPrevRequest(InetAddress addr) throws IOException {
        if (Settings.DEBUG) {
            System.err.println("ask prev for ip = " + Utils.ipToString(addr.getAddress()));
        }
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.GET_PREDECESSOR);
        out.flush();
        InputStream input = sendSocket.getInputStream();
        int res = input.read();
        if (res != Codes.OK) {
            if (Settings.DEBUG) {
                System.err.println("failed to get prev");
            }
            return null;
        }
        InetAddress result = Utils.readIPFromStream(input);
        sendSocket.close();
        if (Settings.DEBUG) {
            System.err.println("got prev = " + Utils.ipToString(result.getAddress()));
        }
        return result;
    }

    @Override
    public void run() {
        while (true) {

            if (!Arrays.equals(info.fingerTable[0].getAddress(), info.myIp)) {
                InetAddress x = null;
                try {
                    if (Settings.DEBUG) {
                        System.err.println("try to send prev req " + info.fingerTable[0]);
                    }
                    try {
                        x = sendPrevRequest(info.fingerTable[0]);
                    } catch (SocketException e) {
                        if (Settings.DEBUG) {
                            System.err.print("failed to connect, try to connect to succ2");
                        }
                        info.succ = info.succ2;
                        InetAddress tmp =Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.succ), Utils.sha1(info.succ));
                        info.succ2 = tmp == null ? null : tmp.getAddress();
                        if (info.succ2 == null) {
                            info.succ2 = info.myIp;
                        }
                    }
                    if (Utils.insideInterval(Utils.sha1(info.myIp), Utils.sha1(info.succ), Utils.sha1(x.getAddress())) && !Arrays.equals(info.myIp, x.getAddress())) {
                        info.fingerTable[0] = x;
                        info.succ = x.getAddress();
                        try {
                            Utils.sendNotify(InetAddress.getByAddress(info.succ), info);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        info.succ2 = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.succ), Utils.sha1(info.succ)).getAddress();
                        if (info.succ2 == null) {
                            info.succ2 = info.myIp;
                        }
                        Utils.sendNotify(InetAddress.getByAddress(info.succ), info);
                    }
                } catch (IOException e) {
//                    e.printStackTrace();
                } catch (NullPointerException e) {

                }
            } else {
                info.succ = info.prev;
                try {
                    info.fingerTable[0] = InetAddress.getByAddress(info.succ);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }

            if (!Arrays.equals(info.myIp, info.succ)) {
                try {
                    info.succ2 = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.succ), Utils.sha1(info.succ)).getAddress();
                } catch (Exception e) {
//                    e.printStackTrace();
                }
                if (info.succ2 == null) {
                    info.succ2 = info.myIp;
                }
            }

            try {
                Utils.sendNotify(InetAddress.getByAddress(info.succ), info);
            } catch (Exception e) {
//                e.printStackTrace();
            }
            try {
                Thread.sleep(Settings.SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
