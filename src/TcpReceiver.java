import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by Borys Minaiev on 26.03.2015.
 */
public class TcpReceiver implements Runnable {
    Info info;

    TcpReceiver(Info info) {
        this.info = info;
    }

    class EventHandler implements Runnable {
        Socket socket;

        public EventHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();
                int commandCode = input.read();
                switch (commandCode) {
                    case Codes.PICK_UP:
                        gotPickUp(input);
                        break;
                    case Codes.NOTIFY:
                        gotNotify(input);
                        break;
                    case Codes.FIND_SUCCESSOR:
                        findSuccessor(input, output);
                        break;
                    case Codes.GET_PREDECESSOR:
                        getPredecessor(output);
                        break;

                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void getPredecessor(OutputStream output) throws IOException {
            output.write(Codes.OK);
            output.write(info.prev);
        }


        private void findSuccessor(InputStream input, OutputStream output) throws IOException {
            int sha1 = Utils.sha1(Utils.readIPFromStream(input).getAddress());
            if (Utils.insideInterval(Utils.sha1(info.prev), Utils.sha1(info.myIp), sha1)) {
                output.write(Codes.OK);
                output.write(info.myIp);
                return;
            }
            for (int i = info.fingerTable.length - 1; i >= 0; i--) {
                if (Utils.insideInterval(Utils.sha1(info.myIp), sha1, Utils.sha1(info.fingerTable[i].getAddress()))) {
                    InetAddress res = Utils.sendFindSuccessorRequest(info.fingerTable[i], sha1);
                    if (res == null) {
                        output.write(Codes.FAIL);
                        return;
                    }
                    output.write(Codes.OK);
                    output.write(res.getAddress());
                    return;
                }
            }
            InetAddress res = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.succ), sha1);
            if (res == null) {
                output.write(Codes.FAIL);
                return;
            }
            output.write(Codes.OK);
            output.write(res.getAddress());
            return;
        }

        private void gotPickUp(InputStream stream) throws IOException {
//            System.err.println("GOT PICK UP MESSAGE");
            InetAddress askAddress = Utils.readIPFromStream(stream);
            System.err.println("from ip = " + Utils.ipToString(askAddress.getAddress()));
            info.succ = Utils.sendFindSuccessorRequest(askAddress, Utils.sha1(info.myIp)).getAddress();
            info.succ2 = Utils.sendFindSuccessorRequest(askAddress, Utils.sha1(info.succ)).getAddress();
            if (Arrays.equals(info.succ, info.succ2)) {
                info.succ2 = info.myIp;
            }
            info.fingerTable[0] = InetAddress.getByAddress(info.succ);
//            System.err.println("received pickup");
//            System.err.println("succ ip = " + Utils.ipToString(info.succ));
//            System.err.println("succ2 ip = " + Utils.ipToString(info.succ2));
        }

        private void gotNotify(InputStream stream) throws IOException {
            byte[] ip = Utils.readIPFromStream(stream).getAddress();
            if (Utils.insideInterval(Utils.sha1(info.prev), Utils.sha1(info.myIp), Utils.sha1(ip))) {
                info.prev = ip;
                System.err.println("got notify that my prev = " + Utils.ipToString(info.prev));
            }
        }
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(Settings.PORT);
            while (true) {
                new Thread(new EventHandler(server.accept())).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
