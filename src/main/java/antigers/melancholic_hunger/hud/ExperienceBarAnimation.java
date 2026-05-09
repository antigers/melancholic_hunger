package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.config.MelancholicConfig;
import net.minecraft.Util;

public class ExperienceBarAnimation {
    private long startTime;
    private boolean isRunning = false;
    private boolean reverse = true;
    private int currentPos = 0;
    private float currentOpacity = 0;
    private long drawUntil = 0;

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

    public void onGainExperience() {
        long now = Util.getMillis();
        drawUntil = now + 3000;
        beginIfNotAlready(now);
    }

    public void update(boolean isExpBarDrawnConstantly) {
        // this method is called on every frame
        if (!MelancholicConfig.hideExperienceBar()) {
            currentPos = 7;
            currentOpacity = 1.0F;
            return;
        }
        long now = Util.getMillis();
        if (isExpBarDrawnConstantly) {
            drawUntil = 0;
            beginIfNotAlready(now);
        }
        else if (now > drawUntil) {
            // starting to go backwards
            drawUntil = 0;
            beginReverseIfNotAlready(now);
        }
        if (!isRunning) {
            return;
        }
        if (MelancholicConfig.enableExperienceAnimation()) {
            long animationTime = now - startTime;
            int animationDuration = MelancholicConfig.experienceAnimationDuration();
            if (animationTime < animationDuration) {
                currentOpacity = (float) animationTime / animationDuration;
                currentPos = (int) (currentOpacity * 7);
            }
            else {
                currentOpacity = 1.0F;
                currentPos = 7;
                isRunning = false;
            }
        }
        else {
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

    public boolean shouldDraw() {
        return drawUntil > 0 || currentPos > 0;
    }
}
