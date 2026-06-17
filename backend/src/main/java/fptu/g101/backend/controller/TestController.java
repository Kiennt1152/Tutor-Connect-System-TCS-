package fptu.g101.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
// Chú ý: Cho phép cổng 3000 của Frontend gọi sang cổng 8080 của Backend công khai
@CrossOrigin(origins = "http://localhost:3000")
public class TestController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Kết nối thành công rồi bạn ơi! Backend Spring Boot chào Frontend Vite nhé 🎉";
    }
}