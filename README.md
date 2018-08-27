# 开发工具库项目介绍

根据我以往的项目经验，提取出最通用的库函数，特别是JPA的各类查询，使用本库，可以将业务代码的查询编码简化到最小程度，让业务代码更加简洁一致，本工具库可移植到各类项目上。

* 运行环境要求JDK8及以上版本。
* 以Spring为基础运行环境，最好使用Spring boot，可以自动导入很多依赖包。
* 本工具库为避免与业务项目发生依赖包的冲突，所以在Maven依赖配置上，scope大多是provided，使用时若出现ClassNotFoundException异常，则需要查看业务项目是否欠缺相关依赖，主要依赖有jdbc、Spring、Spring-data-jpa、Jackson、log4j2、Hibernate search、Hibernate envers等，具体可参考本项目pom.xml文件中的配置。
* 注意选取Hibernate、Hibernate Search以及Lucene相互兼容的版本，在Hibernate search官网上会说明依赖的版本。

## 1. 引入依赖包
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
## 2. JPA相关功能
数据层的编写往往是业务代码的重头，使用本工具库提供的JPA相关功能能将业务代码降低到最精简程度。
### 2.1 编写实体类
业务代码中的实体类可以继承com.github.emailtohl.lib.jpa.BaseEntity，他们具有统一的效果：
1. 获得全局唯一性的id，并以id为主键的equals&hashcode方法，用于鉴别实体的相等性
2. 做增删改操作时，com.github.emailtohl.lib.jpa.EntityListener会在Spring上下文中发布事件,只要Bean实现ApplicationListener&lt;BaseEntity&gt;即可收到事件
3. 实体具有版本管理功能，在乐观锁模式下，可让并发修改更为安全
4. 覆盖toString方法，序列化为json
### 2.2 EntityRepository
基础的JPA数据访问层，业务代码通过继承它来使用其功能：
```java
@Repository
class UserRepoImpl extends EntityRepository<User, Long> {
}
```
第一个泛型参数是实体类的类型，第二个泛型参数是实体类id的类型，EntityRepository会自动计算这两个泛型类，并写进entityClass和idClass两个域中，此外，EntityRepository还提供了线程安全的EntityManager。单独使用EntityRepository的价值不大，我们主要继承在它之上的扩展类。
### 2.3 QueryRepository
QueryRepository继承自EntityRepository，提供动态查询的能力，可以大幅缩减JPA的查询代码。
#### 2.3.1 简单使用方法
首先让业务类继承QueryRepository：
```java
@Repository
class UserRepoImpl extends QueryRepository<User, Long> {
}
```
然后将其注入到服务Bean中：
```java
@Service
@Transactional
public class UserServiceImpl {
  @Autowired
  private UserRepoImpl userRepo;

}
```
这样就可以使用QueryRepository中的动态查询功能了，如列表查询：
```java
@Override
public List<User> query(User example) {
  return userRepo.queryForList(example).stream().map(this::toTransient).collect(Collectors.toList());
}
```
当然，也可以使用分页查询：
```java
Page<User> page = userRepo.queryForPage(example, pageable);
```
这里pageable来自于spring data jpa：org.springframework.data.domain.Pageable，它的实例可以在控制层以注入方式获取到，如：
```java
@GetMapping("search")
public Paging<User> search(@RequestParam(required = false, defaultValue = "") String query, @PageableDefault(page = 0, size = 10, sort = { "id", "modifyDate" }, direction = Direction.DESC) Pageable pageable) {
  return userService.search(query, pageable);
}
```
当然也可以自行构建：
```java
Pageable pageable = PageRequest.of(0, 20);
```
> Spring Data JPA默认起始页从0开始

