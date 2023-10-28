package tests;

import protocol.GBNHost;
import protocol.MyHost;

import java.io.IOException;
import java.util.Scanner;

public class ReceiverMain {
    private static int host1Port = 808;    // host 1占用端口

    private static int host2Port = 809;    // host 2 占用

    public static void main(String[] args) throws IOException {
        MyHost receiver = new GBNHost(host2Port, 1, 20, 3, "Receiver");
        receiver.setDestPort(host1Port);
        System.out.println("选择发送（0）/接收（1）");
        Scanner in  = new Scanner(System.in);
        int x = in.nextInt();
        if(x==0){
            new Thread(() -> {
            try {
                receiver.sendData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();}
        else if(x == 1){
            new Thread(() -> {
                try {
                    receiver.receive();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
//        new Thread(() -> {
//            try {
//                receiver.sendData();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
    }
}
