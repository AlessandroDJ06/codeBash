import java.util.ArrayList;
import java.util.List;

final class Effects {

    static final class Timed {
        int value;
        final int expiresRound;

        Timed(int value, int expiresRound) {
            this.value = value;
            this.expiresRound = expiresRound;
        }

        boolean isActive(int round) {
            return expiresRound >= round;
        }
    }

    private final List<Timed> shields = new ArrayList<>();
    private final List<Timed> buffs = new ArrayList<>();
    private final List<Timed> debuffs = new ArrayList<>();
    private int stunnedUntilRound;

    Effects copy() {
        Effects copy = new Effects();
        copyAll(shields, copy.shields);
        copyAll(buffs, copy.buffs);
        copyAll(debuffs, copy.debuffs);
        copy.stunnedUntilRound = stunnedUntilRound;
        return copy;
    }

    void addShield(int value, int expiresRound) { shields.add(new Timed(value, expiresRound)); }

    void addBuff(int value, int expiresRound) { buffs.add(new Timed(value, expiresRound)); }

    void addDebuff(int value, int expiresRound) { debuffs.add(new Timed(value, expiresRound)); }

    void stunUntil(int round) { stunnedUntilRound = Math.max(stunnedUntilRound, round); }

    int stunnedUntilRound() { return stunnedUntilRound; }

    boolean isStunned(int round) { return round <= stunnedUntilRound; }

    int shieldTotal(int round) { return sumActive(shields, round); }

    int buffTotal(int round) { return sumActive(buffs, round); }

    int debuffTotal(int round) { return sumActive(debuffs, round); }

    int buffCount(int round) { return countActive(buffs, round); }

    int debuffCount(int round) { return countActive(debuffs, round); }

    boolean hasBuffsOrShields(int round) { return buffCount(round) > 0 || shieldTotal(round) > 0; }

    boolean hasDebuffsOrStun(int round) { return debuffCount(round) > 0 || isStunned(round); }

    int absorb(int damage, int round) {
        int remaining = damage;
        for (Timed shield : shields) {
            if (remaining == 0) break;
            if (!shield.isActive(round)) continue;
            int absorbed = Math.min(shield.value, remaining);
            shield.value -= absorbed;
            remaining -= absorbed;
        }
        shields.removeIf(shield -> shield.value <= 0);
        return remaining;
    }

    void dispelHostile() {
        buffs.clear();
        shields.clear();
    }

    void dispelFriendly() {
        debuffs.clear();
        stunnedUntilRound = 0;
    }

    void pruneExpired(int round) {
        shields.removeIf(t -> !t.isActive(round));
        buffs.removeIf(t -> !t.isActive(round));
        debuffs.removeIf(t -> !t.isActive(round));
    }

    Effects forNextBattle(int lastRound) {
        Effects next = new Effects();
        carryOver(buffs, next.buffs, lastRound);
        carryOver(debuffs, next.debuffs, lastRound);
        return next;
    }

    long mixInto(long hash) {
        hash = mixAll(hash, shields);
        hash = mixAll(hash, buffs);
        hash = mixAll(hash, debuffs);
        return CampaignState.mix(hash, stunnedUntilRound);
    }

    private static long mixAll(long hash, List<Timed> timed) {
        hash = CampaignState.mix(hash, timed.size());
        for (Timed t : timed) {
            hash = CampaignState.mix(hash, t.value);
            hash = CampaignState.mix(hash, t.expiresRound);
        }
        return hash;
    }

    private static void carryOver(List<Timed> from, List<Timed> to, int lastRound) {
        for (Timed t : from) {
            int remaining = t.expiresRound - lastRound;
            if (remaining >= 1) to.add(new Timed(t.value, remaining));
        }
    }

    private static void copyAll(List<Timed> from, List<Timed> to) {
        for (Timed t : from) to.add(new Timed(t.value, t.expiresRound));
    }

    private static int sumActive(List<Timed> timed, int round) {
        int sum = 0;
        for (Timed t : timed) if (t.isActive(round)) sum += t.value;
        return sum;
    }

    private static int countActive(List<Timed> timed, int round) {
        int count = 0;
        for (Timed t : timed) if (t.isActive(round)) count++;
        return count;
    }
}
