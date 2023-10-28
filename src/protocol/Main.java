package protocol;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    private static int host1Port = 100;    // host 1占用端口

    private static int host2Port = 809;

    public static void main(String[] args) throws IOException {
        startSR();
////        startStopAndWait();
        startGBN();
    }

    private static void startSR() throws IOException {
        MyHost sender = new SRHost(host1Port, 6, 20, 5, "Sender");
        sender.setDestPort(host2Port);
        MyHost receiver = new SRHost(host2Port, 6, 20, 5, "Receiver");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    receiver.receive();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sender.sendData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void startStopAndWait() throws IOException {
        MyHost sender = new GBNHost(host1Port, 1, 20, 3, "Sender");
        sender.setDestPort(host2Port);
        MyHost receiver = new GBNHost(host2Port, 1, 20, 3, "Receiver");

        new Thread(() -> {
            try {
                receiver.receive();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                sender.sendData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void startGBN() throws IOException {
        MyHost sender = new GBNHost(host1Port, 5, 20, 3, "Sender");
        sender.setDestPort(host2Port);
        MyHost receiver = new GBNHost(host2Port, 5, 20, 3, "Receiver");

        new Thread(() -> {
            try {
                receiver.receive();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                sender.sendData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


}
