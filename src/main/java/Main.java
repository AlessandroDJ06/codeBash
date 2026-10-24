import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class Main {
    private static final int DEFAULT_BEAM_WIDTH = 500;

    private Main() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Gebruik: Main <inputBestandOfMap> <outputBestandOfMap> [beamWidth]");
            return;
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int beamWidth = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_BEAM_WIDTH;

        if (!Files.isDirectory(input)) {
            solve(input, output, beamWidth);
            return;
        }
        Files.createDirectories(output);
        for (Path file : listInputFiles(input)) {
            solveSafely(file, output.resolve(file.getFileName()), beamWidth);
        }
    }

    private static List<Path> listInputFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static void solveSafely(Path input, Path output, int beamWidth) {
        try {
            solve(input, output, beamWidth);
        } catch (Exception e) {
            System.err.println(input.getFileName() + " mislukt: " + e);
        }
    }

    private static void solve(Path input, Path output, int beamWidth) throws IOException {
        Campaign campaign = CampaignParser.parse(input);
        CampaignState best = new BeamSearch(beamWidth).solve(campaign);
        OutputWriter.write(best, output);
        System.out.println(input.getFileName() + ": " + best.score);
    }
}