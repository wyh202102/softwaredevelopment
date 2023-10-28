package protocol;

import timer.TimeModel;
import timer.Timer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

public class SRHost extends MyHost {
    /*SR协议相关*/
    private Set<Integer> senderSentSet = new HashSet<>();//发送过的分组
    private Set<Integer> senderReceivedACKSet = new HashSet<>();//收到的ACK
    private Set<Integer> receiverReceivedSet = new HashSet<>();//收到的分组
    public SRHost(int RECEIVE_PORT, int WINDOW_SIZE, int DATA_NUMBER, int TIMEOUT, String name) throws IOException {
        super(RECEIVE_PORT, WINDOW_SIZE, DATA_NUMBER, TIMEOUT, name);
    }

    @Override
    public void sendData() throws IOException {
        TimeModel timeModel = new TimeModel();
        Timer timer = new Timer(this, timeModel);
        timeModel.setTime(0);
        timer.start();

        while (true) {
            // 发送分组循环
            while (nextSeq < base + WINDOW_SIZE && nextSeq <= DATA_NUMBER && !senderSentSet.contains(nextSeq)) {

                // 模拟数据丢失
                if (nextSeq % 5 != 0) {
                    // 不丢失
                    String sendData = hostName + ": Sending to port " + destPort + ", Seq = " + nextSeq;

                    // 模拟发送分组
                    byte[] data = sendData.getBytes();
                    DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destAddress, destPort);
                    sendSocket.send(datagramPacket);

                    System.out.println(hostName + "发送到" + destPort + "端口， Seq = " + nextSeq);
                } else {
                    // 丢失
                    System.out.println("假装丢失Seq = " + nextSeq);
                }

                senderSentSet.add(nextSeq);   // 加入已发送set
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
            sendSocket.setSoTimeout(4000);
            try {
                sendSocket.receive(datagramPacket);
            } catch (SocketTimeoutException ex) {
                // timeout 因为对方未回应任何数据
                System.out.println(hostName + "等待ACK超时");
                nextSeq--;  // 让它重新进入上面的循环里
                senderSentSet.remove(nextSeq);
                continue;
            }

            // 转换成String
            String fromServer = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
            // 解析出ACK编号
            int ack = Integer.parseInt(fromServer.substring(fromServer.indexOf("ACK: ") + 5).trim());
            senderReceivedACKSet.add(ack);    // 加入已收到set
            System.out.println(hostName + "收到了 ACK: " + ack);
            if (base == ack) {
                // 向右滑动
                while (senderReceivedACKSet.contains(base)) {
                    base++;
                }
                System.out.println("\n当前窗口 base = " + base + "，最大发送到 " + (base + WINDOW_SIZE - 1) + " \n");
            }
            if (ack >= base + WINDOW_SIZE) {
                // 理论上一定false
                assert false;
            }
            if (base == nextSeq) {
                // 停止计时器
                timeModel.setTime(0);
            } else {
                // 开始计时器
                timeModel.setTime(TIMEOUT);
            }
//            System.out.println(hostName + "当前base = " + base);
            System.out.println();

            // 发送完了，此时base会滑到右边多一格
            if (base == DATA_NUMBER + 1) {
                // 停止计时器
                timeModel.setTime(0);
                System.out.println(hostName + "发送完毕，接收方反馈已全部正确接收");
                System.exit(0);
            }
        }
    }

    @Override
    public void timeOut() throws IOException {
        Set<Integer> temp = new HashSet<>(senderSentSet);
        temp.removeAll(senderReceivedACKSet);
        System.out.println("\n" + temp + "\n");
        for (int i : temp) {
            String resendData = hostName
                    + ": Resending to port " + destPort + ", Seq = " + i;

            byte[] data = resendData.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(data,
                    data.length, destAddress, destPort);
            sendSocket.send(datagramPacket);

            System.out.println(hostName
                    + "重新发送到" + destPort + "端口， Seq = " + i);
            break;  // 只发一次就break
        }
    }

    @Override
    public void receive() throws IOException {
        int rcvBase = 1;

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
                int seq = Integer.parseInt(received.substring(seqIndex + 6).trim());
                if (seq >= rcvBase && seq <= rcvBase + WINDOW_SIZE - 1) {
                    receiverReceivedSet.add(seq);
                    System.out.println(hostName + "收到一个窗口内的分组，Seq = " + seq + "已确认\n");

                    // 模拟ACK丢失。
                    // 用seq mod一个数，不发送ACK来模拟，其他操作正常进行
                    if (seq % 17 != 0) {
                        // 发送ACK
                        sendACK(seq, receivePacket.getAddress(), receivePacket.getPort());
                    } else {
                        System.out.println(hostName + "假装丢失ACK: " + seq);
                    }

                    if (seq == rcvBase) {
                        // 收到这个分组后可以开始滑动
                        while (receiverReceivedSet.contains(rcvBase)) {
                            rcvBase++;
                        }
                    }

                } else if (seq >= rcvBase - WINDOW_SIZE && seq <= rcvBase - 1) {
                    System.out.println(hostName + "收到一个已经确认过的分组，Seq = " + seq + "已再次确认");
                    sendACK(seq, receivePacket.getAddress(), receivePacket.getPort());
                } else {
                    // 这个分组序列号太大，不在窗口内，应该舍弃
                    System.out.println(hostName + "收到一个不在窗口内的分组，Seq = " + seq + "已舍弃");
                }
            }
        }
    }
}
