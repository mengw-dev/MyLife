package cn.mengw21.mylife;

import cn.mengw21.mylife.common.api.ApiResponse;
import cn.mengw21.mylife.common.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试专用接口 —— 只存在于测试类路径，不会打进生产包。
 * <p>
 * 用来验证全局异常处理与参数校验是否真的通过 HTTP 链路生效。
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestScenarioController {

    /**
     * 业务异常链路（BusinessException → 统一响应体）
     */
    @GetMapping("/business-error")
    public ApiResponse<Void> businessError() {
        throw new BusinessException("用户名已存在");
    }

    /**
     * @Valid 参数校验链路（MethodArgumentNotValidException → fieldErrors）
     */
    @PostMapping("/validate")
    public ApiResponse<Void> validate(@Valid @RequestBody ValidateDTO dto) {
        return ApiResponse.success(null);
    }

    public record ValidateDTO(@NotBlank(message = "标题不能为空") String title) {
    }
}