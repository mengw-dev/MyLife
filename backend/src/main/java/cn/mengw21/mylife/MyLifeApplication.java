package cn.mengw21.mylife;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MyLife 启动类
 * <p>
 * 只扫描 module 包下标注了 {@code @Mapper} 的接口，
 * 避免把 Service 接口误注册成 MyBatis Mapper。
 */
@SpringBootApplication
@MapperScan(basePackages = "cn.mengw21.mylife.module", annotationClass = Mapper.class)
public class MyLifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyLifeApplication.class, args);
    }
}
