/*
 *
 * MIT License
 *
 * Copyright (c) 2020 Free Geek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package Utilities;

import com.formdev.flatlaf.util.UIScale;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * @author Pico Mitchell (of Free Geek)
 */
public class TwemojiImage {

    String name;
    double userScalingFactor;
    double systemScalingFactor;

    boolean isMacOS;
    boolean isWindows;

    public TwemojiImage(String emojiName, Window window) {
        isMacOS = System.getProperty("os.name").startsWith("Mac OS X") || System.getProperty("os.name").startsWith("macOS");
        isWindows = System.getProperty("os.name").startsWith("Windows");

        name = emojiName.replace(" ", "");
        if (name.equals("AppIcon")) {
            name = "RobotFace";
        }

        // Linux Mint uses userScaleFactor for HiDPI while macOS and Windows use systemScaleFactor for HiDPI.
        // This code only handles whole and half number scaling factors because thosea are the only PNG sizes I've included. (125% scaling will use 150% sizes and 175% scaling will use 200% sizes).
        userScalingFactor = (Math.round(UIScale.getUserScaleFactor() * 2.0) / 2.0);
        systemScalingFactor = (Math.round(UIScale.getSystemScaleFactor((window != null) ? window.getGraphicsConfiguration() : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()) * 2.0) / 2.0);
    }

    public String toImgTag(String positionInText) {
        int size = 16;

        int userScaledSize = (int) Math.round(size * userScalingFactor);
        int userAndSystemScaledSize = (int) Math.round(userScaledSize * systemScalingFactor);

        if (userAndSystemScaledSize < 24) {
            userAndSystemScaledSize = 16;
        } else if (userAndSystemScaledSize < 32) {
            userAndSystemScaledSize = 24;
        } else if (userAndSystemScaledSize < 48) {
            userAndSystemScaledSize = 32;
        } else if (userAndSystemScaledSize < 64) {
            userAndSystemScaledSize = 48;
        } else if (userAndSystemScaledSize < 96) {
            userAndSystemScaledSize = 64;
        } else if (userAndSystemScaledSize < 128) {
            userAndSystemScaledSize = 96;
        } else {
            userAndSystemScaledSize = 128;
        }

        // For Linux, the userScaleFactor needs to be applied to the actual size of the img tag or the emoji are displayed small, on other OSes the userScaleFactor will be 1 so it won't hurt.
        // On Windows, for some reason with 150% scaling, the images get slightly clipped on the right and bottom instead of being properly scaled.
        //  After much trial and error, I found that subtracting 1px from the height makes the scaling work properly to not clip the image.
        //  Although, this does cause a little distortion from slightly funky scaling, it looks better than lots of images being clipped.
        URL thisEmojiURL = this.getClass().getResource("/Resources/Twemoji/" + name + (name.equals("Blank") ? "" : userAndSystemScaledSize) + ".png");

        return ((thisEmojiURL == null) ? "<b>[MISSING EMOJI: " + name + "]</b>"
                : ((positionInText.equals("right") || positionInText.equals("inline")) ? "&nbsp;" : "")
                + "<img width='" + userScaledSize + "' height='" + (userScaledSize - ((isWindows && String.valueOf(systemScalingFactor).endsWith(".5")) ? 1 : 0)) + "' src='" + thisEmojiURL.toString() + "' />"
                + ((positionInText.equals("left") || positionInText.equals("inline")) ? "&nbsp;" : ""));
    }

    public ImageIcon toImageIcon() {
        return toImageIcon(64, true);
    }

    public ImageIcon toImageIcon(int size) {
        return toImageIcon(size, true);
    }

    public ImageIcon toImageIcon(boolean shouldTrimTopTransparentPixels) {
        return toImageIcon(64, shouldTrimTopTransparentPixels);
    }

    public ImageIcon toImageIcon(int size, boolean shouldTrimTopTransparentPixels) {
        int userScaledSize = (int) Math.round(size * userScalingFactor);
        int userAndSystemScaledSize = (int) Math.round(userScaledSize * systemScalingFactor);

        // For Linux, the userScaleFactor needs to be applied to base image size or it will be too small in the window, on other OSes the userScaleFactor will be 1 so it won't hurt.
        URL thisEmojiURL = this.getClass().getResource("/Resources/Twemoji/" + name + userScaledSize + ".png");

        if (thisEmojiURL == null) {
            userAndSystemScaledSize = (int) Math.round(size * systemScalingFactor);
            thisEmojiURL = this.getClass().getResource("/Resources/Twemoji/" + name + size + ".png");
        }

        if (thisEmojiURL == null) {
            return null;
        }

        try {
            BufferedImage thisEmojiImage = ImageIO.read(thisEmojiURL);

            if (shouldTrimTopTransparentPixels) {
                thisEmojiImage = trimTopTransparentPixels(thisEmojiImage);
            }

            if (userAndSystemScaledSize > userScaledSize) {
                List<Image> multiResolutionImages = new ArrayList<>();

                multiResolutionImages.add(thisEmojiImage);

                URL thisEmojiURLscaled = this.getClass().getResource("/Resources/Twemoji/" + name + userAndSystemScaledSize + ".png");

                if (thisEmojiURLscaled != null) {
                    BufferedImage scaledEmojiImage = ImageIO.read(thisEmojiURLscaled);

                    if (shouldTrimTopTransparentPixels) {
                        scaledEmojiImage = trimTopTransparentPixels(scaledEmojiImage);
                    }

                    multiResolutionImages.add(scaledEmojiImage);
                }

                return new ImageIcon(new BaseMultiResolutionImage(multiResolutionImages.toArray(Image[]::new)));
            } else {
                return new ImageIcon(thisEmojiImage);
            }
        } catch (IOException e) {
            return null;
        }
    }

    public List<Image> toImageIconsForFrame() {
        return (isMacOS ? null : toImageList()); // Do not use any ImageIcons for Frames on macOS because they are only used a minized window image, which we don't want.
    }

    public List<Image> toImageList() {
        List<Image> imageList = new ArrayList<>();
        String[] everyImageSize = new String[]{"16", "24", "32", "48", "64", "96", "128"};

        for (String thisImageSize : everyImageSize) {
            URL thisEmojiURL = this.getClass().getResource("/Resources/Twemoji/" + name + thisImageSize + ".png");

            if (thisEmojiURL != null) {
                try {
                    imageList.add(ImageIO.read(thisEmojiURL));
                } catch (IOException ex) {

                }
            }
        }

        return imageList;
    }

    private BufferedImage trimTopTransparentPixels(BufferedImage image) {
        // Based on: https://stackoverflow.com/questions/3224561/crop-image-to-smallest-size-by-removing-transparent-pixels-in-java
        //  and: https://stackoverflow.com/questions/47164777/crop-transparent-edges-of-an-image

        int height = image.getHeight();

        if (height <= 1) {
            return image;
        }

        int width = image.getWidth();

        int top = 0;

        topLoop:
        for (; top < (height - 1); top++) {
            for (int x = 0; x < width; x++) {
                if (new Color(image.getRGB(x, top), true).getAlpha() != 0) {
                    break topLoop;
                }
            }
        }

        if (top == 0) {
            return image;
        }

        // Instead of actually trimming the image, redraw it with it's the same size but the actual image contents at the top of the frame (with no transparent pixels on the top).
        // This way seems to avoid to artifacts that occur when using getSubimage.
        BufferedImage repositionedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        repositionedImage.getGraphics().drawImage(image, 0, (top * -1), null);

        return repositionedImage;
    }
}
