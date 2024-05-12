package com.udacity.catpoint.image.service;
import java.awt.image.BufferedImage;

public interface ImageServices {
    boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
