package io.leego.test;

/**
 * @author Leego Yih
 */
public class SynchronizedStyle {
    private final Object lock = new Object();

    public void syncObject() {
        synchronized (lock) {
            System.out.println("syncObject");
        }
    }

    public synchronized void syncMethod() {
        System.out.println("syncMethod");
    }


    public static synchronized void syncStaticMethod() {
        System.out.println("syncObject");
    }

    public void syncClass() {
        synchronized (SynchronizedStyle.class) {
            System.out.println("syncBlock");
        }
    }
}
