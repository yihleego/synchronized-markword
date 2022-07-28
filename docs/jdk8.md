运行环境：Java HotSpot(TM) 64-Bit Server VM

##  

-XX:BiasedLockingStartupDelay=0
-XX:-UseBiasedLocking
-XX:+PrintGCDetails
-Xlog:gc*
-Djdk.attach.allowAttachSelf

rebiasing/inflation in ...

https://github.com/openjdk/jdk/blob/jdk8-b120/hotspot/src/share/vm/runtime/synchronizer.cpp#L1498
https://openjdk.org/jeps/8183909
https://zhuanlan.zhihu.com/p/28505703
https://github.com/farmerjohngit/myblog/issues/14