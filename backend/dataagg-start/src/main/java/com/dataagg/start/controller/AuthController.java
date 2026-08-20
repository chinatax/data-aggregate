package com.dataagg.start.controller;

import com.dataagg.common.dto.*;
import com.dataagg.common.entity.User;
import com.dataagg.common.result.Result;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.service.MailService;
import com.dataagg.start.util.JwtUtil;
import com.dataagg.start.util.WechatUtil;
import com.dataagg.service.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WechatUtil wechatUtil;

    @Autowired
    private MailService mailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EMAIL_VERIFY_PREFIX = "email:verify:";
    private static final String WECHAT_LOGIN_PREFIX = "wechat:login:";
    private static final int VERIFY_CODE_TTL = 5; // 分钟
    private static final Random RANDOM = new Random();

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest req) {
        String username = req.getUsername();
        String password = req.getPassword();
        String email = req.getEmail();

        if (username == null || username.trim().isEmpty() || username.length() < 3 || username.length() > 50) {
            return Result.error("用户名需3-50位");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return Result.error("用户名只允许字母、数字、下划线");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码至少6位");
        }
        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return Result.error("邮箱格式不正确");
        }

        if (userService.getByUsername(username) != null) {
            return Result.error("用户名已被注册");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email.trim());
        user.setNickname(req.getNickname() != null ? req.getNickname().trim() : username.trim());
        user.setRole("USER");
        user.setStatus("INACTIVE");
        userService.save(user);

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        redisTemplate.opsForValue().set(EMAIL_VERIFY_PREFIX + email, code, VERIFY_CODE_TTL, TimeUnit.MINUTES);
        mailService.sendVerificationCode(email, code);

        return Result.success();
    }

    @PostMapping("/verify-email")
    public Result<Void> verifyEmail(@RequestBody VerifyEmailRequest req) {
        String email = req.getEmail();
        String code = req.getCode();

        String storedCode = redisTemplate.opsForValue().get(EMAIL_VERIFY_PREFIX + email);
        if (storedCode == null) {
            return Result.error("验证码已过期或不存在");
        }
        if (!storedCode.equals(code)) {
            return Result.error("验证码错误");
        }

        User user = userService.getInactiveByEmail(email);
        if (user == null) {
            return Result.error("用户不存在或已激活");
        }
        user.setStatus("NORMAL");
        userService.updateById(user);

        redisTemplate.delete(EMAIL_VERIFY_PREFIX + email);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        String username = req.getUsername();
        String password = req.getPassword();

        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        if (!"NORMAL".equals(user.getStatus())) {
            return Result.error("账号未激活，请先验证邮箱");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        String token = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        return Result.success(buildLoginResponse(user, token, refreshToken));
    }

    @GetMapping("/wechat/qrcode")
    public Result<Map<String, String>> getWechatQrcode(HttpServletRequest request) {
        String state = UUID.randomUUID().toString().replace("-", "");
        String baseUrl = request.getHeader("X-Forwarded-Host");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = request.getHeader("Host");
        }
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) scheme = "http";
        String scanUrl = scheme + "://" + baseUrl + "/api/auth/wechat/scan?state=" + state;
        Map<String, String> result = new HashMap<>();
        result.put("authUrl", scanUrl);
        result.put("state", state);
        return Result.success(result);
    }

    @GetMapping("/wechat/scan")
    public String wechatScan(@RequestParam String state) {
        String oauthUrl = wechatUtil.getOAuth2AuthorizeUrl(state);
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">"
                + "<meta name=\"referrer\" content=\"no-referrer\">"
                + "<title>微信登录</title>"
                + "<style>"
                + "*{margin:0;padding:0;box-sizing:border-box}"
                + "body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:#f5f7fa;min-height:100vh;padding:20px}"
                + ".card{background:#fff;border-radius:16px;padding:32px 24px;margin:0 auto;text-align:center;max-width:360px;width:100%;box-shadow:0 4px 20px rgba(0,0,0,0.08)}"
                + "h2{font-size:20px;color:#1a1a1a;margin-bottom:8px}"
                + ".sub{font-size:14px;color:#999;margin-bottom:20px}"
                + ".section-title{font-size:14px;color:#666;margin-bottom:12px;font-weight:500}"
                + ".qr-box{border:1px solid #e0e0e0;border-radius:8px;padding:8px;margin-bottom:20px;display:inline-block}"
                + ".qr-box img{width:180px;height:180px;display:block}"
                + ".divider{display:flex;align-items:center;margin:20px 0;color:#ccc;font-size:12px}"
                + ".divider::before,.divider::after{content:'';flex:1;border-bottom:1px solid #eee}"
                + ".divider span{margin:0 12px}"
                + ".btn{display:block;width:100%;padding:14px;background:#07c160;color:#fff;border:none;border-radius:8px;font-size:16px;font-weight:500;cursor:pointer;text-decoration:none}"
                + ".btn:active{opacity:0.85}"
                + "</style></head><body><div class=\"card\">"
                + "<h2>微信授权登录</h2>"
                + "<p class=\"sub\">数据聚合智能AI</p>"
                + "<p class=\"section-title\">第一步：长按识别关注测试号</p>"
                + "<div class=\"qr-box\">"
                + "<img src=\"http://3ks2u.com/test-qrcode.jpg\" alt=\"关注测试号\">"
                + "</div>"
                + "<div class=\"divider\"><span>关注后</span></div>"
                + "<p class=\"section-title\">第二步：点击下方按钮授权登录</p>"
                + "<a class=\"btn\" href=\"" + oauthUrl + "\">确认登录</a>"
                + "</div></body></html>";
    }

    @GetMapping("/wechat/check")
    public Result<Map<String, Object>> wechatCheck(@RequestParam String state) {
        String token = redisTemplate.opsForValue().get(WECHAT_LOGIN_PREFIX + state);
        if (token != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("success", true);
            redisTemplate.delete(WECHAT_LOGIN_PREFIX + state);
            return Result.success(result);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        return Result.success(result);
    }

    @GetMapping("/wechat/callback")
    public String wechatCallback(@RequestParam String code, @RequestParam String state) {
        try {
            JsonNode tokenNode = wechatUtil.getAccessToken(code);
            if (tokenNode.has("errcode")) {
                return "<html><body><h2>微信登录失败</h2><p>" + tokenNode.get("errmsg").asText() + "</p></body></html>";
            }

            String accessToken = tokenNode.get("access_token").asText();
            String openid = tokenNode.get("openid").asText();
            String unionid = tokenNode.has("unionid") ? tokenNode.get("unionid").asText() : null;

            JsonNode userInfo = wechatUtil.getUserInfo(accessToken, openid);
            String rawNickname = userInfo.has("nickname") ? userInfo.get("nickname").asText() : "微信用户";
            // WeChat API double-encodes nicknames: UTF-8 bytes treated as ISO-8859-1
            String nickname = new String(rawNickname.getBytes("ISO-8859-1"), "UTF-8");
            String avatar = userInfo.has("headimgurl") ? userInfo.get("headimgurl").asText() : null;

            User user = userService.getByWechatOpenid(openid);
            if (user == null) {
                user = new User();
                user.setUsername("wx_" + openid.substring(0, 12));
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setNickname(nickname);
                user.setAvatar(avatar);
                user.setWechatOpenid(openid);
                user.setWechatUnionid(unionid);
                user.setRole("USER");
                user.setStatus("NORMAL");
                userService.save(user);
            } else {
                user.setNickname(nickname);
                if (avatar != null) {
                    user.setAvatar(avatar);
                }
                if (unionid != null) {
                    user.setWechatUnionid(unionid);
                }
                userService.updateById(user);
            }

            String jwt = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            redisTemplate.opsForValue().set(WECHAT_LOGIN_PREFIX + state, jwt, 60, TimeUnit.SECONDS);

            return "<html><body style=\"text-align:center;padding-top:80px;font-family:sans-serif;\">"
                    + "<h2 style=\"color:#38BDF8;\">登录成功！</h2>"
                    + "<p>请返回网页继续操作</p>"
                    + "</body></html>";

        } catch (Exception e) {
            return "<html><body><h2>登录异常</h2><p>" + e.getMessage() + "</p></body></html>";
        }
    }

    /**
     * 刷新令牌：用 Refresh Token 换取新的 Access Token + Refresh Token
     */
    @PostMapping("/auth/refresh")
    public Result<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            return Result.error(401, "Refresh Token 无效或已过期");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(401, "用户不存在");
        }
        String newToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        return Result.success(buildLoginResponse(user, newToken, newRefreshToken));
    }

    private LoginResponse buildLoginResponse(User user, String token, String refreshToken) {
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setRefreshToken(refreshToken);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        resp.setAvatar(user.getAvatar());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole());
        return resp;
    }
}
