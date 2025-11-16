package io.github.artemisia0.rp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@SpringBootApplication
@RequestMapping("/api")
public class Main {

  @RequestMapping("/hello")
  String hello() {
    return "Hi there from api!";
  }

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }

}

