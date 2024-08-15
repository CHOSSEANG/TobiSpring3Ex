//package com.example.spring_start.user.service;
//
//import org.springframework.beans.factory.FactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import java.lang.reflect.Proxy;
//
//public class TxProxyFactoryBean implements FactoryBean<Object> {
//    Object target;
//    PlatformTransactionManager transactionManager;
//    String pattern;
//    Class<?> serviceInterface;
//
//    public void setTarget(Object target) {
//        this.target = target;
//    }
//
//    public void setTransactionManager(PlatformTransactionManager transactionManager) {
//        this.transactionManager = transactionManager;
//    }
//
//    public void setPattern(String pattern) {
//        this.pattern = pattern;
//    }
//
//    public void setServiceInterface(Class<?> serviceInterface) {
//        this.serviceInterface = serviceInterface;
//    }
//
//    //FactoryBean 인터페이스 구현 메소드
//    public Object getObject() throws Exception {
//        TransactionHandler txHandler = new TransactionHandler();
//        txHandler.setTransactionManager(transactionManager);
//        txHandler.setPattern(pattern);
//        txHandler.setTarget(target);
//        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{serviceInterface}, txHandler);
//    }
//
//    public Class<?> getObjectType() {
//        return serviceInterface;
//    }
//
//    public boolean isSingleton() {
//        return false;
//    }
//}
