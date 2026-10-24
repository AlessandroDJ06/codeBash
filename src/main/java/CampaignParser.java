import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class CampaignParser {
    private final String[] tokens;
    private int position;

    private CampaignParser(String text) {
        this.tokens = text.trim().split("\\s+");
    }

    static Campaign parse(Path file) throws IOException {
        return new CampaignParser(Files.readString(file)).readCampaign();
    }

    private Campaign readCampaign() {
        int heroCount = nextInt();
        int actionCount = nextInt();
        int battleCount = nextInt();
        Map<String, ActionDef> actions = readActions(actionCount);
        List<HeroDef> heroes = readList(heroCount, this::readHero);
        List<BattleDef> battles = readList(battleCount, this::readBattle);
        return new Campaign(actions, heroes, battles);
    }

    private Map<String, ActionDef> readActions(int count) {
        Map<String, ActionDef> actions = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            ActionDef action = readAction();
            actions.put(action.id(), action);
        }
        return actions;
    }

    private ActionDef readAction() {
        return new ActionDef(next(), ActionType.valueOf(next()), nextInt(),
                TargetKind.valueOf(next()), nextInt(), nextInt());
    }

    private HeroDef readHero() {
        String id = next();
        String name = next();
        int maxHp = nextInt();
        int initiative = nextInt();
        List<String> actionIds = readList(nextInt(), this::next);
        return new HeroDef(id, name, maxHp, initiative, actionIds);
    }

    private BattleDef readBattle() {
        String id = next();
        int maxRounds = nextInt();
        int completionReward = nextInt();
        int enemyCount = nextInt();
        int rewardCount = nextInt();
        List<EnemyDef> enemies = readList(enemyCount, this::readEnemy);
        List<RewardDef> rewards = readList(rewardCount, this::readReward);
        return new BattleDef(id, maxRounds, completionReward, enemies, rewards);
    }

    private EnemyDef readEnemy() {
        String id = next();
        String name = next();
        int hp = nextInt();
        int initiative = nextInt();
        int reward = nextInt();
        List<RotationEntry> rotation = readList(nextInt(), this::readRotationEntry);
        return new EnemyDef(id, name, hp, initiative, reward, rotation);
    }

    private RotationEntry readRotationEntry() {
        String actionId = next();
        List<Criterion> criteria = readList(nextInt(), () -> Criterion.valueOf(next()));
        return new RotationEntry(actionId, criteria);
    }

    private RewardDef readReward() {
        String id = next();
        RewardType type = RewardType.valueOf(next());
        int value = nextInt();
        int cost = nextInt();
        String actionId = type == RewardType.LEARN_ACTION ? next() : null;
        return new RewardDef(id, type, value, cost, actionId);
    }

    private <T> List<T> readList(int count, Supplier<T> reader) {
        List<T> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) items.add(reader.get());
        return items;
    }

    private String next() {
        return tokens[position++];
    }

    private int nextInt() {
        return Integer.parseInt(next());
    }
}
