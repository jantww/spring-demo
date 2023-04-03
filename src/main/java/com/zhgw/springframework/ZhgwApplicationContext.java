package com.zhgw.springframework;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @Author:zhgw
 * @Date:15:38,2023/3/13
 */

public class ZhgwApplicationContext {
    private Class configClass;
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private Map<String, Object> singletons = new HashMap<>();


    public ZhgwApplicationContext(Class configClass) {
        this.configClass = configClass;
        scan(configClass);
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            createBean(entry.getKey(), entry.getValue());
        }
    }

    private void scan(Class configClass) {
        //获取扫描路径
        if(configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan scan = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = scan.value();
            path = path.replace(".", "/");
            ClassLoader classLoader = this.getClass().getClassLoader();
            URL url = classLoader.getResource(path);
            File file = new File(url.getFile());
            List<File> readyFiles = new ArrayList<>();
            checkFile(file, readyFiles);
            //加载类对象，判断bean定义
            readyFiles.stream().forEach(r -> {
                String absolutePath = r.getPath();
                String classPath = absolutePath.substring(absolutePath.lastIndexOf("com"), absolutePath.indexOf(".class")).replace("/", ".");
                try {
                    Class<?> clazz = classLoader.loadClass(classPath);
                    if(clazz.isAnnotationPresent(Component.class)) {
                        String beanName = "";
                        Component component = clazz.getAnnotation(Component.class);
                        BeanDefinition beanDefinition = new BeanDefinition();
                        if(clazz.isAnnotationPresent(Lazy.class)) {
                            beanDefinition.setLazy(true);
                        }

                        beanDefinition.setType(clazz);
                        if (clazz.isAnnotationPresent(Scope.class)) {
                            Scope scope = clazz.getAnnotation(Scope.class);
                            beanDefinition.setScope(scope.value());
                        }else {
                            beanDefinition.setScope("singleton");
                        }
                        beanName = component.name().isEmpty() ? Introspector.decapitalize(clazz.getSimpleName()) : component.name();

                        beanDefinitionMap.put(beanName, beanDefinition);
                    }


                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void checkFile(File file, List<File> files) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            Arrays.stream(childFiles).forEach(c -> {
                checkFile(c, files);
            });
        }else {
            files.add(file);
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class<?> clazz = beanDefinition.getType();
        Object o = null;
        try {
            o = clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        for (Field field : clazz.getDeclaredFields()) {
            if(field.isAnnotationPresent(Autowried.class)) {
                field.setAccessible(true);
                String name = field.getName();
                Object value = getBean(name);
                try {
                    field.set(o, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        if(o instanceof ApplicationContextAware) {
            ((ApplicationContextAware) o).setApplicationContext(this);
        }

        if(o instanceof BeanNameAware) {
            ((BeanNameAware) o).setBeanName(beanName);
        }
        boolean isTransaction = false;
        Enhancer enhancer = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if(method.isAnnotationPresent(Transactional.class)) {
                isTransaction = true;
                enhancer = new Enhancer();
                enhancer.setSuperclass(clazz);
                Object target = o;
                enhancer.setCallback(new MethodInterceptor() {
                    @Override
                    public Object intercept(Object proxy, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                        System.out.println("开启事务...");
                        Object result = method.invoke(target, objects);
                        System.out.println("提交事务...");
                        return result;
                    }
                });

            }
        }
        if (isTransaction) {
            o = enhancer.create();
        }

        if(!beanDefinition.isLazy() && beanDefinition.getScope().equals("singleton")) {
            singletons.put(beanName, o);
        }
        return o;
    }

    public Object getBean(String beanName) {
        Object o = null;
        //获取beanDefinition数据:
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (null == beanDefinition) {
            throw new RuntimeException("No bean definition!");
        }
        if(beanDefinition.getScope().equals("singleton") && !beanDefinition.isLazy()) {
            o = singletons.get(beanName);
            if(null == o) {
                o = createBean(beanName, beanDefinition);
                singletons.put(beanName, o);
            }
        }else {
            o = createBean(beanName, beanDefinition);
        }

        return o;
    }
}

