import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class OutputWriter {
    private OutputWriter() {}

    static void write(CampaignState finalState, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (PathNode node = finalState.path; node != null; node = node.previous()) lines.add(node.line());
        Collections.reverse(lines);
        Files.write(file, lines);
    }
}
