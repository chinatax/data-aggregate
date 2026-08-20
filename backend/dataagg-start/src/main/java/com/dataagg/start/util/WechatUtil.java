package com.dataagg.start.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class WechatUtil {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    @Value("${wechat.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String QR_CONNECT_URL = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String OAUTH2_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USERINFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    public WechatUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getQrConnectUrl(String state) {
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return QR_CONNECT_URL + "?appid=" + appId
                + "&redirect_uri=" + encodedRedirect
                + "&response_type=code"
                + "&scope=snsapi_login"
                + "&state=" + state
                + "#wechat_redirect";
    }

    public String getOAuth2AuthorizeUrl(String state) {
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return OAUTH2_AUTHORIZE_URL + "?appid=" + appId
                + "&redirect_uri=" + encodedRedirect
                + "&response_type=code"
                + "&scope=snsapi_userinfo"
                + "&state=" + state
                + "#wechat_redirect";
    }

    public JsonNode getAccessToken(String code) throws Exception {
        String url = ACCESS_TOKEN_URL + "?appid=" + appId
                + "&secret=" + appSecret
                + "&code=" + code
                + "&grant_type=authorization_code";
        String response = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(response);
    }

    public JsonNode getUserInfo(String accessToken, String openid) throws Exception {
        String url = USERINFO_URL + "?access_token=" + accessToken
                + "&openid=" + openid;
        String response = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(response);
    }
}
