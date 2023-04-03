package com.zhgw.user;

import com.zhgw.springframework.ZhgwApplicationContext;
import com.zhgw.user.service.UserService;

/**
 * @Author:zhgw
 * @Date:16:54,2023/3/13
 */
public class ZhgwApplication {
  public static void main(String[] args) {
    ZhgwApplicationContext context = new ZhgwApplicationContext(AppConfig.class);
    UserService userService = (UserService) context.getBean("userService");
    userService.test();
    userService.dumplateTransaction();
  }
}
