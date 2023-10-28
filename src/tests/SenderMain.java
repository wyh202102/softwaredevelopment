package tests;

import protocol.GBNHost;
import protocol.MyHost;
import protocol.SRHost;

import java.io.IOException;
import java.util.Scanner;

public class SenderMain {
    private static int host1Port = 808;    // host 1占用端口

    private static int host2Port = 809;

    public static void main(String[] args) throws IOException {
        MyHost sender = new GBNHost(host1Port, 1, 20, 3, "Sender");
        sender.setDestPort(host2Port);
        System.out.println("选择发送（0）/接收（1）");
        Scanner in  = new Scanner(System.in);
        int x = in.nextInt();
        if(x==0){
            new Thread(() -> {
                try {
                    sender.sendData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();}
        else if(x == 1){
            new Thread(() -> {
                try {
                    sender.receive();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
