package com.basas.blackholesim.ui;


public class UiState {
    private double mass = 80.0;
    private double speed = 1.0;
    private double zoom = 1.0;

    private boolean paused = false;

    private boolean trailsEnabled = true;
    private boolean gridDistortionEnabled = true;
    private boolean velocityVectorsEnabled = false;

    
    private boolean placeBlackHoleMode = false;

    public double getMass() { return mass; }
    public void setMass(double mass) { this.mass = mass; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = zoom; }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public boolean isTrailsEnabled() { return trailsEnabled; }
    public void setTrailsEnabled(boolean trailsEnabled) { this.trailsEnabled = trailsEnabled; }

    public boolean isGridDistortionEnabled() { return gridDistortionEnabled; }
    public void setGridDistortionEnabled(boolean gridDistortionEnabled) { this.gridDistortionEnabled = gridDistortionEnabled; }

    public boolean isVelocityVectorsEnabled() { return velocityVectorsEnabled; }
    public void setVelocityVectorsEnabled(boolean velocityVectorsEnabled) { this.velocityVectorsEnabled = velocityVectorsEnabled; }

    public boolean isPlaceBlackHoleMode() { return placeBlackHoleMode; }
    public void setPlaceBlackHoleMode(boolean placeBlackHoleMode) { this.placeBlackHoleMode = placeBlackHoleMode; }
}
