# 开发工具包

根据以往项目经验，提取出最通用的库函数，并进行重构，特别是JPA的各类查询，可以将业务代码的查询编码简化到最小程度，如此可移植到各类项目上。

* 运行环境要求JDK8及以上版本。
* 以Spring为基础运行环境。
* 为避免包冲突，本项目Maven依赖scope为provided，若出现ClassNotFoundException异常，则需要查看是否欠缺相关依赖，主要依赖有jdbc、Spring、Spring-data-jpa、Jackson、log4j、Hibernate search、Hibernate envers等，具体可参考本项目pom.xml文件中的配置。
* 注意选取Hibernate、Hibernate Search以及Lucene相互兼容的版本在Hibernate search官网上会说明依赖的版本。

这里主要介绍日常项目中使用得最多的JPA动态查询库函数。
### 引入包
将包引入到项目中，以Maven本地依赖为例：
```xml
<dependency>
  <groupId>com.github.emailtohl</groupId>
  <artifactId>lib</artifactId>
  <version>2.0.0-RELEASE</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/lib/lib-2.0.0-RELEASE.jar</systemPath>
</dependency>
```
然后解决需要的底层依赖，以Spring boot项目为例，主要引入spring-boot-starter-data-jpa、hibernate-search-orm等。
### 编写实体类
实体类可以继承com.github.emailtohl.lib.jpa.BaseEntity，获得全局唯一性的id，且在增删改时收到对应的事件
