package com.dat.notebook.controller;

import com.dat.notebook.util.TransitionUtil;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * 🚀 Controller cho Splash Screen - Gen Z Edition
 * 
 * Hiển thị màn hình khởi động với:
 * - Animation breathing cho logo
 * - Progress bar mượt mà
 * - Fade in/out transitions
 * 
 * @author SmartNotebook Team
 * @version 2.0
 */
public class SplashController {
    

    
    @FXML private VBox mainContainer;
    @FXML private Label logoIcon;
    @FXML private Label loadingText;
    @FXML private ProgressBar progressBar;
    

    
    private static final String[] LOADING_MESSAGES = {
        "✨ Loading your ideas...",
        "🎨 Preparing interface...",
        "🔌 Connecting database...",
        "📝 Loading notes...",
        "🚀 Almost there..."
    };
    

    
    @FXML
    public void initialize() {
        // Fade in animation cho toàn bộ container
        mainContainer.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), mainContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        
        // Breathing animation cho logo
        playLogoPulseAnimation();
        
        // Bắt đầu loading animation
        startLoadingAnimation();
    }
    
    /**
     * Animation nhấp nháy (breathing) cho logo
     */
    private void playLogoPulseAnimation() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1500), logoIcon);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }
    
    /**
     * Bắt đầu animation loading với progress bar
     */
    private void startLoadingAnimation() {
        Timeline timeline = new Timeline();
        int stepDuration = 600; // ms cho mỗi bước
        
        for (int i = 0; i < LOADING_MESSAGES.length; i++) {
            final int step = i;
            final double progress = (double) (i + 1) / LOADING_MESSAGES.length;
            
            KeyFrame keyFrame = new KeyFrame(
                Duration.millis(stepDuration * (i + 1)),
                event -> {
                    // Animate progress bar
                    animateProgress(progress);
                    
                    // Update loading text với fade
                    updateLoadingText(LOADING_MESSAGES[step]);
                    
                    // Khi hoàn thành, chuyển sang Login
                    if (step == LOADING_MESSAGES.length - 1) {
                        Timeline delay = new Timeline(new KeyFrame(
                            Duration.millis(400),
                            e -> navigateToLogin()
                        ));
                        delay.play();
                    }
                }
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        
        timeline.play();
    }
    
    /**
     * Animate progress bar mượt mà
     */
    private void animateProgress(double targetProgress) {
        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(progressBar.progressProperty(), targetProgress, Interpolator.EASE_BOTH);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500), keyValue);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }
    
    /**
     * Update loading text với fade animation
     */
    private void updateLoadingText(String newText) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), loadingText);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            loadingText.setText(newText);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), loadingText);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }
    

    
    /**
     * Chuyển sang màn hình Login với fade out animation
     */
    private void navigateToLogin() {
        // Fade out splash screen
        TransitionUtil.playFadeOut(mainContainer, () -> {
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
                    Parent root = loader.load();
                    
                    // Fade in login screen
                    TransitionUtil.playSceneTransition(root);
                    
                    Stage loginStage = new Stage();
                    loginStage.setTitle("SmartNotebook - Đăng nhập");
                    loginStage.setScene(new Scene(root));
                    loginStage.setResizable(false);
                    loginStage.show();
                    
                    // Close splash
                    Stage splashStage = (Stage) mainContainer.getScene().getWindow();
                    splashStage.close();
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Lỗi khi chuyển sang Login: " + e.getMessage());
                }
            });
        });
    }
}
