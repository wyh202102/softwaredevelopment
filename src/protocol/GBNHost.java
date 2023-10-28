package protocol;

import timer.TimeModel;
import timer.Timer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.Random;

/**
 * GBNHost。
 */
public class GBNHost extends MyHost {

    protected TimeModel timeModel = new TimeModel();
    protected Timer timer = new Timer(this, timeModel);
    protected double datarate = 0.2;
    protected double ackrate = 0.2;

    public GBNHost(int RECEIVE_PORT, int WINDOW_SIZE, int DATA_NUMBER, int TIMEOUT, String name) throws IOException {
        super(RECEIVE_PORT, WINDOW_SIZE, DATA_NUMBER, TIMEOUT, name);
    }

    @Override
    public void sendData() throws IOException {
        // 初始化定时器

        timeModel.setTime(0);
        timer.start();

        while (true) {
            // 发送分组循环
            while (nextSeq < base + WINDOW_SIZE && nextSeq <= DATA_NUMBER) {
                // 模拟数据丢失
                if (nextSeq%5 == 0) {
                    System.out.println(hostName + "假装丢失Seq = " + nextSeq);
                    nextSeq++;
                    timeModel.setTime(TIMEOUT);
                    continue;
                }

                String sendData = hostName + ": Sending to port " + destPort + ", Seq = " + nextSeq;

                // 模拟发送分组
                byte[] data = sendData.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destAddress, destPort);
                sendSocket.send(datagramPacket);

                System.out.println(hostName + "发送到" + destPort + "端口， Seq = " + nextSeq);

                if (nextSeq == base) {
                    // 开始计时，等待接收端返回ACK
                    timeModel.setTime(TIMEOUT);
                }
                nextSeq++;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 接收ACK
            // 从服务器端接受ACK
            byte[] bytes = new byte[4096];
            DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
            sendSocket.receive(datagramPacket);

            // 转换成String
            String fromServer = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
            // 解析出ACK编号
            int ack = Integer.parseInt(fromServer.substring(fromServer.indexOf("ACK: ") + 5).trim());
            base = ack + 1;
            if (base == nextSeq) {
                // 停止计时器
                timeModel.setTime(0);
            } else {
                // 开始计时器
                timeModel.setTime(TIMEOUT);
            }
            System.out.println(hostName + "接收到了ACK: " + ack);
            // 发送完了
            if (ack == DATA_NUMBER) {
                // 停止计时器
                timeModel.setTime(0);
                System.out.println(hostName + "发送完毕，接收方反馈已全部正确接收");
                System.exit(0);
            }
        }
    }

    @Override
    public void timeOut() throws IOException {
        for (int i = base; i < nextSeq; i++) {
            String resendData = hostName + ": Resending to port " + destPort + ", Seq = " + i;
            byte[] data = resendData.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destAddress, destPort);
            sendSocket.send(datagramPacket);
            System.out.println(hostName + "重新发送到" + destPort + "端口， Seq = " + i);
        }
    }

    @Override
    public void receive() throws IOException {
        while (true) {
            byte[] receivedData = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);
            receiveSocket.setSoTimeout(5000);
            try {
                receiveSocket.receive(receivePacket);
            } catch (SocketTimeoutException ex) {
                System.out.println(hostName + " 等待下一个分组超时");
                continue;
            }
            // 收到的数据
            String received = new String(receivedData, 0, receivePacket.getLength());
            int seqIndex = received.indexOf("Seq = ");
            if (seqIndex == -1) {
                System.out.println(hostName + " 收到错误数据");
                // 仍发送之前的ACK
                sendACK(expectedSeq - 1, receivePacket.getAddress(), receivePacket.getPort());
            } else {
                if (Integer.parseInt(received.substring(seqIndex + 6).trim()) == expectedSeq) {
                    // 收到了预期的数据
                    // 发送ACK
                    if(nextSeq%7 == 0){
                        System.out.println(hostName + " 假装丢失ACK: " + expectedSeq);
                    }
                    else{
                        sendACK(expectedSeq, receivePacket.getAddress(), receivePacket.getPort());
                    }
                    expectedSeq++;
                    System.out.println(hostName + " 期待的数据Seq = " + expectedSeq);
                    // 期待值加1

                } else {
                    // 未收到预期的Seq
                    System.out.println(hostName + " 期待的数据Seq = " + expectedSeq);
                    System.out.println(hostName + " 未收到预期编号");
                    // 仍发送之前的ACK
                    sendACK(expectedSeq - 1, receivePacket.getAddress(), receivePacket.getPort());
                }
            }
            if(expectedSeq == DATA_NUMBER+1){
                System.exit(0);
            }
        }
    }
}
