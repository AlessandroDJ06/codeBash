final class Scoring {
    private Scoring() {}

    static long completionBonus(int maxReward, int maxRounds, int roundsTaken) {
        if (maxRounds <= 1) return maxReward;
        long span = (long) (maxRounds - 1) * (maxRounds - 1);
        long used = (long) (roundsTaken - 1) * (roundsTaken - 1);
        return Math.max(0, maxReward * (span - used) / span);
    }
}
