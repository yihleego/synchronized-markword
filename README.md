# Synchronized & Mark Word

本文旨在验证不同场景下通过`synchronized`关键字修饰的对象的对象头的 Mark Word 的情况。

外部依赖：

- [Java Object Layout (JOL)](https://openjdk.java.net/projects/code-tools/jol/)

## 覆盖 Java 版本

- OpenJDK 7
- OpenJDK 8
- OpenJDK 9
- OpenJDK 10
- OpenJDK 11
- OpenJDK 12
- OpenJDK 13
- OpenJDK 14
- OpenJDK 15
- OpenJDK 16
- OpenJDK 17
- OpenJDK 18

## 结论

### 偏向锁 Biased Locking

- 偏向锁可重入。
- 偏向锁会在竞争时候重置为无锁状态，这个过程称为偏向锁撤销，需要进入安全点（即STW）。
- 偏向锁撤销时，如果前一个线程是存活状态，则会升级为轻量级锁，如果前一个线程是非存活状态，则重偏向。

### 轻量级锁 Lightweight Locking

- 轻量级锁可重入。
- 两个线程竞争时