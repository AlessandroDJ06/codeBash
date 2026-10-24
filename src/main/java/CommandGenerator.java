import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class CommandGenerator {
    private CommandGenerator() {}

    static List<Command> heroCommands(CampaignState state) {
        HeroState hero = state.currentHero();
        List<Command> commands = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ActionSlot slot : hero.slots) {
            if (slot.availableFromRound > state.round) continue;
            if (!seen.add(slot.actionId)) continue;
            addCommandsFor(state, hero, state.campaign.actions().get(slot.actionId), commands);
        }
        return commands;
    }

    static List<RewardChoice> rewardChoices(CampaignState state) {
        List<RewardChoice> choices = new ArrayList<>();
        choices.add(new RewardChoice(null, -1));
        for (RewardDef reward : state.battle().rewards()) addRewardChoices(state, reward, choices);
        return choices;
    }

    private static void addCommandsFor(CampaignState state, HeroState hero, ActionDef action, List<Command> commands) {
        for (Fighter target : targetsFor(state, hero, action)) {
            if (!isUseful(state, action, target)) continue;
            commands.add(new Command(hero.index, action.id(), target instanceof HeroState, target.index));
        }
    }

    private static List<Fighter> targetsFor(CampaignState state, HeroState hero, ActionDef action) {
        List<Fighter> targets = new ArrayList<>();
        switch (action.targetKind()) {
            case ENEMY -> state.enemies.stream().filter(Fighter::isAlive).forEach(targets::add);
            case ALLY -> state.heroes.stream().filter(h -> h.isAlive() && h != hero).forEach(targets::add);
            case SELF -> targets.add(hero);
        }
        return targets;
    }

    private static boolean isUseful(CampaignState state, ActionDef action, Fighter target) {
        int round = state.round;
        boolean hostile = action.targetKind() == TargetKind.ENEMY;
        return switch (action.type()) {
            case HEAL -> target.hp < target.maxHp;
            case STUN -> target.effects.stunnedUntilRound() < round + action.duration();
            case DISPEL -> hostile ? target.effects.hasBuffsOrShields(round) : target.effects.hasDebuffsOrStun(round);
            default -> true;
        };
    }

    private static void addRewardChoices(CampaignState state, RewardDef reward, List<RewardChoice> choices) {
        switch (reward.type()) {
            case SCORE_BONUS, RESTORE_HP_PARTY -> choices.add(new RewardChoice(reward, -1));
            case REVIVE_HERO -> addForHeroes(state, reward, choices, h -> !h.isAlive());
            case RESTORE_HP_SINGLE -> addForHeroes(state, reward, choices, h -> h.isAlive() && h.hp < h.maxHp);
            default -> addForHeroes(state, reward, choices, HeroState::isAlive);
        }
    }

    private static void addForHeroes(CampaignState state, RewardDef reward, List<RewardChoice> choices, Predicate<HeroState> eligible) {
        for (HeroState hero : state.heroes) {
            if (eligible.test(hero)) choices.add(new RewardChoice(reward, hero.index));
        }
    }
}
