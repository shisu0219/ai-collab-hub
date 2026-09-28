package com.qll.ucch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 人工智能学院双创平台 —— 启动类。
 *
 * 平台定位：面向在校学生与老师，搭建师生共创、项目对接的线上桥梁。
 * 学生发布合作项目、浏览老师需求；老师发布用人需求、浏览学生项目；
 * 管理员负责账户与内容审核。
 *
 * 启动方式：
 *   本地开发：直接跑这个 main 方法，或 mvn spring-boot:run
 *   服务器  ：java -jar AiCollabHub.jar --spring.profiles.active=prov
 *
 * 接口文档：http://localhost:28848/doc.html
 *
 * @author 人工智能学院双创平台
 */
@SpringBootApplication
@MapperScan("com.qll.ucch.mapper")
@EnableTransactionManagement
public class AiCollabHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCollabHubApplication.class, args);
        System.out.println("""

                ==========================================================
                  人工智能学院双创平台 启动完成
                  接口文档: http://localhost:28848/doc.html
                ==========================================================
                """);
    }
}
