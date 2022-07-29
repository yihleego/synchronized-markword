# Synchronized & Mark Word

本文旨在验证不同场景下通过`synchronized`关键字修饰的对象的对象头的 Mark Word 的情况。

[OpenJDK JOL (Java Object Layout)](https://openjdk.java.net/projects/code-tools/jol/) 是用于分析 JVM 中对象布局的工具。
通过使用 Unsafe、JVMTI 和 Serviceability Agent (SA) 来解码实际的对象布局、占用空间和引用。

## 各版本测试结果

- [OpenJDK 7](docs/jdk7.md)
- [OpenJDK 8](docs/jdk8.md)
- [OpenJDK 9](docs/jdk9.md)
- [OpenJDK 10](docs/jdk10.md)（结果同 OpenJDK 9）
- [OpenJDK 11](docs/jdk11.md)（结果同 OpenJDK 9）
- [OpenJDK 12](docs/jdk12.md)（结果同 OpenJDK 9）
- [OpenJDK 13](docs/jdk13.md)（结果同 OpenJDK 9）
- [OpenJDK 14](docs/jdk14.md)（结果同 OpenJDK 9）
- [OpenJDK 15](docs/jdk15.md)
- [OpenJDK 16](docs/jdk16.md)（结果同 OpenJDK 15）
- [OpenJDK 17](docs/jdk17.md)（结果同 OpenJDK 15）
- [OpenJDK 18](docs/jdk18.md)（结果同 OpenJDK 15）

## 总结

### 偏向锁 Biased Locking

- 偏向锁可重入
- 偏向锁在竞争时候，会被重置为无锁状态，这个过程称为偏向锁撤销
- 偏向锁撤销需要进入`SafePoint`安全点，即会发送 Stop The World
- 获取偏向锁时，如果上一个持有该偏向锁的线程是存活状态，则会升级为轻量级锁
- 获取偏向锁时，如果上一个持有该偏向锁的线程是非存活状态，则会重偏向
- 在 Java 9 及之后的版本中，没有线程持有偏向锁时，调用`System.gc()`会重置 Mark Word
- 在 Java 15 及之后的版本中，偏向锁被移除

### 轻量级锁 Lightweight Locking

- 轻量级锁可重入
- 多个线程竞争时，轻量级锁膨胀为重量级，并不会自旋

### 重量级锁 Heavyweight Locking

- 重量级锁可重入
- 