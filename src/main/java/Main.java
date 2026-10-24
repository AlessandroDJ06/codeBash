import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    private static final int DEFAULT_BEAM_WIDTH = 500;

    private Main() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Gebruik: Main <input> <output> [beamWidth]");
            return;
        }
        Campaign campaign = CampaignParser.parse(Path.of(args[0]));
        int beamWidth = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_BEAM_WIDTH;
        CampaignState best = new BeamSearch(beamWidth).solve(campaign);
        OutputWriter.write(best, Path.of(args[1]));
        System.out.println("Berekende score: " + best.score);
    }
}
