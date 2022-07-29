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
     * 偏向锁重入
     */
    @Test
    public void testBiasedLocking_Reenter() {
        print("无锁时");
        // 创建[线程-0]
        run(() -> {
            // 首次获取偏向锁
            synchronized (lock) {
                print("[线程-0] 获取锁");
                // 偏向锁重入
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
        // 创建[线程-0]，获取偏向锁后释放，并保持线程处于活跃状态
        run(() -> {
            synchronized (lock) {
                print("[线程-0] 获取锁");
            }
            print("[线程-0] 释放锁");
            // 保持线程处于存活状态
            while (true) {
                sleep(1);
            }
        });
        // 保证[线程-0]释放锁
        sleep(1000);
        // 创建[线程-1]，再次获取锁，此时升级为轻量级锁
        run(() -> {
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
        // 创建[线程-0]，获取偏向锁后释放，线程处于非活跃状态时，继续往下执行
        runUntilNotAlive(() -> {
            synchronized (lock) {
                print("[线程-0] 获取锁");
            }
            print("[线程-0] 释放锁");
        });

        busy();
        //sleep(5000);
        //System.gc();

        // 创建[线程-1]，获取偏向锁后释放，线程处于非活跃状态时，继续往下执行
        runUntilNotAlive(() -> {
            synchronized (lock) {
                print("[线程-1] 获取锁");
            }
            print("[线程-1] 释放锁");
        });

        busy();
        //sleep(5000);
        //System.gc();

        // 创建[线程-2]，获取偏向锁后释放
        runUntilNotAlive(() -> {
            synchronized (lock) {
                print("[线程-2] 获取锁");
            }
            print("[线程-2] 释放锁");
        });
    }

    /**
     * 轻量级锁重入
     * 禁用偏向锁 -XX:-UseBiasedLocking
     */
    @Test
    public void testLightweightLocking_Reenter() {
        print("无锁时");
        // 创建新线程并运行
        run(() -> {
            // 首次获取轻量级锁锁
            synchronized (lock) {
                print("[线程-0] 获取锁");
                // 轻量级锁重入
                synchronized (lock) {
                    print("[线程-0] 重入获取锁");
                }
                print("[线程-0] 重入释放锁");
            }
            print("[线程-0] 释放锁");
        });
    }

    /**
     * 轻量级锁膨胀为重量级锁
     * 多个线程同时竞争，直接膨胀为重量级锁。
     * 禁用偏向锁 -XX:-UseBiasedLocking
     */
    @Test
    public void testLightweightLocking_Inflate() {
        print("无锁时");
        // 创建[线程-0]，获取轻量级锁
        Thread t0 = run(() -> {
            synchronized (lock) {
                print("[线程-0] 获取锁");
            }
            print("[线程-0] 释放锁");
        });
        // 创建[线程-1]，将轻量级锁膨胀重量级锁
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
        // 创建 n 个线程竞争，直接膨胀为重量级锁
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
        // 等待 n 个线程执行完成
        waitUntilNotAlive(ts);
        // 无竞争后，依然是重量级锁
        print("无竞争后");
        // 线程休眠一段时间
        sleep(1000);
        // 锁被重置为无锁状态
        print("线程休眠一段时间后");
        // 当前线程重新获取锁，结果为轻量级锁
        synchronized (lock) {
            print("[当前线程] 再次获取锁");
        }
        print("[当前线程] 再次释放锁");
    }

    /** 创建一个新线程并运行 */
    public Thread run(Runnable r) {
        Thread t = new Thread(r);
        t.start();
        return t;
    }

    /** 创建一个新线程并运行，等待线程结束后返回 */
    public void runUntilNotAlive(Runnable r) {
        Thread t = new Thread(r);
        t.start();
        waitUntilNotAlive(t);
        t = null;
    }

    /** 等待线程结束 */
    public void waitUntilNotAlive(Thread t) {
        while (true) {
            if (!t.isAlive() && t.getState() == Thread.State.TERMINATED) {
                break;
            }
        }
        t = null;
    }

    /** 等待线程结束 */
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

    /** 打印 lock 对象的 layout */
    public void print(String prefix) {
        System.out.println(prefix + "\n" + ClassLayout.parseInstance(lock).toPrintable());
    }

    /**
     * 假装繁忙
     */
    public int busy() {
        int adder = 0;
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            adder += i;
        }
        return adder;
    }

    /**
     * 线程休眠
     */
    public void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
