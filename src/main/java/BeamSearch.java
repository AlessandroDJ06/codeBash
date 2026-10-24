import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BeamSearch {
    private record BattleOutcome(List<CampaignState> won, CampaignState bestFailed) {}

    private record Scored(CampaignState state, double value) {}

    private final int beamWidth;

    BeamSearch(int beamWidth) {
        this.beamWidth = beamWidth;
    }

    CampaignState solve(Campaign campaign) {
        List<CampaignState> frontier = List.of(CampaignState.initial(campaign));
        CampaignState best = null;
        while (!frontier.isEmpty()) {
            BattleOutcome outcome = fightBattle(frontier);
            best = betterOf(best, outcome.bestFailed());
            List<CampaignState> afterRewards = outcome.won().parallelStream()
                    .flatMap(state -> rewardChildren(state).stream())
                    .toList();
            for (CampaignState state : afterRewards) {
                if (state.phase == Phase.COMPLETED) best = betterOf(best, state);
            }
            frontier = trim(afterRewards.stream().filter(s -> s.phase == Phase.PLAYING).toList());
        }
        return best;
    }

    private BattleOutcome fightBattle(List<CampaignState> starts) {
        List<CampaignState> layer = starts;
        List<CampaignState> won = new ArrayList<>();
        CampaignState bestFailed = null;
        while (!layer.isEmpty()) {
            List<CampaignState> children = layer.parallelStream()
                    .flatMap(state -> expand(state).stream())
                    .toList();
            List<CampaignState> playing = new ArrayList<>();
            for (CampaignState child : children) {
                switch (child.phase) {
                    case PLAYING -> playing.add(child);
                    case BATTLE_WON -> won.add(child);
                    case FAILED -> bestFailed = betterOf(bestFailed, child);
                    case COMPLETED -> throw new IllegalStateException("Battle cannot complete without reward");
                }
            }
            won = new ArrayList<>(trim(won));
            layer = trim(playing);
        }
        return new BattleOutcome(won, bestFailed);
    }

    private List<CampaignState> expand(CampaignState state) {
        List<Command> commands = CommandGenerator.heroCommands(state);
        if (commands.isEmpty()) return List.of(passed(state));
        List<CampaignState> children = new ArrayList<>(commands.size());
        for (Command command : commands) {
            CampaignState child = state.copy();
            Rules.applyHeroCommand(child, command);
            children.add(child);
        }
        return children;
    }

    private CampaignState passed(CampaignState state) {
        CampaignState child = state.copy();
        Rules.skipHero(child);
        return child;
    }

    private List<CampaignState> rewardChildren(CampaignState state) {
        List<CampaignState> children = new ArrayList<>();
        for (RewardChoice choice : CommandGenerator.rewardChoices(state)) {
            CampaignState child = state.copy();
            Rules.applyReward(child, choice);
            children.add(child);
        }
        return children;
    }

    private List<CampaignState> trim(List<CampaignState> states) {
        Map<Long, Scored> unique = new HashMap<>();
        for (CampaignState state : states) {
            Scored scored = new Scored(state, Heuristic.evaluate(state));
            unique.merge(state.fingerprint(), scored, (a, b) -> a.value() >= b.value() ? a : b);
        }
        return unique.values().stream()
                .sorted(Comparator.comparingDouble(Scored::value).reversed())
                .limit(beamWidth)
                .map(Scored::state)
                .toList();
    }

    private static CampaignState betterOf(CampaignState a, CampaignState b) {
        if (a == null) return b;
        if (b == null) return a;
        return b.score > a.score ? b : a;
    }
}
