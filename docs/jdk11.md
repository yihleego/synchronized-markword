# OpenJDK 11

运行环境：

- java version "11.0.16" 2022-07-19 LTS
- Java(TM) SE Runtime Environment 18.9 (build 11.0.16+11-LTS-199)
- Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.16+11-LTS-199, mixed mode)

## 偏向锁重入

### 运行参数

- `-XX:BiasedLockingStartupDelay=0`：设置偏向锁延迟为`0ms`，即不延迟。
- `-Xlog:gc*`：打印 GC 日志。

### 代码

```java
print("无锁时");
// 创建新线程并运行
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
```

### 输出

```text
[0.021s][info][gc,heap] Heap region size: 1M
[0.028s][info][gc     ] Using G1
[0.028s][info][gc,heap,coops] Heap address: 0x0000000780000000, size: 2048 MB, Compressed Oops mode: Zero based, Oop shift amount: 3
[0.917s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.917s][info][gc,task      ] GC(0) Using 3 workers of 4 for evacuation
[0.921s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
[0.921s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 3.5ms
[0.921s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.6ms
[0.921s][info][gc,phases    ] GC(0)   Other: 0.3ms
[0.921s][info][gc,heap      ] GC(0) Eden regions: 14->0(74)
[0.921s][info][gc,heap      ] GC(0) Survivor regions: 0->2(2)
[0.921s][info][gc,heap      ] GC(0) Old regions: 0->2
[0.921s][info][gc,heap      ] GC(0) Humongous regions: 0->0
[0.921s][info][gc,metaspace ] GC(0) Metaspace: 9648K->9648K(1058816K)
[0.921s][info][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 14M->3M(128M) 4.450ms
[0.921s][info][gc,cpu       ] GC(0) User=0.02s Sys=0.00s Real=0.01s
Java Version: 11.0.16
Java VM: Java HotSpot(TM) 64-Bit Server VM
VM Options: -XX:BiasedLockingStartupDelay=0 -Xlog:gc* 

无锁时
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000005 (biasable; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f9cb2186805 (biased: 0x0000001fe72c861a; epoch: 0; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 重入获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f9cb2186805 (biased: 0x0000001fe72c861a; epoch: 0; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 重入释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f9cb2186805 (biased: 0x0000001fe72c861a; epoch: 0; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f9cb2186805 (biased: 0x0000001fe72c861a; epoch: 0; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[3.488s][info][gc,heap,exit ] Heap
[3.488s][info][gc,heap,exit ]  garbage-first heap   total 131072K, used 23214K [0x0000000780000000, 0x0000000800000000)
[3.488s][info][gc,heap,exit ]   region size 1024K, 22 young (22528K), 2 survivors (2048K)
[3.488s][info][gc,heap,exit ]  Metaspace       used 14946K, capacity 15667K, committed 15744K, reserved 1062912K
[3.488s][info][gc,heap,exit ]   class space    used 1612K, capacity 1894K, committed 1920K, reserved 1048576K
```

### 总结

偏向锁可重入，且 Mark Word 符合预期。

### 源码

