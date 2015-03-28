import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Borys Minaiev on 26.03.2015.
 */
public class TcpReceiver implements Runnable {
    Info info;

    TcpReceiver(Info info) {
        this.info = info;
    }

    class ResponseManager implements Runnable {
        Socket socket;

        public ResponseManager(Socket socket) {
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
                    case Codes.ADD_ENTRY:
                        addEntry(input, output);
                        break;
                    case Codes.GET_IP:
                        getIP(input, output);
                        break;
                    case Codes.GET_DATA:
                        getData(input, output);
                        break;
                    case Codes.PRED_FAILED:
                        predFailed(input, output);
                        break;
                    case Codes.ADD_TO_BACKUP:
                        addToBackUp(input, output);
                        break;
                }
                socket.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }

        private void getPredecessor(OutputStream output) throws IOException {
            output.write(Codes.OK);
            output.write(info.prev);
        }

        private void addToBackUp(InputStream input, OutputStream output) throws IOException {
            output.write(Codes.OK);
        }




        private void findSuccessor(InputStream input, OutputStream output) throws IOException {
            int sha1 = Utils.intFromBytes(Utils.readIPFromStream(input).getAddress());
            if (Settings.DEBUG) {
                System.err.println("ask next for sha1 = " + sha1);
                System.err.println("cur prev = " + Utils.ipToString(info.prev));
            }
            if (Utils.insideInterval(Utils.sha1(info.prev), Utils.sha1(info.myIp), sha1)) {
                output.write(Codes.OK);
                output.write(info.myIp);
//                if (Settings.DEBUG) {
                    System.err.println("result for " +  sha1 + " is  = myIp");
//                }
                return;
            }
            for (int i = info.fingerTable.length - 1; i >= 0; i--) {
                if (Utils.insideInterval(Utils.sha1(info.myIp), sha1, Utils.sha1(info.fingerTable[i].getAddress()))
                        && !Arrays.equals(info.myIp, info.fingerTable[i].getAddress()) && Utils.sha1(info.myIp) != sha1) {
                    if (Settings.DEBUG) {
                        System.err.println("asked for help from " + Utils.ipToString(info.fingerTable[i].getAddress()));
                    }
                    InetAddress res = Utils.sendFindSuccessorRequest(info.fingerTable[i], sha1);
                    if (res == null) {
                        output.write(Codes.FAIL);
                        return;
                    }
                    output.write(Codes.OK);
                    output.write(res.getAddress());
                    if (Settings.DEBUG) {
                        System.err.println("result  = " + Utils.ipToString(res.getAddress()));
                    }
                    return;
                }
            }
            if (Arrays.equals(info.myIp, info.succ)) {
                output.write(Codes.FAIL);
                return;
            }
            InetAddress res;
            try {
                res = Utils.sendFindSuccessorRequest(InetAddress.getByAddress(info.succ), sha1);
            } catch (ConnectException e) {
                res = null;
            }
            if (res == null) {
                output.write(Codes.FAIL);
                return;
            }
            output.write(Codes.OK);
            output.write(res.getAddress());
            if (Settings.DEBUG) {
                System.err.println("result = " + Utils.ipToString(res.getAddress()));
            }
            return;
        }

        private void gotPickUp(InputStream stream) throws IOException {
//            System.err.println("GOT PICK UP MESSAGE");
            InetAddress askAddress = Utils.readIPFromStream(stream);
            if (Settings.DEBUG) {
                System.err.println("from ip = " + Utils.ipToString(askAddress.getAddress()));
            }
            info.succ = Utils.sendFindSuccessorRequest(askAddress, Utils.sha1(info.myIp)).getAddress();
            if (info.succ == null) {
                info.succ = info.myIp;
            } else {
                info.succ2 = Utils.sendFindSuccessorRequest(askAddress, Utils.sha1(info.succ)).getAddress();
            }
            if (Arrays.equals(info.succ, info.succ2)) {
                info.succ2 = info.myIp;
            }
            info.fingerTable[0] = InetAddress.getByAddress(info.succ);
        }

        private void addEntry(InputStream stream, OutputStream output) throws IOException {
            int sha = Utils.readInt(stream);
            if (info.whereMap.containsKey(sha)) {
                output.write(Codes.FAIL); // TODO: CHANGE CODE
                return;
            }
            info.whereMap.put(sha, Utils.readIp(stream));
            output.write(Codes.OK);
        }

        private void predFailed(InputStream stream, OutputStream output) throws IOException {
            System.err.println("pred failed");
            byte[] ips = Utils.readIPFromStream(stream).getAddress();
            info.prev = ips;
        }

        private void getIP(InputStream stream, OutputStream output) throws IOException {
            int sha = Utils.readInt(stream);
            if (info.whereMap.containsKey(sha)) {
                output.write(Codes.OK);
                output.write(info.whereMap.get(sha));
                return;
            }
            output.write(Codes.FAIL);
        }


        private void getData(InputStream stream, OutputStream output) throws IOException {
            int sha = Utils.readInt(stream);
            if (info.map.containsKey(sha)) {
                output.write(Codes.OK);
                Utils.writeInt(output, info.map.get(sha).length());
                for (char c : info.map.get(sha).toCharArray()) {
                    output.write(c);
                }
                return;
            }
            output.write(Codes.FAIL);
        }

        private void gotNotify(InputStream stream) throws IOException {
            byte[] ip = Utils.readIPFromStream(stream).getAddress();
            if (Utils.insideInterval(Utils.sha1(info.prev), Utils.sha1(info.myIp), Utils.sha1(ip))) {
                info.prev = ip;
                if (Settings.DEBUG) {
                    System.err.println("got notify that my prev = " + Utils.ipToString(info.prev));
                }
                HashSet<Integer> toRemove = new HashSet<>();
                for (Map.Entry<Integer, byte[]> entry : info.whereMap.entrySet()) {
                    // prev in [entry, myIp)
                    if (Utils.insideInterval(entry.getKey(), Utils.sha1(info.myIp), Utils.sha1(info.prev))) {
                        System.err.println("want remove " + entry.getKey());
                        if (Utils.sendDataToSomeone(InetAddress.getByAddress(info.prev), entry.getKey(), entry.getValue())) {
                            toRemove.add(entry.getKey());
                        } else {
                            System.err.println("failed to submit data to " + Utils.ipToString(info.prev));
                        }
                    }
                }
                for (int entry : toRemove) {
                    info.whereMap.remove(entry);
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(Settings.PORT);
            while (true) {
                new Thread(new ResponseManager(server.accept())).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
