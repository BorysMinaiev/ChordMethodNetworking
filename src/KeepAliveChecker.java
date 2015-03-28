import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Borys Minaiev on 27.03.2015.
 */
public class KeepAliveChecker implements Runnable {
    Info info;

    KeepAliveChecker(Info info) {
        this.info = info;
    }

    @Override
    public void run() {
        while (true) {
            if (System.currentTimeMillis() - info.lastTimeReceivedKeepAlive > Settings.KEEP_ALIVE_WAIT) {
//                if (Settings.DEBUG) {
                    System.err.println("SOMEBODY FAILED TO RESPOND!!! " + Utils.ipToString(info.succ));
//                }
                info.succ = info.succ2;
                try {
                    info.succ2 = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.succ), Utils.sha1(info.succ)).getAddress();
                } catch (Exception e) {
//                    e.printStackTrace();
                }
                if (info.succ2 == null) {
                    info.succ2 = info.myIp;
                }
                try {
                    Utils.sendPredFail(InetAddress.getByAddress(info.succ), info);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(Settings.SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
