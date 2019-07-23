package com.github.emailtohl.lib.demo;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
			
			CustListener listener = ctx.getBean(CustListener.class);
			assert listener != null;
			
			Abean abean = null;
			try {
				abean = ctx.getBean(Abean.class);
			} catch (NoSuchBeanDefinitionException e) {}
			assert abean == null;
			
			abean = new Abean();
			assert abean.custListener == null;
			
			ctx.getAutowireCapableBeanFactory().autowireBean(abean);
			assert abean.custListener == listener;
			ctx.getBeanFactory().registerSingleton(abean.getClass().getSimpleName(), abean);
			
			Abean fromCtx = ctx.getBean(Abean.class);
			assert fromCtx == abean;
			
			ctx.publishEvent(new CustEvent());
			
			ctx.stop();
		}
		System.out.println("运行结束");
	}
}

@Configuration
//@PropertySource("classpath:/com/myco/app.properties")
//@ImportResource("classpath:/com/myco/spring-security.xml")
//@ComponentScan(basePackageClasses = AutowireBean.class)
class Conf {
	@Bean
	public CustListener custListener() {
		return new CustListener();
	}
}

class CustEvent extends ApplicationEvent {
	private static final long serialVersionUID = 5077340468201468652L;
	public CustEvent() {
		super("custEvent");
	}
}

//@Component
class CustListener implements ApplicationListener<CustEvent> {
	@Override
	public void onApplicationEvent(CustEvent event) {
		System.out.println(event.getSource());
	}
}

class Abean {
	@Autowired
	CustListener custListener;
}