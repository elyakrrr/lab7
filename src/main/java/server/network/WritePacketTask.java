package server.network;
import shared.network.PacketSplitter;
import shared.network.Response;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.logging.Logger;

public class WritePacketTask implements Runnable {
    private static final Logger logger = Logger.getLogger(WritePacketTask.class.getName());
    private final Response response;
    private final SocketAddress clientAddress;
    private final DatagramChannel channel;

    public WritePacketTask(Response response, SocketAddress clientAddress, DatagramChannel channel) {
        this.response = response;
        this.clientAddress = clientAddress;
        this.channel = channel;
    }

    @Override
    public void run() {
        try {
            List<byte[]> packets = PacketSplitter.split(response);
            for (byte[] packet : packets) {
                channel.send(ByteBuffer.wrap(packet), clientAddress);
            }
            channel.send(ByteBuffer.wrap(new byte[]{0}), clientAddress);
            logger.info("Response sent to " + clientAddress);
        } catch (IOException e) {
            logger.severe("Ошибка отправки ответа: " + e.getMessage());
        }
    }
}