package org.openjfx;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class WebcamService extends Service<Image> {

    private Webcam webcam;
    // Gunakan volatile agar aman diakses antar thread
    private volatile Image lastImage; 

    public WebcamService() {
        try {
            // Initialize webcam
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(WebcamResolution.VGA.getSize());
            } else {
                System.err.println("No webcam found.");
            }
        } catch (Exception e) {
            System.err.println("Error initializing webcam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected Task<Image> createTask() {
        return new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                if (webcam == null) {
                    throw new RuntimeException("No webcam found");
                }

                if (!webcam.isOpen()) {
                    try {
                        webcam.open();
                    } catch (Exception e) {
                        System.err.println("Failed to open webcam: " + e.getMessage());
                        throw e; // Lemparkan lagi agar service gagal
                    }
                }

                while (!isCancelled() && webcam.isOpen()) {
                    try {
                        BufferedImage bufferedImage = webcam.getImage();
                        if (bufferedImage != null) {
                            Image fxImage = convertToFXImage(bufferedImage);
                            lastImage = fxImage;
                            updateValue(fxImage); // Kirim gambar ke JavaFX Application Thread
                        }
                        
                        // Anda bisa turunkan nilai sleep untuk FPS lebih tinggi, misal 50ms (20 FPS)
                        Thread.sleep(100); // 10 FPS
                        
                    } catch (InterruptedException e) {
                        if (isCancelled()) {
                            break; // Keluar jika service dibatalkan
                        }
                    } catch (Exception e) {
                        System.err.println("Error capturing image: " + e.getMessage());
                        break;
                    }
                }
                return lastImage;
            }
        };
    }

    private Image convertToFXImage(BufferedImage bufferedImage) {
    int width = bufferedImage.getWidth();
    int height = bufferedImage.getHeight();

    WritableImage writableImage = new WritableImage(width, height);
    int[] pixels = new int[width * height];
    bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
    
    byte[] bytes = new byte[width * height * 4];
    for (int i = 0; i < pixels.length; i++) {
        int pixel = pixels[i];
        
        // --- PERBAIKAN DI SINI ---
        // Ubah urutan dari RGBA menjadi BGRA
        
        bytes[i * 4]     = (byte) (pixel & 0xFF);         // Blue (Biru)
        bytes[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);  // Green (Hijau)
        bytes[i * 4 + 2] = (byte) ((pixel >> 16) & 0xFF); // Red (Merah)
        bytes[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF); // Alpha
    }

    // Gunakan format yang ADA: getByteBgraInstance()
    writableImage.getPixelWriter().setPixels(0, 0, width, height,
            PixelFormat.getByteBgraInstance(), bytes, 0, width * 4);

    return writableImage;
}

    public Image getLastImage() {
        return lastImage;
    }

    public void closeWebcam() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        closeWebcam();
    }
    
    @Override
    protected void failed() {
        super.failed();
        getException().printStackTrace(); // Tampilkan error jika task gagal
        closeWebcam();
    }
    
    @Override
    protected void succeeded() {
        super.succeeded();
        closeWebcam();
    }
}