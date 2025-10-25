package ep;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
public class JrnlImport {

    private List<JrnlImportEntry> entries;

    @NoArgsConstructor @AllArgsConstructor
    @Getter @Setter
    public static class JrnlImportEntry {
        private String title;
        private String body;
        private String date;
        private String time;
        private List<String> tags;
        private boolean starred;
    }
}
