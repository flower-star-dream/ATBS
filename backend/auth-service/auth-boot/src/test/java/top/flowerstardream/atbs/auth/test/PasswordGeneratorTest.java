package top.flowerstardream.atbs.auth.test;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成测试类
 * @Author: 花海
 * @Date: 2026/03/29
 * @Description: 用于生成 BCrypt 加密密码的测试工具类
 */
public class PasswordGeneratorTest {

    /**
     * 生成加密密码
     * 使用与 SecurityConfig 中相同的 BCryptPasswordEncoder(12) 配置
     */
    @Test
    public void generatePassword() {
        // 创建与 SecurityConfig 相同配置的密码编码器
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        // 要加密的明文密码
        String rawPassword = "Atbs20260329";

        // 生成加密密码
        String encodedPassword = encoder.encode(rawPassword);

        // 输出结果
        System.out.println("============================================");
        System.out.println("明文密码: " + rawPassword);
        System.out.println("加密密码: " + encodedPassword);
        System.out.println("============================================");

        // 验证密码是否匹配
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("验证结果: " + matches);
    }

    /**
     * 批量生成多个加密密码
     */
    @Test
    public void generateMultiplePasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        String[] passwords = {"123456", "admin123", "password", "test123"};

        System.out.println("============================================");
        System.out.println("批量生成加密密码:");
        System.out.println("============================================");

        for (String rawPassword : passwords) {
            String encodedPassword = encoder.encode(rawPassword);
            System.out.println("明文: " + rawPassword + " -> 密文: " + encodedPassword);
        }
    }

    /**
     * 验证密码是否匹配
     */
    @Test
    public void verifyPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        String rawPassword = "123456";
        // 这里可以放入数据库中的加密密码进行验证
        String storedPassword = "$2a$12$LPL..."; // 替换为实际的加密密码

        boolean matches = encoder.matches(rawPassword, storedPassword);
        System.out.println("密码验证结果: " + matches);
    }
}
