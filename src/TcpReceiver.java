import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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
                        System.err.println("received pickup");
                        break;
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
