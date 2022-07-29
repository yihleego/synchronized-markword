package io.leego.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Leego Yih
 */
public class SynchronizedMarkWordTests {
    private static final Object lock = new Object();

    @BeforeEach
    public void before() {
        StringBuilder s = new StringBuilder();
        s.append("Java Version: ");
        s.append(System.getProperty("java.version")).append("\n");
        s.append("Java VM: ");
        s.append(System.getProperty("java.vm.name")).append("\n");
        s.append("VM Options: ");
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : args) {
            if (arg.contains("-X")) {
                s.append(arg).append(" ");
            }
        }
        s.append("\n");
        System.out.println(s);
    }

    /**
     * 偏向锁-重入
     */
    @Test
    public void testBiasedLocking_Reenter() {
        print("无锁时");

        runUntilNotAlive(() -> {
            synchronized (lock) {
                print("[线程-0] 获取锁");
                synchronized (lock) {
                    print("[线程-0] 重入获取锁");
                }
                print("[线程-0] 重入释放锁");
            }
            print("[线程-0] 释放锁");
        });
    }

    /**
     * 偏向锁升级轻量级锁
     * 当前线程进行加锁解锁操作，由于当前线程会一直处于存活状态，所以后一个线程无法获取偏向锁，升级为轻量级锁。
     */
    @Test
    public void testBiasedLocking_Upgrade() {
        print("无锁时");

        run(() -> {
            synchronized (lock) {
                print("[线程-0] 获取锁");
            }
            print("[线程-0] 释放锁");
            while (true) {
                sleep(1);
            }
        });

        runUntilNotAlive(() -> {
            synchronized (lock) {
                print("[线程-1] 获取锁");
            }
            print("[线程-1] 释放锁");
        });
    }

    /**
     * 偏向锁重偏向
     * 三个线程依次获取锁，前一个线程执行完成，且线程为非存活状态后，再执行后一个线程。
     */
    @Test
    public void testBiasedLocking_Rebias() {
        print("无锁时");

        Thread t0 = run(() -> {
            synchronized (lock) {
                print("[线程-0] 获取锁");
            }
            print("[线程-0] 释放锁");
        });

        waitUntilNotAlive(t0);
        busy();
        sleep(1000);
        //System.gc();

        Thread t1 = run(() -> {
            synchronized (lock) {
                print("[线程-1] 获取锁");
            }
            print("[线程-1] 释放锁");
        });

        waitUntilNotAlive(t1);
        busy();
        sleep(1000);
        //System.gc();

        Thread t2 = run(() -> {
            synchronized (lock) {
                print("[线程-2] 获取锁");
            }
            print("[线程-2] 释放锁");
        });

        waitUntilNotAlive(t2);
    }

    /**
     * 轻量级锁-重入
     * 禁用偏向锁 -XX:-UseBiasedLocking
     */
    @Test
    public void testLightweightLocking_Reenter() {
        print("无锁时");

        synchronized (lock) {
            print("[当前线程] 获取锁");
            synchronized (lock) {
                print("[当前线程] 重入获取锁");
            }
            print("[当前线程] 重入释放锁");
        }
        print("[当前线程] 释放锁");
    }

    /**
     * 轻量级锁膨胀为重量级锁
     * 多个线程同时竞争，直接膨胀为重量级锁。
     * 禁用偏向锁 -XX:-UseBiasedLocking
     */
    @Test
    public void testLightweightLocking_Inflate() {
        print("无锁时");

        Thread t0 = run(() -> {
            synchronized (lock) {
                print("[线程-0] 获取锁");
            }
            print("[线程-0] 释放锁");
        });

        Thread t1 = run(() -> {
            synchronized (lock) {
                print("[线程-1] 获取锁");
            }
            print("[线程-1] 释放锁");
        });

        waitUntilNotAlive(t0, t1);
    }

    /**
     * 重量级锁降级
     * 禁用偏向锁 -XX:-UseBiasedLocking
     */
    @Test
    public void testLightweightLocking_Deflate() {
        print("无锁时");

        Thread[] ts = new Thread[10];
        for (int i = 0; i < ts.length; i++) {
            final int f = i;
            ts[i] = run(() -> {
                synchronized (lock) {
                    print("[线程-" + f + "] 获取锁");
                }
                print("[线程-" + f + "] 释放锁");
            });
        }

        waitUntilNotAlive(ts);

        print("无竞争后");

        sleep(1000);

        print("休眠一段时间后");

        synchronized (lock) {
            print("[当前线程] 再次获取锁");
        }
        print("[当前线程] 再次释放锁");
    }

    public Thread run(Runnable r) {
        Thread t = new Thread(r);
        t.start();
        return t;
    }

    public void runUntilNotAlive(Runnable r) {
        Thread t = new Thread(r);
        t.start();
        waitUntilNotAlive(t);
        t = null;
    }

    public void waitUntilNotAlive(Thread t) {
        while (true) {
            if (!t.isAlive()) {
                break;
            }
        }
        t = null;
    }

    public void waitUntilNotAlive(Thread... ts) {
        while (true) {
            int count = 0;
            for (Thread t : ts) {
                if (!t.isAlive()) {
                    count++;
                }
            }
            if (count == ts.length) {
                break;
            }
        }
        ts = null;
    }

    public void print(String prefix) {
        System.out.println(prefix + "\n" + ClassLayout.parseInstance(lock).toPrintable());
    }

    public int busy() {
        int adder = 0;
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            adder += i;
        }
        return adder;
    }

    public void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
