package com.dataagg.start.controller;

import com.dataagg.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    private Path getUploadDir() {
        Path path = Paths.get(uploadPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), uploadPath);
        }
        return path.toAbsolutePath().normalize();
    }

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名无效");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ext.matches("^(jpg|jpeg|png|gif|webp|bmp|svg)$")) {
            return Result.error("不支持的图片格式");
        }

        try {
            // 按日期分目录
            String dateDir = new SimpleDateFormat("yyyyMMdd").format(new Date());
            Path dirPath = getUploadDir().resolve("images").resolve(dateDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String baseName = UUID.randomUUID().toString().replace("-", "");
            String newFilename = baseName + "." + ext;
            Path filePath = dirPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            // 生成缩略图（360px 宽，JPEG）
            String thumbFilename = baseName + "_thumb.jpg";
            Path thumbPath = dirPath.resolve(thumbFilename);
            generateThumbnail(filePath.toFile(), thumbPath.toFile(), 360);

            // 生成 WebP 版本（可选，服务器需安装 libwebp）
            generateWebP(filePath.toFile(), new File(dirPath.toFile(), baseName + ".webp"));

            // 构建访问 URL（生产环境使用 HTTPS）
            String serverUrl = "https://" + request.getServerName();
            if (request.getServerPort() != 80 && request.getServerPort() != 443) {
                serverUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            }
            String accessUrl = serverUrl + "/uploads/images/" + dateDir + "/" + newFilename;
            return Result.success(accessUrl);
        } catch (IOException e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    private void generateThumbnail(File source, File target, int maxWidth) {
        // SVG 不做缩略图
        String name = source.getName().toLowerCase();
        if (name.endsWith(".svg") || name.endsWith(".gif")) return;

        try {
            BufferedImage original = ImageIO.read(source);
            if (original == null) return;

            int w = original.getWidth();
            int h = original.getHeight();
            if (w <= maxWidth) {
                // 原图已足够小，直接复制
                Files.copy(source.toPath(), target.toPath());
                return;
            }

            int newH = (int) ((double) h / w * maxWidth);
            BufferedImage thumb = new BufferedImage(maxWidth, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumb.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(original, 0, 0, maxWidth, newH, null);
            g2d.dispose();

            // 输出 JPEG，质量 0.8
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(target)) {
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.8f);
                    writer.write(null, new IIOImage(thumb, null, null), param);
                    writer.dispose();
                }
            } else {
                ImageIO.write(thumb, "jpg", target);
            }
        } catch (IOException e) {
            // 缩略图生成失败不阻塞上传
        }
    }

    private void generateWebP(File source, File target) {
        String name = source.getName().toLowerCase();
        if (name.endsWith(".svg") || name.endsWith(".gif")) return;
        try {
            ProcessBuilder pb = new ProcessBuilder("cwebp", "-q", "80", source.getAbsolutePath(), "-o", target.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            // WebP 生成失败不阻塞上传（cwebp 可能未安装）
        }
    }
}
