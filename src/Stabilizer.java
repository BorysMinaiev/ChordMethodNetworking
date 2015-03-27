import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by Borys Minaiev on 27.03.2015.
 */
public class Stabilizer implements Runnable {
    Info info;

    Stabilizer(Info info) {
        this.info = info;
    }

    private InetAddress sendPrevRequest(InetAddress addr) throws IOException {
        System.err.println("ask prev for ip = " + Utils.ipToString(addr.getAddress()));
        Socket sendSocket = new Socket(addr, Settings.PORT);
        OutputStream out = sendSocket.getOutputStream();
        out.write(Codes.GET_PREDECESSOR);
        out.flush();
        InputStream input = sendSocket.getInputStream();
        int res = input.read();
        if (res != Codes.OK) {
            System.err.println("failed to get prev");
            return null;
        }
        InetAddress result = Utils.readIPFromStream(input);
        sendSocket.close();
        System.err.println("got prev = " + Utils.ipToString(result.getAddress()));
        return result;
    }

    @Override
    public void run() {
        while (true) {
            System.err.println("CURRENT INFO:");
            System.err.println("myIp = " + Utils.ipToString(info.myIp));
            System.err.println("succ = " + Utils.ipToString(info.succ));
            System.err.println("succ2 = " + Utils.ipToString(info.succ2));
            System.err.println("prev = " + Utils.ipToString(info.prev));


            if (!Arrays.equals(info.fingerTable[0].getAddress(), info.myIp)) {
                InetAddress x = null;
                try {
                    x = sendPrevRequest(info.fingerTable[0]);
                    if (Utils.insideInterval(Utils.sha1(info.myIp), Utils.sha1(info.succ), Utils.sha1(x.getAddress()))) {
                        info.fingerTable[0] = x;
                        info.succ = x.getAddress();
                        info.succ2 = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.succ), Utils.sha1(info.succ)).getAddress();
                        Utils.sendNotify(InetAddress.getByAddress(info.succ), info);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Utils.sendNotify(InetAddress.getByAddress(info.succ), info);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(Settings.SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
