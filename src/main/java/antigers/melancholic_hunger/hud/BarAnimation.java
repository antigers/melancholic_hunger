package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.config.MelancholicConfig;
import net.minecraft.util.Util;
import net.minecraft.client.gui.Gui;

import java.util.function.Predicate;

public class BarAnimation {
    private long startTime;
    private boolean isRunning = false;
    private boolean reverse = true;
    private int currentPos = 0;
    private float currentOpacity = 0;
    private Gui.ContextualInfo currentBarType;
    // this predicate checks if the current bar is always rendered, in which case the animation is in the fixed position
    Predicate<Gui.ContextualInfo> alwaysOnPredicate;

    public BarAnimation(Predicate<Gui.ContextualInfo> alwaysOnPredicate) {
        this.alwaysOnPredicate = alwaysOnPredicate;
    }

    private void beginIfNotAlready(long now) {
        if (currentPos > 0 || isRunning) {
            return;
        }
        startTime = now;
        reverse = false;
        isRunning = true;
    }

    private void beginReverseIfNotAlready(long now) {
        if (currentPos < 7 || isRunning) {
            return;
        }
        startTime = now;
        reverse = true;
        isRunning = true;
    }

    public void update(Gui.ContextualInfo currentBarType, boolean shouldDraw) {
        // this method is called on every frame
        var now = Util.getMillis();
        if (this.alwaysOnPredicate.test(currentBarType) || shouldDraw) {
            beginIfNotAlready(now);
        } else {
            // starting to go backwards
            beginReverseIfNotAlready(now);
        }
        if (!isRunning) {
            this.currentBarType = currentBarType;
        }
        if (MelancholicConfig.enableExperienceAnimation()) {
            long animationTime = now - startTime;
            int animationDuration = MelancholicConfig.experienceAnimationDuration();
            if (animationTime < animationDuration) {
                currentOpacity = (float) animationTime / animationDuration;
                currentPos = (int) (currentOpacity * 7);
            } else {
                currentOpacity = 1.0F;
                currentPos = 7;
                isRunning = false;
            }
        } else {
            currentOpacity = 1.0F;
            currentPos = 7;
            isRunning = false;
        }
        if (reverse) {
            currentOpacity = 1.0F - currentOpacity;
            currentPos = 7 - currentPos;
        }
    }

    public int getCurrentPos() {
        return currentPos;
    }

    public float getCurrentOpacity() {
        return currentOpacity;
    }

    /**
     * Checks if the experience bar animation is still running, which is important to keep rendering the exp bar until
     * the animation is fully finished
     */
    public boolean shouldStillDrawExperience() { //Gui.ContextualInfo barType) {
        return currentBarType == Gui.ContextualInfo.EXPERIENCE && isRunning;
    }
}