QueryRepository会分析参数，并将里面不为null的属性以AND的逻辑拼写成JPQL查询，例如在User实例里面的name="FOO",gender=Gender.MALE，那么就会进行如下查询：
```sql
SELECT u FROM User u WHERE LOWER(name) LIKE 'foo' AND u.gender='MALE'
```
若参数的关联关系不为null，且属性有值，则会进行连接查询，ManyToOne，OneToOne，Embedded会使用内连接查询，以User.city.name='ChongQing'为例，QueryRepository会将其解析为：
```sql
SELECT u FROM User u WHERE LOWER(u.city.name) LIKE 'chongqing'
```
如果是ManyToMany，OneToMany，ElementCollection关系，则会使用左连接查询，以User.roles.name="ADMIN"为例，其中roles属性是集合Set<Role>，QueryRepository会将其解析为：
```sql
SELECT u FROM User u LEFT JOIN u.roles r ON LOWER(r.name) LIKE 'admin'
```
#### 2.3.2 微调查询
##### 2.3.2.1 关于基本类型
从上面介绍的使用来看，实体类最好不要使用基本类型，因为基本类型有初始值，不能表达null，QueryRepository将忽略基本类型的初始值作为查询条件，如int类型会忽略0，double类型会忽略0.0，boolean类型会忽略false……，若确实需要将该基本类型的初始值作为查询条件，需在该属性上添加com.github.emailtohl.lib.jpa.InitialValueAsCondition注解，如：
```java
@InitialValueAsCondition
public int getAge() {
  return age;
}
```

##### 2.3.2.2 关于字符串
对于字符串的查询，默认使用LIKE，并忽略大小写，以属性name举例：
```java
public String getName() {
  return name;
}
```
若该属性存储值为"FOO"，则会被转成条件表达式：
```sql
LOWER(name) LIKE 'foo'
```
若需模糊查询，则应该由业务代码自行在值上添加通配符：'FOO%'。若一定要对字符串用相等做比较，可以在其属性上添加上com.github.emailtohl.lib.jpa.Instruction注解进行特殊说明，如：
```java
@Instruction(operator = Operator.EQ)
public String getName() {
    return name;
}
```
QueryRepository就会解析成条件表达式：
```sql
name='foo'
```
##### 2.3.2.3 其他条件比较
有的查询需要其他条件比较，如大于、小于、不为NULL等等，对于这些需求，就得在对应的属性上使用com.github.emailtohl.lib.jpa.Instruction注解，如：
```java
@Instruction(operator = Operator.GTE)
public BigDecimal getPrice() {
    return price;
}
```
若price属性中存储的值是100.00，则查询条件将会写成：
```sql
price >= 100.00
```
对于有的属性的查询，需要条件组合，以User中有生日属性为例：
```java
@Temporal(TemporalType.DATE)
public Date getBirthday() {
  return birthday;
}
```
为了在查询条件中表达出大于等于1980-01-01，小于1990-01-01的条件，可在User实体中添加两个瞬时属性（不映射到数据库字段的属性），并添加上Instruction注解：
```java
@Instruction(propertyName = "birthday", operator = Operator.GTE)
@Transient
public Date getStartDate() {
  return startDate;
}

@Instruction(propertyName = "birthday", operator = Operator.LT)
@Transient
public Date getEndDate() {
  return endDate;
}
```
在startDate属性中存储1980-01-01，会被转为：
```sql
birthday >= 1980-01-01
```
在endDate属性中存储1990-01-01，会被转为：
```sql
birthday < 1990-01-01
```
若不愿意在实体类中添加与数据库字段无关的属性，也可以新创建一个用作查询的类，添加上查询属性，再让其继承实体类，如：
```java
class UserForm extends User {
  @Instruction(propertyName = "birthday", operator = Operator.GTE)
  public Date startDate;
  @Instruction(propertyName = "birthday", operator = Operator.LT)
  public Date endDate;
}
```
另外若需表达：
```sql
name in ('foo', 'bar')
```
可创建一个查询属性：
```java
@Instruction(propertyName = "name", operator = Operator.IN)
@Transient
public Set<String> inNames = new HashSet<>();
```
在该属性中存储进"foo"、"bar"，就会被QueryRepository解析。
### 2.4 SearchRepository
SearchRepository继承自QueryRepository，提供了使用Hibernate Search的简便方法，它会分析实体类中注解了org.hibernate.search.annotations.Field或org.hibernate.search.annotations.IndexedEmbedded属性，并用作索引和搜索，使用方法很简单，首先让业务类继承它。
```java
@Repository
class UserRepoImpl extends SearchRepository<User, Long> {

}
```
然后使用其
```java
Page<E> search(String query, Pageable pageable);
```
或
```java
List<E> search(String query);
```
即可进行全文搜索。其中Pageable是Spring data JPA提供的查询类，可在控制层注入，前面已经有介绍。

