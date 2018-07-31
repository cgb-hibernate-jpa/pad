# 开发工具包

重构常用库函数，特别是JPA的常用查询，可以将查询查询的编码简化到最小程度，如此可移植到各类项目上。

* 运行环境要求JDK8以上版本。

* JPA库的运行需要在Spring环境下，@PersistenceContext会自动注入线程安全的EntityManager。

* 为了不引入重复包，项目依赖范围为provided，使用本包需自行添加Spring/JPA/Hibernate等依赖包，但需注意选取Hibernate、Hibernate Search以及Lucene相互兼容的版本，具体可参考本项目pom.xml文件中的配置。