[SynchronizedMarkWordTests#testBiasedLocking_Reenter](https://github.com/yihleego/synchronized-markword/blob/main/src/test/java/io/leego/test/SynchronizedMarkWordTests.java#L39)

## 偏向锁升级轻量级锁

### 运行参数

- `-XX:BiasedLockingStartupDelay=0`：设置偏向锁延迟为`0ms`，即不延迟。
- `-Xlog:gc*`：打印 GC 日志。

### 代码

```java
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
```

### 输出

```text
[0.011s][info][gc,heap] Heap region size: 1M
[0.013s][info][gc     ] Using G1
[0.013s][info][gc,heap,coops] Heap address: 0x0000000780000000, size: 2048 MB, Compressed Oops mode: Zero based, Oop shift amount: 3
[0.831s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.831s][info][gc,task      ] GC(0) Using 3 workers of 4 for evacuation
[0.836s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
[0.836s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 3.9ms
[0.836s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.5ms
[0.836s][info][gc,phases    ] GC(0)   Other: 0.3ms
[0.836s][info][gc,heap      ] GC(0) Eden regions: 14->0(74)
[0.836s][info][gc,heap      ] GC(0) Survivor regions: 0->2(2)
[0.836s][info][gc,heap      ] GC(0) Old regions: 0->2
[0.836s][info][gc,heap      ] GC(0) Humongous regions: 0->0
[0.836s][info][gc,metaspace ] GC(0) Metaspace: 9634K->9634K(1058816K)
[0.836s][info][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 14M->3M(128M) 4.750ms
[0.836s][info][gc,cpu       ] GC(0) User=0.01s Sys=0.00s Real=0.00s
Java Version: 11.0.16
Java VM: Java HotSpot(TM) 64-Bit Server VM
VM Options: -XX:BiasedLockingStartupDelay=0 -Xlog:gc* 

无锁时
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x000000000000000d (biasable; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007fba1f1c700d (biased: 0x0000001fee87c71c; epoch: 0; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007fba1f1c700d (biased: 0x0000001fee87c71c; epoch: 0; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-1] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x000070000c4cf980 (thin lock: 0x000070000c4cf980)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-1] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000009 (non-biasable; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[4.713s][info][gc,heap,exit ] Heap
[4.713s][info][gc,heap,exit ]  garbage-first heap   total 131072K, used 23525K [0x0000000780000000, 0x0000000800000000)
[4.713s][info][gc,heap,exit ]   region size 1024K, 23 young (23552K), 2 survivors (2048K)
[4.713s][info][gc,heap,exit ]  Metaspace       used 14986K, capacity 15671K, committed 15744K, reserved 1062912K
[4.713s][info][gc,heap,exit ]   class space    used 1612K, capacity 1895K, committed 1920K, reserved 1048576K
```

### 总结

由于持有过偏向锁的`线程-0`处于存活状态，所以`线程-1`再次获取锁时，会升级为轻量级锁。

### 源码

[SynchronizedMarkWordTests#testBiasedLocking_Upgrade](https://github.com/yihleego/synchronized-markword/blob/main/src/test/java/io/leego/test/SynchronizedMarkWordTests.java#L61)

## 偏向锁重偏向

### 运行参数

- `-XX:BiasedLockingStartupDelay=0`：设置偏向锁延迟为`0ms`，即不延迟。
- `-Xlog:gc*`：打印 GC 日志。

### 代码

```java
print("无锁时");
// 创建[线程-0]，获取偏向锁后释放，线程处于非活跃状态时，继续往下执行
runUntilNotAlive(() -> {
    synchronized (lock) {
        print("[线程-0] 获取锁");
    }
    print("[线程-0] 释放锁");
});
// 创建[线程-1]，获取偏向锁后释放，线程处于非活跃状态时，继续往下执行
runUntilNotAlive(() -> {
    synchronized (lock) {
        print("[线程-1] 获取锁");
    }
    print("[线程-1] 释放锁");
});
// 创建[线程-2]，获取偏向锁后释放
runUntilNotAlive(() -> {
    synchronized (lock) {
        print("[线程-2] 获取锁");
    }
    print("[线程-2] 释放锁");
});
```

### 输出

```text
[0.029s][info][gc,heap] Heap region size: 1M
[0.031s][info][gc     ] Using G1
[0.031s][info][gc,heap,coops] Heap address: 0x0000000780000000, size: 2048 MB, Compressed Oops mode: Zero based, Oop shift amount: 3
[0.917s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.917s][info][gc,task      ] GC(0) Using 3 workers of 4 for evacuation
[0.922s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
[0.922s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 3.9ms
[0.922s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.7ms
[0.922s][info][gc,phases    ] GC(0)   Other: 0.4ms
[0.922s][info][gc,heap      ] GC(0) Eden regions: 14->0(74)
[0.922s][info][gc,heap      ] GC(0) Survivor regions: 0->2(2)
[0.922s][info][gc,heap      ] GC(0) Old regions: 0->2
[0.922s][info][gc,heap      ] GC(0) Humongous regions: 0->0
[0.922s][info][gc,metaspace ] GC(0) Metaspace: 9658K->9658K(1058816K)
[0.922s][info][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 14M->3M(128M) 5.119ms
[0.922s][info][gc,cpu       ] GC(0) User=0.02s Sys=0.00s Real=0.00s
Java Version: 11.0.16
Java VM: Java HotSpot(TM) 64-Bit Server VM
VM Options: -XX:BiasedLockingStartupDelay=0 -Xlog:gc* 

无锁时
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x000000000000000d (biasable; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f893b1ba00d (biased: 0x0000001fe24ec6e8; epoch: 0; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f893b1ba00d (biased: 0x0000001fe24ec6e8; epoch: 0; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-1] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f893b1ba00d (biased: 0x0000001fe24ec6e8; epoch: 0; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-1] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f893b1ba00d (biased: 0x0000001fe24ec6e8; epoch: 0; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-2] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f893b1ba00d (biased: 0x0000001fe24ec6e8; epoch: 0; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-2] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007f893b1ba00d (biased: 0x0000001fe24ec6e8; epoch: 0; age: 1)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[5.855s][info][gc,heap,exit ] Heap
[5.855s][info][gc,heap,exit ]  garbage-first heap   total 131072K, used 24236K [0x0000000780000000, 0x0000000800000000)
[5.855s][info][gc,heap,exit ]   region size 1024K, 24 young (24576K), 2 survivors (2048K)
[5.855s][info][gc,heap,exit ]  Metaspace       used 14992K, capacity 15673K, committed 15744K, reserved 1062912K
[5.855s][info][gc,heap,exit ]   class space    used 1613K, capacity 1896K, committed 1920K, reserved 1048576K
```

### 总结

由输出结果可得，`线程-0`、`线程-1`、`线程-2`均可以获得偏向锁，只要保证曾经获取过偏向锁的线程是非活跃状态，即可实现重偏向。

但是，实际上，运行上述代码出现的结果可能有三种情况：

1. `线程-0`获得偏向锁，而`线程-1`、`线程-2`获得轻量级锁（不符合预期）
2. `线程-0`、`线程-1`获得偏向锁，而`线程-2`获得轻量级锁（不符合预期）
3. `线程-0`、`线程-1`、`线程-2`都获得偏向锁（符合预期）

这是因为线程执行完成，可能仍处于存活状态，在两个线程之间加一点等待时间，可提高复现上述结果的几率。

### 源码

[SynchronizedMarkWordTests#testBiasedLocking_Rebias](https://github.com/yihleego/synchronized-markword/blob/main/src/test/java/io/leego/test/SynchronizedMarkWordTests.java#L90)

## 轻量级锁重入

验证轻量级锁相关功能时，禁用偏向锁，防止干扰结果。

### 运行参数

- `-XX:-UseBiasedLocking`：禁用偏向锁。
- `-Xlog:gc*`：打印 GC 日志。

### 代码

```java
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
```

### 输出

```text
[0.034s][info][gc,heap] Heap region size: 1M
[0.037s][info][gc     ] Using G1
[0.037s][info][gc,heap,coops] Heap address: 0x0000000780000000, size: 2048 MB, Compressed Oops mode: Zero based, Oop shift amount: 3
[0.903s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.903s][info][gc,task      ] GC(0) Using 3 workers of 4 for evacuation
[0.908s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
[0.908s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 3.7ms
[0.908s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.7ms
[0.908s][info][gc,phases    ] GC(0)   Other: 0.4ms
[0.908s][info][gc,heap      ] GC(0) Eden regions: 14->0(74)
[0.908s][info][gc,heap      ] GC(0) Survivor regions: 0->2(2)
[0.908s][info][gc,heap      ] GC(0) Old regions: 0->2
[0.908s][info][gc,heap      ] GC(0) Humongous regions: 0->0
[0.908s][info][gc,metaspace ] GC(0) Metaspace: 9629K->9629K(1058816K)
[0.908s][info][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 14M->3M(128M) 4.930ms
[0.908s][info][gc,cpu       ] GC(0) User=0.02s Sys=0.00s Real=0.00s
Java Version: 11.0.16
Java VM: Java HotSpot(TM) 64-Bit Server VM
VM Options: -XX:-UseBiasedLocking -Xlog:gc* 

无锁时
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000001 (non-biasable; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007000098d5970 (thin lock: 0x00007000098d5970)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 重入获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007000098d5970 (thin lock: 0x00007000098d5970)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 重入释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007000098d5970 (thin lock: 0x00007000098d5970)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000001 (non-biasable; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[3.902s][info][gc,heap,exit ] Heap
[3.902s][info][gc,heap,exit ]  garbage-first heap   total 131072K, used 23227K [0x0000000780000000, 0x0000000800000000)
[3.902s][info][gc,heap,exit ]   region size 1024K, 22 young (22528K), 2 survivors (2048K)
[3.902s][info][gc,heap,exit ]  Metaspace       used 14975K, capacity 15665K, committed 15744K, reserved 1062912K
[3.902s][info][gc,heap,exit ]   class space    used 1612K, capacity 1894K, committed 1920K, reserved 1048576K
```

### 总结

轻量级锁可重入，且 Mark Word 符合预期。

### 源码

[SynchronizedMarkWordTests#testLightweightLocking_Reenter](https://github.com/yihleego/synchronized-markword/blob/main/src/test/java/io/leego/test/SynchronizedMarkWordTests.java#L130)

## 轻量级锁膨胀重量级锁

验证轻量级锁相关功能时，禁用偏向锁，防止干扰结果。

### 运行参数

- `-XX:-UseBiasedLocking`：禁用偏向锁。
- `-Xlog:gc*`：打印 GC 日志。

### 代码

```java
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
```

### 输出

```text
[0.015s][info][gc,heap] Heap region size: 1M
[0.021s][info][gc     ] Using G1
[0.021s][info][gc,heap,coops] Heap address: 0x0000000780000000, size: 2048 MB, Compressed Oops mode: Zero based, Oop shift amount: 3
[0.975s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.975s][info][gc,task      ] GC(0) Using 3 workers of 4 for evacuation
[0.980s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
[0.980s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 4.1ms
[0.980s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.5ms
[0.980s][info][gc,phases    ] GC(0)   Other: 0.3ms
[0.980s][info][gc,heap      ] GC(0) Eden regions: 14->0(74)
[0.980s][info][gc,heap      ] GC(0) Survivor regions: 0->2(2)
[0.980s][info][gc,heap      ] GC(0) Old regions: 0->2
[0.980s][info][gc,heap      ] GC(0) Humongous regions: 0->0
[0.980s][info][gc,metaspace ] GC(0) Metaspace: 9655K->9655K(1058816K)
[0.980s][info][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 14M->3M(128M) 4.954ms
[0.980s][info][gc,cpu       ] GC(0) User=0.01s Sys=0.00s Real=0.00s
[1.932s][info][gc,start     ] GC(1) Pause Young (Normal) (G1 Evacuation Pause)
[1.932s][info][gc,task      ] GC(1) Using 3 workers of 4 for evacuation
[1.936s][info][gc,phases    ] GC(1)   Pre Evacuate Collection Set: 0.0ms
[1.936s][info][gc,phases    ] GC(1)   Evacuate Collection Set: 3.2ms
[1.936s][info][gc,phases    ] GC(1)   Post Evacuate Collection Set: 0.6ms
[1.936s][info][gc,phases    ] GC(1)   Other: 0.1ms
[1.936s][info][gc,heap      ] GC(1) Eden regions: 11->0(30)
[1.936s][info][gc,heap      ] GC(1) Survivor regions: 2->1(2)
[1.936s][info][gc,heap      ] GC(1) Old regions: 2->4
[1.936s][info][gc,heap      ] GC(1) Humongous regions: 0->0
[1.936s][info][gc,metaspace ] GC(1) Metaspace: 11462K->11462K(1060864K)
[1.936s][info][gc           ] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 14M->4M(128M) 3.982ms
[1.936s][info][gc,cpu       ] GC(1) User=0.01s Sys=0.00s Real=0.01s
Java Version: 11.0.16
Java VM: Java HotSpot(TM) 64-Bit Server VM
VM Options: -XX:-UseBiasedLocking -Xlog:gc* 

无锁时
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000001 (non-biasable; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x000070000b555980 (thin lock: 0x000070000b555980)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x000000010f5d5f02 (fat lock: 0x000000010f5d5f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-1] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x000000010f5d5f02 (fat lock: 0x000000010f5d5f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-1] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x000000010f5d5f02 (fat lock: 0x000000010f5d5f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[4.612s][info][gc,heap,exit ] Heap
[4.612s][info][gc,heap,exit ]  garbage-first heap   total 131072K, used 13551K [0x0000000780000000, 0x0000000800000000)
[4.612s][info][gc,heap,exit ]   region size 1024K, 11 young (11264K), 1 survivors (1024K)
[4.612s][info][gc,heap,exit ]  Metaspace       used 14958K, capacity 15670K, committed 15744K, reserved 1062912K
[4.612s][info][gc,heap,exit ]   class space    used 1612K, capacity 1895K, committed 1920K, reserved 1048576K
```

### 总结

由于禁用了偏向锁，所以`线程-0`获取锁时为轻量级锁，而释放锁时为重量级锁。
这是因为`[线程-1]`尝试获取锁，发现`[线程-0]`已经持有锁了，于是将轻量级锁膨胀为重量级锁，并将`[线程-1]`挂起。
在`[线程-0]`释放锁时，发现锁已经变为重量级锁，会唤醒被挂起的`[线程-1]`。

轻量级锁只要存在竞争就会膨胀重量级锁，不会使用自旋，[详情请见 README 文档](../README.md)。

### 源码

[SynchronizedMarkWordTests#testLightweightLocking_Inflate](https://github.com/yihleego/synchronized-markword/blob/main/src/test/java/io/leego/test/SynchronizedMarkWordTests.java#L153)

## 重量级锁降级

验证重量级锁相关功能时，禁用偏向锁，防止干扰结果。

### 运行参数

- `-XX:-UseBiasedLocking`：禁用偏向锁。
- `-Xlog:gc*`：打印 GC 日志。

### 代码

```java
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
```

### 输出

```text
[0.035s][info][gc,heap] Heap region size: 1M
[0.041s][info][gc     ] Using G1
[0.041s][info][gc,heap,coops] Heap address: 0x0000000780000000, size: 2048 MB, Compressed Oops mode: Zero based, Oop shift amount: 3
[0.977s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.977s][info][gc,task      ] GC(0) Using 3 workers of 4 for evacuation
[0.983s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
[0.983s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 4.5ms
[0.983s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.7ms
[0.983s][info][gc,phases    ] GC(0)   Other: 0.4ms
[0.983s][info][gc,heap      ] GC(0) Eden regions: 14->0(74)
[0.983s][info][gc,heap      ] GC(0) Survivor regions: 0->2(2)
[0.983s][info][gc,heap      ] GC(0) Old regions: 0->2
[0.983s][info][gc,heap      ] GC(0) Humongous regions: 0->0
[0.983s][info][gc,metaspace ] GC(0) Metaspace: 9673K->9673K(1058816K)
[0.983s][info][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 14M->3M(128M) 5.686ms
[0.983s][info][gc,cpu       ] GC(0) User=0.01s Sys=0.00s Real=0.01s
[1.314s][info][gc,start     ] GC(1) Pause Young (Normal) (G1 Evacuation Pause)
[1.314s][info][gc,task      ] GC(1) Using 3 workers of 4 for evacuation
[1.318s][info][gc,phases    ] GC(1)   Pre Evacuate Collection Set: 0.0ms
[1.318s][info][gc,phases    ] GC(1)   Evacuate Collection Set: 3.4ms
[1.318s][info][gc,phases    ] GC(1)   Post Evacuate Collection Set: 0.4ms
[1.318s][info][gc,phases    ] GC(1)   Other: 0.1ms
[1.318s][info][gc,heap      ] GC(1) Eden regions: 9->0(29)
[1.318s][info][gc,heap      ] GC(1) Survivor regions: 2->1(2)
[1.318s][info][gc,heap      ] GC(1) Old regions: 2->4
[1.318s][info][gc,heap      ] GC(1) Humongous regions: 0->0
[1.318s][info][gc,metaspace ] GC(1) Metaspace: 11140K->11140K(1060864K)
[1.318s][info][gc           ] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 12M->4M(128M) 3.915ms
[1.318s][info][gc,cpu       ] GC(1) User=0.01s Sys=0.00s Real=0.00s
Java Version: 11.0.16
Java VM: Java HotSpot(TM) 64-Bit Server VM
VM Options: -XX:-UseBiasedLocking -Xlog:gc* 

无锁时
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000001 (non-biasable; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-0] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-7] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-7] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-6] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-6] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-5] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-5] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-4] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-4] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-3] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-3] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-2] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-2] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-1] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-1] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-9] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-9] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-8] 获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[线程-8] 释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

无竞争后
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000106556f02 (fat lock: 0x0000000106556f02)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

线程休眠一段时间后
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000001 (non-biasable; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[当前线程] 再次获取锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x00007000048b2f70 (thin lock: 0x00007000048b2f70)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[当前线程] 再次释放锁
java.lang.Object object internals:
OFF  SZ   TYPE DESCRIPTION               VALUE
  0   8        (object header: mark)     0x0000000000000001 (non-biasable; age: 0)
  8   4        (object header: class)    0x00001000
 12   4        (object alignment gap)    
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

[4.868s][info][gc,heap,exit ] Heap
[4.868s][info][gc,heap,exit ]  garbage-first heap   total 131072K, used 17538K [0x0000000780000000, 0x0000000800000000)
[4.868s][info][gc,heap,exit ]   region size 1024K, 15 young (15360K), 1 survivors (1024K)
[4.868s][info][gc,heap,exit ]  Metaspace       used 15026K, capacity 15733K, committed 16000K, reserved 1062912K
[4.868s][info][gc,heap,exit ]   class space    used 1612K, capacity 1895K, committed 1920K, reserved 1048576K
```

### 总结

首先创建了 10 个线程，将锁膨胀为重量级锁，当线程无竞争后，依然是重量级锁，此时锁还未被降级。
通过`sleep`使线程休眠一段时间后，发现锁已经被重置为无锁状态了。重新获取锁，则是轻量级锁。

说明重量级锁是可以被降级的，只是降级的需要一定前提，[详情请见 README 文档](../README.md)。

### 源码

[SynchronizedMarkWordTests#testLightweightLocking_Deflate](https://github.com/yihleego/synchronized-markword/blob/main/src/test/java/io/leego/test/SynchronizedMarkWordTests.java#L177)


