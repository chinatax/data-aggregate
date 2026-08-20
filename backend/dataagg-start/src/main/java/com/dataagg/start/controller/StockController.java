package com.dataagg.start.controller;

import com.dataagg.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/market")
    public Result<?> getMarket() {
        try {
            String codes = "s_sh000001,s_sz399001,s_sz399006," +
                           "s_sh000016,s_sh000688,s_sh000300,s_sz399005,s_sz399905";
            String url = "https://hq.sinajs.cn/list=" + codes;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://finance.sina.com.cn");
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> resp = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class
            );

            String body = new String(resp.getBody() != null ? resp.getBody() : new byte[0],
                                     Charset.forName("GBK"));

            List<Map<String, Object>> list = new ArrayList<>();
            for (String line : body.split("\n")) {
                if (line.isBlank()) continue;
                String var = line.trim();
                if (var.startsWith("var ")) {
                    String[] parts = var.split("\"");
                    if (parts.length < 2) continue;
                    // parts[1] = name,open,prevClose,price,high,low,...
                    boolean isIndex = var.contains("s_sh") || var.contains("s_sz");
                    String[] fields = parts[1].split(",");
                    if (fields.length < 4) continue;
                    String name = fields[0];
                    double price, change, changePct;
                    if (isIndex) {
                        price = Double.parseDouble(fields[1]);
                        change = Double.parseDouble(fields[2]);
                        changePct = Double.parseDouble(fields[3]);
                    } else {
                        double prevClose = Double.parseDouble(fields[2]);
                        price = Double.parseDouble(fields[3]);
                        change = price - prevClose;
                        changePct = prevClose > 0 ? (change / prevClose) * 100 : 0;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", name);
                    item.put("price", price);
                    item.put("change", Math.round(change * 100.0) / 100.0);
                    item.put("changePercent", Math.round(changePct * 100.0) / 100.0);
                    list.add(item);
                }
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取行情失败: " + e.getMessage());
        }
    }
}
