package com.wutao.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

@Aspect
public class TccTransactionContextAspect  implements Ordered{
	private int order = Ordered.HIGHEST_PRECEDENCE + 1; // 最高优先级（值较低的那个有更高的优先级）
	
	@Override
	public int getOrder() {
		// TODO Auto-generated method stub
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
	
    //@Pointcut("execution(public * *(org.mengyun.tcctransaction.api.TransactionContext,..))||@annotation(com.wutao.annotation.Compensable)")
	@Pointcut("@annotation(com.wutao.annotation.Compensable)")
	public void transactionContextCall(){
		
	}
	
	@Around("transactionContextCall()")
	public Object around(ProceedingJoinPoint pjp) throws Throwable{
		return pjp.proceed();
	}
}