> 需要注意的是，SearchRepository只提供字符串搜索功能，不支持数字、日期的大于、小于条件查询，对于Date、Number、Enumeration，在标注上@Field注解后，需要再添加上com.github.emailtohl.lib.jpa.StringBridgeCustomization注解，让该属性值作为字符串被索引查询。

### 2.5 AuditedRepository
AuditedRepository继承SearchRepository，能让业务代码简便地使用Hibernate envers功能，同样是先让业务类继承它：
```java
@Repository
class UserRepoImpl extends AuditedRepository<User, Long> {

}
```
使用方式如下：
#### 2.5.1 查询历史版本列表
使用AuditedRepository#getRevisions(ID id)接口查询某实体所有的历史版本信息，例如：
```java
List<Tuple<User>> getRevisions(Long id);
```
其中在Tuple<E>.DefaultRevisionEntity.id即为修订版本id。
#### 2.5.2 获取历史版本快照
结合修订版id和实体id，可通过AuditedRepository#getEntityAtRevision(ID id, Number revision)接口获取该实体当初的快照，如：
```java
User getEntityAtRevision(Long id, Number revision);
```

> void rollback(ID id, Number revision) 接口是将该实体回滚到当初的快照上，但是不能用于继承在BaseEntity的实体上，这是因为BaseEntity实体中的createDates属性是不能修改的，所有无法还原。

### 2.6 结合Spring Data JPA
在Spring环境中，我们一般都需要使用便捷的Spring data JPA，但我们可能同时需要使用本库中的全文搜索功能，这里介绍如何将两者结合起来使用。
#### 2.6.1 自定义接口
首先我们先编写自定义接口UserRepoCust：
```java
interface UserRepoCust extends SearchInterface<User, Long> {
}
```
SearchInterface接口具有全文搜索接口Page<E> search(String query, Pageable pageable)和List<E> search(String query)，所以UserRepoCust就不必重复定义全文搜索接口了。
#### 2.6.2 创建实现类
然后再编写一个实现类：
```java
@Repository
class UserRepoImpl extends SearchRepository<User, Long> implements UserRepoCust {

}
```
该实现类继承了SearchRepository，所以就具有全文搜索功能了，因为SearchRepository实现了SearchInterface接口，所以UserRepoImpl也就实现UserRepoCust。

> 注意UserRepoImpl的命名是有意义的，它虽然实现了UserRepoCust接口，但是并没有叫“UserRepoCustImpl”，而是叫做“UserRepoImpl”，Spring Data JPA会将命名为“UserRepoImpl”的Bean绑定到“UserRepo”接口上。

#### 2.6.3 创建基于Spring Data JPA的接口
现在创建UserRepo接口，为了纳入Spring Data JPA管理，让其继承JpaRepository接口：
```java
interface UserRepo extends JpaRepository<User, Long>, UserRepoCust {
	User findByEmail(String email);
}
```
可以看到UserRepo同时继承了我们自定义的UserRepoCust接口，所以UserRepo不仅可以使用Spring Data JPA提供的若干便捷方法外，同时也具有自定义的全文搜索功能了。

