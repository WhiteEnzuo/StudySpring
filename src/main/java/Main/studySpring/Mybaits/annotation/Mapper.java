package Main.studySpring.Mybaits.annotation;

import Main.studySpring.Spring.annotation.Bean;

import java.lang.annotation.*;

/**
 * @Classname Annotation
 * @Description
 * @Version 1.0.0
 * @Date 2023/4/26 12:52
 * @Created by Enzuo
 */
@Bean
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Mapper {
}
