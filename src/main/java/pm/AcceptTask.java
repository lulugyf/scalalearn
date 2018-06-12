package pm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Logger;

public class AcceptTask extends Thread{
    private static final Logger logger = Logger.getLogger("AcceptTask");

    private final SocketChannel clientSocketChannel;
    private final ServiceDescriptor serviceDescriptor;
    private int byteBufferCapacity;
    private String clientAddr;

    public AcceptTask(SocketChannel clientSocketChannel, ServiceDescriptor serviceDescriptor, int byteBufferCapacity) {
        this.clientSocketChannel = clientSocketChannel;
        this.serviceDescriptor = serviceDescriptor;
        this.byteBufferCapacity = byteBufferCapacity;
        try {
            clientAddr = clientSocketChannel.getRemoteAddress().toString();
        }catch(IOException e){
        }
    }

    @Override
    public void run() {
        try {
            SocketChannel targetSocketChannel = SocketChannel.open();
            targetSocketChannel.connect(new InetSocketAddress(serviceDescriptor.getRemoteHost(), serviceDescriptor.getRemotePort()));
            targetSocketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            targetSocketChannel.register(selector, SelectionKey.OP_READ, clientSocketChannel);
            clientSocketChannel.register(selector, SelectionKey.OP_READ, targetSocketChannel);

            ByteBuffer byteBuffer = ByteBuffer.allocate(byteBufferCapacity);


            while (selector.keys().size() > 0) {
                if (selector.select() == 0) {
                    continue;
                }

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    SocketChannel from = ((SocketChannel) selectionKey.channel());
                    SocketChannel to = ((SocketChannel) selectionKey.attachment());

                    byteBuffer.clear();
                    int bytesRead = from.read(byteBuffer);
                    if (bytesRead == -1) {
                        from.shutdownInput();
                        to.shutdownOutput();
                        selectionKey.cancel();

                        // cancel operation takes effect only on the next select(), so force the next select
                        selector.wakeup();
                    }else{
                        byteBuffer.flip();
                        int bytesWritten = to.write(byteBuffer);
                    }

                    iterator.remove();
                }
            }

            selector.close();
            clientSocketChannel.close();
            targetSocketChannel.close();
            logger.info("a task of ["+serviceDescriptor.getServiceId() + "] from "+ clientAddr +" finished");
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
