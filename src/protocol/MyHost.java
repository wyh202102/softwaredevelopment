package protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class MyHost {
    protected final int WINDOW_SIZE;//窗口大小
    protected int DATA_NUMBER;//分组数
    protected final int TIMEOUT;//超时时间，以秒计
    protected String hostName;//主机名称

    /*发送数据相关*/
    protected int nextSeq = 1;//下一个发送分组序号
    protected int base = 1;//当前窗口起始位置
    protected InetAddress destAddress;//目标地址
    protected int destPort = 80;//目标端口

    /*接收数据相关*/
    protected int expectedSeq = 1;//期望得到的分组序列号

    /*Sockets*/
    protected DatagramSocket sendSocket;//发送分组
    protected DatagramSocket receiveSocket;//接收分组

    public MyHost(int RECEIVE_PORT, int WINDOW_SIZE, int DATA_NUMBER, int TIMEOUT, String name) throws IOException {
        this.WINDOW_SIZE = WINDOW_SIZE;
        this.DATA_NUMBER = DATA_NUMBER;
        this.TIMEOUT = TIMEOUT;
        this.hostName = name;

        sendSocket = new DatagramSocket();
        receiveSocket = new DatagramSocket(RECEIVE_PORT);
        destAddress = InetAddress.getLocalHost();
    }

    public InetAddress getDestAddress() {
        return destAddress;
    }//获得目标地址
    public void setDestAddress(InetAddress destAddress) {
        this.destAddress = destAddress;
    }//设置目标地址
    public int getDestPort() {
        return destPort;
    }//获得目标端口
    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }//设置目标端口


    public abstract void sendData() throws IOException;

    public abstract void timeOut() throws IOException;

    public abstract void receive() throws IOException;

    /**
     * 向发送方回应ACK。
     *
     * @param seq    ACK序列号
     * @param toAddr 目的地址
     * @param toPort 目的端口
     * @throws IOException socket相关错误时抛出
     */
    protected void sendACK(int seq, InetAddress toAddr, int toPort) throws IOException {
        String response = hostName + " responses ACK: " + seq;
        byte[] responseData = response.getBytes();
        // 获得来源IP地址和端口，确定发给谁
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, toAddr, toPort);
        receiveSocket.send(responsePacket);
    }

    public String getHostName() {
        return hostName;
    }
}
