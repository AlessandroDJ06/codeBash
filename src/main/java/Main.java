import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class Main {
    private static final Path DEFAULT_INPUT = Path.of("src", "main", "resources");
    private static final Path DEFAULT_OUTPUT = Path.of("output");
    private static final int DEFAULT_BEAM_WIDTH = 500;

    private Main() {}

    public static void main(String[] args) throws IOException {
        Path input = args.length > 0 ? Path.of(args[0]) : DEFAULT_INPUT;
        Path output = args.length > 1 ? Path.of(args[1]) : DEFAULT_OUTPUT;
        int beamWidth = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_BEAM_WIDTH;

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