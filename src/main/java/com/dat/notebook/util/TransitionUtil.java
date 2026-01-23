package com.dat.notebook.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Utility class for JavaFX animations
 * Provides reusable animation methods for smooth UI transitions
 */
public class TransitionUtil {

    // ==================== SCENE TRANSITIONS ====================
    
    /**
     * Fade in + Slide right animation for scene transition
     */
    public static void playSceneTransition(Node root) {
        // Fade in
        FadeTransition fade = new FadeTransition(Duration.millis(400), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        
        // Slide from left
        TranslateTransition slide = new TranslateTransition(Duration.millis(400), root);
        slide.setFromX(-30);
        slide.setToX(0);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }
    
    /**
     * Fade out animation
     */
    public static void playFadeOut(Node node, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        fade.play();
    }
    
    /**
     * Scale up animation (0.95 → 1.0)
     */
    public static void playScaleIn(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), node);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1.0);
        scale.setToY(1.0);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        
        ParallelTransition parallel = new ParallelTransition(scale, fade);
        parallel.play();
    }

    // ==================== BUTTON ANIMATIONS ====================
    
    /**
     * Add hover animation to button (scale + shadow)
     */
    public static void addButtonHoverEffect(Node button) {
        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });
        
        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
        
        button.setOnMousePressed(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(80), button);
            scale.setToX(0.95);
            scale.setToY(0.95);
            scale.play();
        });
        
        button.setOnMouseReleased(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(80), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });
    }

    // ==================== CARD ANIMATIONS ====================
    
    /**
     * Add hover effect to card (scale + lift)
     */
    public static void addCardHoverEffect(Node card) {
        card.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.03);
            scale.setToY(1.03);
            scale.play();
        });
        
        card.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
    }
    
    /**
     * Stagger animation for multiple cards
     */
    public static void playStaggerAnimation(Node... nodes) {
        SequentialTransition sequential = new SequentialTransition();
        
        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            
            // Fade + Slide up
            FadeTransition fade = new FadeTransition(Duration.millis(300), node);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), node);
            slide.setFromY(15);
            slide.setToY(0);
            
            ParallelTransition parallel = new ParallelTransition(fade, slide);
            
            // Add delay between cards
            PauseTransition pause = new PauseTransition(Duration.millis(i * 50));
            SequentialTransition cardAnim = new SequentialTransition(pause, parallel);
            
            cardAnim.play();
        }
    }
    
    /**
     * Pulse animation (breathing effect)
     */
    public static void playPulseAnimation(Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1000), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    // ==================== VALIDATION ANIMATIONS ====================
    
    /**
     * Shake animation for error validation
     */
    public static void playShakeAnimation(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }
    
    /**
     * Success animation (bounce)
     */
    public static void playSuccessAnimation(Node node) {
        ScaleTransition bounce = new ScaleTransition(Duration.millis(200), node);
        bounce.setFromX(0.9);
        bounce.setFromY(0.9);
        bounce.setToX(1.1);
        bounce.setToY(1.1);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(2);
        bounce.play();
    }

    // ==================== LOADING ANIMATIONS ====================
    
    /**
     * Fade + Rotate loading animation
     */
    public static RotateTransition createLoadingSpinner(Node node) {
        RotateTransition rotate = new RotateTransition(Duration.millis(1000), node);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        return rotate;
    }
    
    /**
     * Progress bar animation
     */
    public static void animateProgress(javafx.scene.control.ProgressBar progressBar, double targetProgress, Runnable onFinished) {
        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(progressBar.progressProperty(), targetProgress);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(2000), keyValue);
        timeline.getKeyFrames().add(keyFrame);
        timeline.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        timeline.play();
    }

    // ==================== SIDEBAR ANIMATIONS ====================
    
    /**
     * Slide menu item on hover
     */
    public static void addMenuItemHoverEffect(Node menuItem) {
        menuItem.setOnMouseEntered(e -> {
            TranslateTransition slide = new TranslateTransition(Duration.millis(150), menuItem);
            slide.setToX(5);
            slide.play();
        });
        
        menuItem.setOnMouseExited(e -> {
            TranslateTransition slide = new TranslateTransition(Duration.millis(150), menuItem);
            slide.setToX(0);
            slide.play();
        });
    }
}
