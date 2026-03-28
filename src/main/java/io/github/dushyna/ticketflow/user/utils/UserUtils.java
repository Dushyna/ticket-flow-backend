package io.github.dushyna.ticketflow.user.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class UserUtils {

    public static final Path AVATAR_DIR = Paths.get("uploads", "avatars");
    public static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    public static String normalizeUserName(String name) {
        if (name == null || name.isBlank()) return null;
        name = name.trim();
        if (name.length() == 1) return name.toUpperCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static String emptyFieldToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    public static String generateUserAvatarFileName(UUID userId) {
        return userId + ".png";
    }

    public static BufferedImage resizeAvatarAndConvertToPng(MultipartFile file, int maxSize) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("Unsupported image format");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double scale = Math.min(
                (double) maxSize / originalWidth,
                (double) maxSize / originalHeight
        );

        if (scale > 1) {
            scale = 1;
        }

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        BufferedImage resizedImage = new BufferedImage(
                newWidth,
                newHeight,
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    public static Path saveUserAvatar(BufferedImage image, Path dir, String filename) throws IOException {
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        ImageIO.write(image, "png", target.toFile());
        return target;
    }
}
