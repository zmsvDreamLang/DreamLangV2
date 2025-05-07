package examples.java;

public class Scope {
    public static void main(String[] args) {
    {
        String str = new String("Hello");
    }
    System.out.println(str);
}
}