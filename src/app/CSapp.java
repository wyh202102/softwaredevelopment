package app;

import protocol.GBNHost;
import timer.TimeModel;
import timer.Timer;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSapp extends GBNHost {

    private String filename;
    protected byte[] dataList = new byte[0];
    protected List<Integer> dataLengthList = new ArrayList<>() ;

    public CSapp(int RECEIVE_PORT, int WINDOW_SIZE, int DATA_NUMBER, int TIMEOUT, String name, String filename) throws IOException {
        super(RECEIVE_PORT, WINDOW_SIZE, DATA_NUMBER, TIMEOUT, name);
        this.filename = filename;
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
                int lossBound = (int) (datarate * 100);
                int x = new Random().nextInt(100);
                if (nextSeq%5 == 0) {
                    System.out.println(hostName + "假装丢失Seq = " + nextSeq);
                    nextSeq++;
                    timeModel.setTime(TIMEOUT);
                    continue;
                }


                // 模拟发送分组
                byte[] data=new byte[1024];
                int length=0;
                length=dataLengthList.get(nextSeq-1);
                int curByte=0;
                for(int i=0 ;i<nextSeq-1;i++){
                    curByte+=dataLengthList.get(i);
                }
                System.arraycopy(dataList,curByte,data,0,length);
                String sendDataLabel = hostName + ": Sending to port " + destPort + ", Seq = " + nextSeq
                        +" length = "+length +" DATA_NUMBER = "+DATA_NUMBER +"@@@@@";
                byte[] datagram = addBytes(sendDataLabel.getBytes(),data);
                DatagramPacket datagramPacket = new DatagramPacket(datagram, datagram.length, destAddress, destPort);
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

    public void send() {
        File file = new File(filename);
        if (file.length() == 0) {
            System.out.println("文件为空！");
            return;
        }
        try {
            DATA_NUMBER = 0;
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int length = 0;
            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                DATA_NUMBER++;
                dataList = addBytes(dataList, bytes);
                dataLengthList.add(length);
            }
            System.out.println(hostName + ":文件被拆分为" + DATA_NUMBER + "个包");
            fis.close();
            sendData();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;

    }
    @Override
    public void receive() throws IOException{
        File output = new File(filename);
        FileOutputStream fos= new FileOutputStream(output);
        while (true) {
            byte[] receivedData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);
            receiveSocket.setSoTimeout(1000*TIMEOUT);
            try {
                receiveSocket.receive(receivePacket);
            } catch (SocketTimeoutException ex) {
                System.out.println(hostName + " 在正在等待分组： Seq= "+expectedSeq+"的到来 ");
                continue;
            }

            // 收到的数据
            String receivedLabel = new String(receivedData, 0,receivedData.length );
            String label =receivedLabel.split("@@@@@")[0];
            int labelSize = (label+"@@@@@").getBytes().length;

            String pattern = "\\w*: Sending to port \\d+, Seq = (\\d+) length = (\\d+) DATA_NUMBER = (\\d+)";
            Matcher matcher = Pattern.compile(pattern).matcher(label);

            if (!matcher.find()) {
                System.out.println(hostName + " 收到错误数据"+label);
                // 仍发送之前的ACK
                sendACK(expectedSeq - 1, receivePacket.getAddress(), receivePacket.getPort());
                continue;
            }

            int receivedSeq=Integer.parseInt(matcher.group(1));
            int dataLength = Integer.parseInt(matcher.group(2));
            DATA_NUMBER = Integer.parseInt(matcher.group(3));

            if (receivedSeq  == expectedSeq) {
                // 收到了预期的数据
                System.out.println(hostName + " 收到了期待的数据,发送ACK：Seq = " + expectedSeq);
                System.out.println(hostName + "写入数据 " + expectedSeq );
                if(nextSeq == 1){
                    fos.write(receivedData,0,dataLength);
                }
                else {
                    fos.write(receivedData, labelSize + (nextSeq - 1) * dataLength, dataLength);
                }
                // 发送ACK
                if (expectedSeq % 7 == 0) {
                    //不发送ACK
                    System.out.println(hostName + "收到了期待的数据,但是模拟丢失ACK: " + expectedSeq);
                } else {
                    sendACK(expectedSeq, receivePacket.getAddress(), receivePacket.getPort());

                }

                if (expectedSeq == DATA_NUMBER) {
                    System.out.println(hostName + "接受完成");
                    fos.flush();
                    fos.close();
                    return;
                }
                // 期待值加1
                expectedSeq++;

            } else {
                // 未收到预期的Seq
                System.out.println(hostName + " 实际收到的数据Seq ="+receivedSeq+"，然而期待顺序收到数据Seq = " + expectedSeq+" 因此丢弃此分组");
                // 仍发送之前的ACK
                sendACK(expectedSeq - 1, receivePacket.getAddress(), receivePacket.getPort());
            }

        }
    }
}

