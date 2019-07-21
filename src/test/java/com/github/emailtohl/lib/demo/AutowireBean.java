package com.github.emailtohl.lib.demo;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

/**
 * 运行时，将对象注入到Spring的上下文容器中
 * 
 * @author HeLei
 */
public class AutowireBean {
	public static void main(String[] args) {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
			ctx.register(Conf.class);
			ctx.refresh();
			ctx.start();
			ctx.registerShutdownHook();

			Bean1 bean1 = ctx.getBean(Bean1.class);
			assert bean1 != null;
			
			Bean2 bean2 = null;
			try {
				bean2 = ctx.getBean(Bean2.class);
			} catch (NoSuchBeanDefinitionException e) {}
			assert bean2 == null;
			
			bean2 = new Bean2();
			assert bean2.bean1 == null;
			
			AutowireCapableBeanFactory factory = ctx.getAutowireCapableBeanFactory();
			factory.autowireBeanProperties(bean2, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
			
			assert bean2.bean1 == bean1;
			
			ctx.publishEvent(new CustEvent());
			
			ctx.stop();
		}
		System.out.println("运行结束");
	}
}

@Configuration
//@PropertySource("classpath:/com/myco/app.properties")
@ImportResource(/* "classpath:/com/myco/spring-security.xml" */)
@ComponentScan(basePackageClasses = AutowireBean.class)
class Conf {

}

@Component
class Bean1 {
}

class CustEvent extends ApplicationEvent {
	private static final long serialVersionUID = 5077340468201468652L;
	public CustEvent() {
		super("custEvent");
	}
}

@Component
class CustListener implements ApplicationListener<CustEvent> {
	@Override
	public void onApplicationEvent(CustEvent event) {
		System.out.println(event.getSource());
	}
}

class Bean2 {
	@Autowired
	Bean1 bean1;
	@Autowired
	CustListener custListener;
}