## 3 标准服务StandardService
业务代码中经常编写Service层的增删查改样板功能，若不加约束则会写得风格各异，但如果让这些Service代码都继承StandardService，则风格会非常统一且容易扩展。
### 3.1 CRUD接口
根据我自己最佳实践，除删除接口外，新增、修改均有结果返回，这样不仅可以让调用者能立即获取到结果信息（如创建后的id），同时有利于缓存的定义:
```java
String CACHE_NAME = "cache_name";

@CachePut(value = CACHE_NAME, key = "#result.id")
E create(E entity);

@Cacheable(value = CACHE_NAME, key = "#root.args[0]", condition = "#result != null")
E read(ID id);

@CachePut(value = CACHE_NAME, key = "#root.args[0]", condition = "#result != null")
E update(ID id, E newEntity);

@CacheEvict(value = CACHE_NAME, key = "#root.args[0]")
void delete(ID id);
```
### 3.2 返回瞬时实例
StandardService中定义了
```java
abstract E toTransient(E entity);
```
以及
```java
abstract E transientDetail(E entity);
```
两个抽象方法需要业务代码自行实现，这是考虑到读取出来的实体对象具有持久化状态，若出了事务层后再被调用懒加载的属性，则会引起LazyInitializationException，所以需要在返回前，将数据转存到瞬态对象上。其中toTransient主要用于列表，对转存的数据进行浅拷贝，而transientDetail主要用于详情，对转存的数据进行深拷贝，具体需要拷贝什么内容，程度有多深，需由业务代码自行确定。
### 3.3 参数校验
javax.validation.constraints中的校验可以在切面中完成，也可以在业务代码中进行，例如在create方法的入口处使用StandardService#validate(E entity)，若不满足条件的，则会抛出校验异常。
### 3.4 裁剪字符串前后空格
StandardService#trimStringProperty(Object o)，能将参数的字符串属性（符合JavaBean属性定义）前后空白裁剪，在持久化数据前调用它，可以对字符串数据进行过滤。
### 3.5 判断字符串是否为空
业务代码经常需要判断字符串是否有效，所以StandardService#hasText(String text)提供了字符串判空的功能，null和""均返回false。
### 3.6 当前用户信息
在StandardService中有一个CURRENT_USER_INFO静态域，它是ThreadLocal<String>类型，业务代码中，若需要获取当前访问用户，可在此域中查找。当然，使用它的前提是在过滤器层统一为其注入当前用户信息，参考代码如下：
```java
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
  String username = "anonymous";
  Authentication auth = SecurityContextHolder.getContext().getAuthentication();
  if (auth != null && StringUtils.hasText(auth.getName())) {
    username = auth.getName();
  }
  StandardService.CURRENT_USER_INFO.set(username);
  try {
    chain.doFilter(request, response);
  } finally {
    StandardService.CURRENT_USER_INFO.remove();
  }
}
```
## 4 文件搜索
com.github.emailtohl.lib.lucene.FileSearch具备文件内容的搜索功能，它使用org.mozilla.intl.chardet.nsDetector自动识别文件的编码格式，再利用Lucene对文件的内容进行搜索。

首先，在构造时，需要传入Lucene的Directory作为索引的存储仓库，可以是基于内存的RAMDirectory，也可以是基于文件系统的FSDirectory。

其次，对需要搜索的目录进行索引，即调用FileSearch#index(File searchDir)方法。

最后，使用List<Document> query(String queryString)或Page<Document> query(String queryString, Pageable pageable)进行搜索，返回的结果是Lucene的Document信息，通过FileSearch.FILE_PATH获取到搜索到的文件目录，用户程序可自行转换为前端所需的数据结构。

> 被搜索的目录下若有新的文件添加、修改或删除时，需更新索引，相应的方法是：addIndex(File file)、updateIndex(File file)、deleteIndex(File file)。

## 5 最后
本lib库是我自己经验的总结，它提取了日常开发中最常用的功能，可做业务代码的基础库使用。这里面还有一个我自己实现的RSA加密算法，虽然是按照算法原理进行开发的，但是并未经过行业验证，所以这里就不做推荐了。
