/*
 * ====================================================================
 * 龙果学院： www.roncoo.com （微信公众号：RonCoo_com）
 * 超级教程系列：《微服务架构的分布式事务解决方案》视频教程
 * 讲师：吴水成（水到渠成），840765167@qq.com
 * 课程地址：http://www.roncoo.com/course/view/7ae3d7eddc4742f78b0548aa8bd9ccdb
 * ====================================================================
 */
package com.wutao.intercepter;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.wutao.Exception.NoExistedTransactionException;
import com.wutao.Exception.OptimisticLockException;
import com.wutao.api.TransactionContext;
import com.wutao.api.TransactionStatus;
import com.wutao.common.MethodType;
import com.wutao.support.TransactionConfigurator;
import com.wutao.util.CompensableMethodUtils;
import com.wutao.util.ReflectionUtils;

/**
 * 可补偿事务拦截器。
 * Created by changmingxie on 10/30/15.
 */
public class CompensableTransactionInterceptor {

    static final Logger logger = Logger.getLogger(CompensableTransactionInterceptor.class.getSimpleName());

    /**
     * 事务配置器
     */
    private TransactionConfigurator transactionConfigurator;

    /**
     * 设置事务配置器.
     * @param transactionConfigurator
     */
    public void setTransactionConfigurator(TransactionConfigurator transactionConfigurator) {
        this.transactionConfigurator = transactionConfigurator;
    }

    /**
     * 拦截补偿方法.
     * @param pjp
     * @throws Throwable
     */
    public Object interceptCompensableMethod(ProceedingJoinPoint pjp) throws Throwable {

    	// 从拦截方法的参数中获取事务上下文
        TransactionContext transactionContext = CompensableMethodUtils.getTransactionContextFromArgs(pjp.getArgs());
        
        // 计算可补偿事务方法类型
        MethodType methodType = CompensableMethodUtils.calculateMethodType(transactionContext, true);
        
        logger.info("==>interceptCompensableMethod methodType:" + methodType.toString());

        switch (methodType) {
            case ROOT:
                return rootMethodProceed(pjp); // 主事务方法的处理(没有transactionContext参数)
            case PROVIDER:
                return providerMethodProceed(pjp, transactionContext); // 服务提供者事务方法处理
            default:
                return pjp.proceed(); // 其他的方法都是直接执行
        }
    }

    /**
     * 主事务方法的处理.
     * @param pjp
     * @throws Throwable
     */
    private Object rootMethodProceed(ProceedingJoinPoint pjp) throws Throwable {
    	logger.info("==>rootMethodProceed");

        transactionConfigurator.getTransactionManager().begin(); // 事务开始（创建事务日志记录，并在当前线程缓存该事务日志记录）

        Object returnValue = null; // 返回值
        try {
        	
        	logger.info("==>rootMethodProceed try begin");
            returnValue = pjp.proceed();  // Try (开始执行被拦截的方法，或进入下一个拦截器处理逻辑)
            logger.info("==>rootMethodProceed try end");
            
        } catch (OptimisticLockException e) {
        	logger.info("==>compensable transaction trying exception.");
            throw e; //do not rollback, waiting for recovery job
        } catch (Throwable tryingException) {
            logger.info("compensable transaction trying failed.");
            transactionConfigurator.getTransactionManager().rollback();
            throw tryingException;
        }

        logger.info("===>rootMethodProceed begin commit()");
        transactionConfigurator.getTransactionManager().commit(); // Try检验正常后提交(事务管理器在控制提交)：Confirm

        return returnValue;
    }

    /**
     * 服务提供者事务方法处理.
     * @param pjp
     * @param transactionContext
     * @throws Throwable
     */
    private Object providerMethodProceed(ProceedingJoinPoint pjp, TransactionContext transactionContext) throws Throwable {
    	
    	logger.info("==>providerMethodProceed transactionStatus:" + TransactionStatus.valueOf(transactionContext.getStatus()).toString());

        switch (TransactionStatus.valueOf(transactionContext.getStatus())) {
            case TRYING:
            	logger.info("==>providerMethodProceed try begin");
            	// 基于全局事务ID扩展创建新的分支事务，并存于当前线程的事务局部变量中.
                transactionConfigurator.getTransactionManager().propagationNewBegin(transactionContext);
                logger.info("==>providerMethodProceed try end");
                return pjp.proceed(); // 开始执行被拦截的方法，或进入下一个拦截器处理逻辑
            case CONFIRMING:
                try {
                	logger.info("==>providerMethodProceed confirm begin");
                	// 找出存在的事务并处理.
                    transactionConfigurator.getTransactionManager().propagationExistBegin(transactionContext);
                    transactionConfigurator.getTransactionManager().commit(); // 提交
                    logger.info("==>providerMethodProceed confirm end");
                } catch (NoExistedTransactionException excepton) {
                    //the transaction has been commit,ignore it.
                }
                break;
            case CANCELLING:
                try {
                	logger.info("==>providerMethodProceed cancel begin");
                    transactionConfigurator.getTransactionManager().propagationExistBegin(transactionContext);
                    transactionConfigurator.getTransactionManager().rollback(); // 回滚
                    logger.info("==>providerMethodProceed cancel end");
                } catch (NoExistedTransactionException exception) {
                    //the transaction has been rollback,ignore it.
                }
                break;
        }

        Method method = ((MethodSignature) (pjp.getSignature())).getMethod();

        return ReflectionUtils.getNullValue(method.getReturnType());
    }

}
