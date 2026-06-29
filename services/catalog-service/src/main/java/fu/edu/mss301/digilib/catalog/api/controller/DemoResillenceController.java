package fu.edu.mss301.digilib.catalog.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/demo")
public class DemoResillenceController {
    private int retryCount = 0;

    // API này cố tình delay 5 giây để test Timeout & Retry
    @GetMapping("/slow")
    public String slowEndpoint() throws InterruptedException {
        retryCount++;
        System.out.println("⏳ [Catalog] Đã nhận được request lần thứ: " + retryCount + " vào lúc: " + java.time.LocalTime.now());
        
        // Reset lại biến đếm sau 4 lần (1 gốc + 3 retry) để lần demo sau chạy lại từ đầu
        if (retryCount >= 4) {
            retryCount = 0;
        }

        Thread.sleep(5000);
        return "Success từ Catalog Service (nhưng rất chậm)";
    }

    // API riêng để test Bulkhead (Chỉ cho phép 1 request vào cùng lúc)
    @GetMapping("/bulkhead")
    @io.github.resilience4j.bulkhead.annotation.Bulkhead(name = "catalogBulkhead", fallbackMethod = "bulkheadFallback")
    public String bulkheadEndpoint() throws InterruptedException {
        System.out.println("[Catalog] Bắt đầu xử lý khoang tàu Bulkhead vào lúc: " + java.time.LocalTime.now());
        Thread.sleep(5000); // Ngâm 5 giây để mô phỏng đang bận xử lý
        return "Success! Khoang tàu Bulkhead đã xử lý xong.";
    }

    // Hàm Fallback khi Bulkhead đầy (Bị chặn ngay lập tức, không cần chờ)
    public String bulkheadFallback(Exception e) {
        System.out.println("[Catalog] Bulkhead đầy! Đã chặn 1 request vào lúc: " + java.time.LocalTime.now());
        return "Bulkhead Fallback: Hệ thống đang quá tải (hết khoang trống), vui lòng thử lại sau!";
    }
}
