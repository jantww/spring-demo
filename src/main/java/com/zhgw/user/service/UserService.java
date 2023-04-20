package com.zhgw.user.service;

import com.zhgw.springframework.*;

/**
 * @Author:zhgw
 * @Date:18:15,2023/3/13
 */
@Component
public class UserService implements BeanNameAware, ApplicationContextAware {

    private ZhgwApplicationContext context;

    private String beanName;

    @Autowried
    private OrderService orderService;

    @Transactional
    public void test() {
        System.out.println("hello spring!");
        System.out.println(context);
        System.out.println(beanName);
        System.out.println(orderService);
    }

    @Transactional
    public void duplicateTransaction() {
        System.out.println("dumplateTransaction!");
    }

    @Override
    public void setApplicationContext(ZhgwApplicationContext applicationContext) {
        context = applicationContext;
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

}
