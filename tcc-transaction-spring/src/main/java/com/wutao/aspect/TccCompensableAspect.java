package com.wutao.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

@Aspect
public class TccCompensableAspect implements Ordered {
	
	@Pointcut("@annotation(com.wutao.annotation.Compensable)")
	public void compensableService(){
		
	}
	
	@Around("compensableService()")
	public void around(ProceedingJoinPoint pjp){
		System.out.println("=====>测试");
	}

	@Override
	public int getOrder() {
		// TODO Auto-generated method stub
		return 0;
	}
}
