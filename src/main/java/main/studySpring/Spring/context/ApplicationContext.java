package main.studySpring.Spring.context;

import main.studySpring.Spring.annotation.bean.Bean;
import main.studySpring.Spring.bean.BeanFactory;
import main.studySpring.Spring.inteceptor.context.HttpInterceptorContext;
import main.studySpring.utils.annotation.AnnotationUtils;
import main.studySpring.utils.yaml.YamlLoader;
import lombok.Data;

import java.io.File;
import java.util.Map;
import java.util.Objects;

/**
 * @Classname ApplicationContext
 * @Description
 * @Version 1.0.0
 * @Date 2023/4/19 20:34
 * @Created by Enzuo
 */
@Data

public class ApplicationContext {
    private BeanFactory beanFactory;
    private Class<?> ApplicationClazz;
    private Map<String, Object> config;
    private final HttpInterceptorContext interceptorContext;
    private String classpath;

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public ApplicationContext(Class<?> clazz) {
        config = YamlLoader.getLoad(clazz);
        beanFactory = new BeanFactory();
        interceptorContext = new HttpInterceptorContext();
        ApplicationClazz = clazz;
    }


    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public <T> T getBean(Class<T> clazz) {
        return (T) beanFactory.getBean(clazz);
    }

    public void initContext(Class<?> clazz) {
        if (beanFactory == null) beanFactory = new BeanFactory();
        String[] split = clazz.getName().split("\\.");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length - 1; i++) {
            sb.append(split[i]).append(".");
        }
        if (sb.charAt(sb.length() - 1) == '.') sb.deleteCharAt(sb.length() - 1);
        classpath = sb.toString();
        String path = Objects.requireNonNull(clazz.getResource("")).getPath();
        initBean(new File(path));

    }

    private void initBean(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            assert files != null;
            for (File pathFile : files) {
                if (pathFile.isDirectory()) {
                    initBean(pathFile);
                    continue;
                }
                if (pathFile.getName().contains(".class")) {
                    String className = pathFile.getPath().substring(pathFile.getPath().indexOf(classpath)).replace(".class", "").replace("\\", ".");
                    try {

                        Class<?> obj = Class.forName(className);
                        if (!isBean(obj)) {
                            continue;
                        }
                        if (obj.isAnnotation()) {
                            continue;
                        }
                        if (obj.isInterface()) {
                            beanFactory.putBean(obj, null);
                            //执行接口方法
                            continue;
                        }
                        if (!obj.isPrimitive()) {

                            Class<?>[] interfaces = obj.getInterfaces();
                            if (interfaces.length > 0) {
                                beanFactory.putBean(interfaces[0], obj.newInstance());
                            } else {
                                beanFactory.putBean(obj, obj.newInstance());
                            }
                        }
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private boolean isBean(Class<?> clazz) {
        return AnnotationUtils.isAnnotation(clazz, Bean.class, null);

    }


}
