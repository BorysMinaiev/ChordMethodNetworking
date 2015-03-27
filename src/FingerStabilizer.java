import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by Borys Minaiev on 27.03.2015.
 */
public class FingerStabilizer implements Runnable {
    Info info;

    FingerStabilizer(Info info) {
        this.info = info;
    }

    @Override
    public void run() {
        final Random rnd = new Random();
        while (true) {
            int pos = rnd.nextInt(32);
            if (!Arrays.equals(info.myIp, info.succ)) {
                try {
                    info.fingerTable[pos] = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.succ),
                            info.start[pos]);
                } catch (IOException e) {
                    e.printStackTrace();
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
