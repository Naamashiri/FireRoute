package fireroute.domain.routing;
public enum WalkingPace {
    SLOW(3.5),
    AVERAGE(5.0),
    FAST(6.0);

    private final double speedKmh;

    WalkingPace(double speedKmh) {
        this.speedKmh = speedKmh;
    }

    public double getSpeedKmh() {
        return speedKmh;
    }
}

