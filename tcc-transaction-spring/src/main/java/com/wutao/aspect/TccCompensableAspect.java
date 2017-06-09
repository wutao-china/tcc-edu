package com.wutao.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

@Aspect
public class TccCompensableAspect implements Ordered {
	
	private int order = Ordered.HIGHEST_PRECEDENCE; // 最高优先级（值较低的那个有更高的优先级）
	
	public void setOrder(int order) {
		this.order = order;
	}

	@Pointcut("@annotation(com.wutao.annotation.Compensable)")
	public void compensableService(){
		
	}
	
	@Around("compensableService()")
	public Object around(ProceedingJoinPoint pjp) throws Throwable{
		Object[] args = pjp.getArgs();
		return pjp.proceed();
	}

	@Override
	public int getOrder() {
		// TODO Auto-generated method stub
		return order;
	}
}
