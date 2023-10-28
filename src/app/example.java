package app;
import java.io.IOException;

public class example {

    public static void main(String[] args) throws IOException {
        CSapp sender = new CSapp(20,1,30,30,"sender","C:\\Users\\wyh\\Desktop\\lab2\\src\\app\\source\\a.txt");
        CSapp recieve = new CSapp(80,1,30,30,"reciever","C:\\Users\\wyh\\Desktop\\lab2\\src\\app\\source\\b.txt");
        new Thread(() -> {
            try {
                recieve.receive();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            sender.send();
        }).start();
    }


    public example() throws IOException {
    }
}
