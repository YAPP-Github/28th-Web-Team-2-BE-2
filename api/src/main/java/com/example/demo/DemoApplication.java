package com.example.demo;

import com.example.demo.external.kakao.feign.KakaoMapClient;
import com.example.demo.external.kamis.feign.KamisClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {KamisClient.class, KakaoMapClient.class})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
