package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.ProjectFormField;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProjectFormParserService {

    private static final String TEMPLATE_CODE = "TT-FR-003.10";
    private static final String UNRECOGNIZED_TEMPLATE_MESSAGE =
            "Desteklenen proje formu taninamadi";

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "^(\\d+)\\s*(?:ay)?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern CHECKBOX_PATTERN = Pattern.compile(
            "([☐☑☒□■○●✓✔]|\\[[xX ]]|\\([xX ]\\))\\s*"
    );
    private static final Set<String> CHECKED_MARKERS = Set.of(
            "☑", "☒", "■", "●", "✓", "✔", "[x]", "(x)"
    );
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)
    );

    private static final List<Heading> HEADINGS = List.of(
            heading("Firma Unvanı", ProjectFormField.COMPANY_NAME),
            heading("Proje Adı", ProjectFormField.NAME),
            heading("Proje Yöneticisi", ProjectFormField.PROJECT_MANAGER),
            heading("Proje Süresi", ProjectFormField.DURATION_MONTHS),
            heading("Proje Başlangıç Tarihi", ProjectFormField.START_DATE),
            boundary("NACE Kodu"),
            heading("Proje Kategorisi", ProjectFormField.CATEGORY),
            heading("Proje Özeti", ProjectFormField.SUMMARY),
            heading("Projenin Sektörü", ProjectFormField.SECTOR),
            boundary("Proje Hedefleri"),
            boundary("Hedef Kitlesi"),
            boundary("Hedef Lokasyonu"),
            boundary("Hedeflenen Kazanım ve Sonuçlar"),
            boundary("Proje Nitelikleri"),
            heading("Ar-Ge Niteliği", ProjectFormField.RESEARCH_AND_DEVELOPMENT_NATURE),
            heading("Yenilikçi Yönü", ProjectFormField.INNOVATIVE_ASPECT),
            boundary("Proje İş Birlikleri"),
            boundary("Finansman Kaynağı"),
            boundary("Teknolojik Hazırlık Seviyesi"),
            boundary("Proje AR-GE Aşamaları"),
            headingAliases(ProjectFormField.MAIN_TECHNOLOGY_AREA,
                    "Proje Teknoloji Alanı", "Ana Teknoloji Alanı"),
            boundary("Teknopark Teknik Hizmetleri"),
            boundary("Teknopark Teknolojik Hizmetleri"),
            boundary("Teknoparkta Yer Alma Nedeni"),
            boundary("Teknoparkta Etkileşimde Bulunulan Firmalar"),
            boundary("Teknopark Danışmanlık Hizmetleri"),
            boundary("TTO Hizmetleri"),
            heading("Proje Detayı", ProjectFormField.PROJECT_DETAIL),
            heading("Anahtar Kelimeler", ProjectFormField.KEYWORDS),
            boundary("Proje Ekibi"),
            boundary("Proje Bütçesi"),
            boundary("Çevreye Etkileri")
    );

    private static final Set<String> TEMPLATE_MARKERS = normalizedSet(
            "Firma Unvanı", "Proje Adı", "Proje Özeti", "Ar-Ge Niteliği",
            "Yenilikçi Yönü", "Proje Teknoloji Alanı", "Proje Detayı",
            "Anahtar Kelimeler"
    );

    public ParsedProjectForm parse(ExtractedProjectDocumentText document) {
        if (document == null || document.extractedText() == null) {
            throw new InvalidRequestException(UNRECOGNIZED_TEMPLATE_MESSAGE);
        }

        List<String> lines = document.extractedText().lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .toList();
        recognizeTemplate(lines);
        Map<ProjectFormField, List<String>> sections = collectSections(lines);

        ParseState state = new ParseState();
        String companyName = state.text(ProjectFormField.COMPANY_NAME,
                valueLines(sections, ProjectFormField.COMPANY_NAME));
        String name = state.text(ProjectFormField.NAME,
                valueLines(sections, ProjectFormField.NAME));
        String projectManager = state.text(ProjectFormField.PROJECT_MANAGER,
                valueLines(sections, ProjectFormField.PROJECT_MANAGER));

        LocalDate startDate = parseDate(
                valueLines(sections, ProjectFormField.START_DATE), state);
        Integer durationMonths = parseDuration(
                valueLines(sections, ProjectFormField.DURATION_MONTHS), state);

        String category = parseSelection(ProjectFormField.CATEGORY,
                valueLines(sections, ProjectFormField.CATEGORY), state);
        String sector = parseSelection(ProjectFormField.SECTOR,
                valueLines(sections, ProjectFormField.SECTOR), state);

        String summary = state.text(ProjectFormField.SUMMARY,
                contentLines(sections, ProjectFormField.SUMMARY));
        String researchAndDevelopmentNature = state.text(
                ProjectFormField.RESEARCH_AND_DEVELOPMENT_NATURE,
                contentLines(sections, ProjectFormField.RESEARCH_AND_DEVELOPMENT_NATURE));
        String innovativeAspect = state.text(ProjectFormField.INNOVATIVE_ASPECT,
                contentLines(sections, ProjectFormField.INNOVATIVE_ASPECT));
        String mainTechnologyArea = state.text(ProjectFormField.MAIN_TECHNOLOGY_AREA,
                technologyAreaLines(sections));
        String projectDetail = state.text(ProjectFormField.PROJECT_DETAIL,
                contentLines(sections, ProjectFormField.PROJECT_DETAIL));
        Set<String> keywords = parseKeywords(
                contentLines(sections, ProjectFormField.KEYWORDS), state);

        return new ParsedProjectForm(
                TEMPLATE_CODE, companyName, name, projectManager, startDate,
                durationMonths, category, sector, summary,
                researchAndDevelopmentNature, innovativeAspect,
                mainTechnologyArea, projectDetail, keywords,
                immutableOrderedSet(state.missing),
                immutableOrderedSet(state.reviewRequired),
                List.copyOf(state.warnings)
        );
    }

    private void recognizeTemplate(List<String> lines) {
        Set<String> normalizedLines = new LinkedHashSet<>();
        for (String line : lines) {
            normalizedLines.add(normalizeLabel(line));
            int colon = line.indexOf(':');
            if (colon >= 0) {
                normalizedLines.add(normalizeLabel(line.substring(0, colon)));
            }
        }
        long matches = TEMPLATE_MARKERS.stream().filter(normalizedLines::contains).count();
        if (matches < 6) {
            throw new InvalidRequestException(UNRECOGNIZED_TEMPLATE_MESSAGE);
        }
    }

    private Map<ProjectFormField, List<String>> collectSections(List<String> lines) {
        Map<ProjectFormField, List<String>> sections = new EnumMap<>(ProjectFormField.class);
        ProjectFormField currentField = null;
        for (String line : lines) {
            HeadingMatch match = headingMatch(line);
            if (match != null) {
                currentField = match.heading().field();
                if (currentField != null) {
                    sections.computeIfAbsent(currentField, ignored -> new ArrayList<>());
                    if (match.inlineValue() != null && !match.inlineValue().isBlank()) {
                        sections.get(currentField).add(match.inlineValue().strip());
                    }
                }
                continue;
            }
            if (currentField != null) {
                sections.get(currentField).add(line);
            }
        }
        return sections;
    }

    private HeadingMatch headingMatch(String line) {
        String normalized = normalizeLabel(line);
        for (Heading heading : HEADINGS) {
            for (String alias : heading.aliases()) {
                if (normalized.equals(alias)) {
                    return new HeadingMatch(heading, null);
                }
                String rawPrefix = aliasPrefixMatch(line, alias);
                if (rawPrefix != null) {
                    return new HeadingMatch(heading, rawPrefix);
                }
            }
        }
        return null;
    }

    private String aliasPrefixMatch(String line, String normalizedAlias) {
        int colon = line.indexOf(':');
        if (colon < 0 || !normalizeLabel(line.substring(0, colon)).equals(normalizedAlias)) {
            return null;
        }
        return line.substring(colon + 1);
    }

    private List<String> valueLines(
            Map<ProjectFormField, List<String>> sections,
            ProjectFormField field
    ) {
        return sections.getOrDefault(field, List.of()).stream()
                .filter(line -> !isInstruction(line))
                .toList();
    }

    private List<String> contentLines(
            Map<ProjectFormField, List<String>> sections,
            ProjectFormField field
    ) {
        return valueLines(sections, field);
    }

    private List<String> technologyAreaLines(
            Map<ProjectFormField, List<String>> sections
    ) {
        return valueLines(sections, ProjectFormField.MAIN_TECHNOLOGY_AREA).stream()
                .filter(line -> !normalizeLabel(line).equals(normalizeLabel("Bir öğe seçin.")))
                .toList();
    }

    private boolean isInstruction(String line) {
        String normalized = normalizeLabel(line)
                .replaceFirst("^[“”\\\"'→•-]+\\s*", "");
        return normalized.startsWith(normalizeLabel("Bu bölümde"))
                || normalized.startsWith(normalizeLabel("Projenizin araştırma ve geliştirme"))
                || normalized.startsWith(normalizeLabel("Kullandığınız teknolojilerin"))
                || normalized.startsWith(normalizeLabel("Personelinizin uzmanlık alanları"))
                || normalized.startsWith(normalizeLabel("Ayrıca, proje için yaptığınız"))
                || normalized.startsWith(normalizeLabel("Projeden beklenen toplam gelire"))
                || normalized.startsWith(normalizeLabel("Toplam üretim giderlerine"))
                || normalized.startsWith(normalizeLabel("Proje kapsamında gerçekleştirilecek"))
                || normalized.startsWith(normalizeLabel("Projenin ilgili olduğu sektör"))
                || normalized.startsWith(normalizeLabel("Ana Teknoloji Alanını seçiniz"));
    }

    private LocalDate parseDate(List<String> lines, ParseState state) {
        String raw = joined(lines);
        if (raw == null) {
            state.missing.add(ProjectFormField.START_DATE);
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next explicitly supported format.
            }
        }
        state.review(ProjectFormField.START_DATE,
                "Proje baslangic tarihi kontrol edilmeli: " + raw);
        return null;
    }

    private Integer parseDuration(List<String> lines, ParseState state) {
        String raw = joined(lines);
        if (raw == null) {
            state.missing.add(ProjectFormField.DURATION_MONTHS);
            return null;
        }
        Matcher matcher = DURATION_PATTERN.matcher(raw.strip());
        if (matcher.matches()) {
            try {
                int duration = Integer.parseInt(matcher.group(1));
                if (duration > 0) {
                    return duration;
                }
            } catch (NumberFormatException ignored) {
                // Mark as review-required below.
            }
        }
        state.review(ProjectFormField.DURATION_MONTHS,
                "Proje suresi kontrol edilmeli: " + raw);
        return null;
    }

    private String parseSelection(
            ProjectFormField field,
            List<String> lines,
            ParseState state
    ) {
        if (lines.isEmpty()) {
            state.missing.add(field);
            return null;
        }
        String source = String.join("\n", lines);
        Matcher matcher = CHECKBOX_PATTERN.matcher(source);
        List<MarkerPosition> markers = new ArrayList<>();
        while (matcher.find()) {
            markers.add(new MarkerPosition(matcher.group(1), matcher.start(), matcher.end()));
        }
        if (markers.isEmpty()) {
            String raw = joined(lines);
            state.review(field, fieldWarning(field) + ": " + raw);
            return raw;
        }

        List<String> selected = new ArrayList<>();
        for (int index = 0; index < markers.size(); index++) {
            MarkerPosition marker = markers.get(index);
            int end = index + 1 < markers.size()
                    ? markers.get(index + 1).start()
                    : source.length();
            String option = source.substring(marker.end(), end)
                    .replace('\n', ' ')
                    .strip();
            if (isChecked(marker.marker()) && !option.isBlank()) {
                selected.add(option);
            }
        }
        if (selected.isEmpty()) {
            state.missing.add(field);
            return null;
        }
        return String.join(", ", selected);
    }

    private boolean isChecked(String marker) {
        return CHECKED_MARKERS.contains(marker.toLowerCase(Locale.ROOT));
    }

    private Set<String> parseKeywords(List<String> lines, ParseState state) {
        String raw = joined(lines);
        if (raw == null) {
            state.missing.add(ProjectFormField.KEYWORDS);
            return Set.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        Set<String> canonical = new LinkedHashSet<>();
        for (String keyword : raw.split("[,;\\n]")) {
            String trimmed = keyword.strip();
            if (!trimmed.isBlank() && canonical.add(normalizeValueKey(trimmed))) {
                keywords.add(trimmed);
            }
        }
        if (keywords.isEmpty()) {
            state.missing.add(ProjectFormField.KEYWORDS);
        }
        return immutableOrderedSet(keywords);
    }

    private String fieldWarning(ProjectFormField field) {
        return switch (field) {
            case CATEGORY -> "Proje kategorisi secimi kontrol edilmeli";
            case SECTOR -> "Proje sektoru secimi kontrol edilmeli";
            default -> "Alan kontrol edilmeli";
        };
    }

    private static Heading heading(String label, ProjectFormField field) {
        return headingAliases(field, label);
    }

    private static Heading headingAliases(ProjectFormField field, String... aliases) {
        return new Heading(field, normalizedSet(aliases));
    }

    private static Heading boundary(String label) {
        return headingAliases(null, label);
    }

    private static Set<String> normalizedSet(String... values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            result.add(normalizeLabel(value));
        }
        return Set.copyOf(result);
    }

    private static <T> Set<T> immutableOrderedSet(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private static String normalizeLabel(String value) {
        String turkishSafe = value
                .replace('\u00A0', ' ')
                .replace('İ', 'i')
                .replace('I', 'i')
                .replace('ı', 'i');
        String ascii = Normalizer.normalize(turkishSafe, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Z}]+", " ")
                .strip();
        return ascii.replaceFirst("\\s*:\\s*$", "");
    }

    private String normalizeValueKey(String value) {
        return normalizeLabel(value);
    }

    private String joined(List<String> lines) {
        String value = String.join("\n", lines).strip();
        return value.isBlank() ? null : value;
    }

    private record Heading(ProjectFormField field, Set<String> aliases) {
    }

    private record HeadingMatch(Heading heading, String inlineValue) {
    }

    private record MarkerPosition(String marker, int start, int end) {
    }

    private static class ParseState {
        private final Set<ProjectFormField> missing = new LinkedHashSet<>();
        private final Set<ProjectFormField> reviewRequired = new LinkedHashSet<>();
        private final List<String> warnings = new ArrayList<>();

        private String text(ProjectFormField field, List<String> lines) {
            String value = String.join("\n", lines).strip();
            if (value.isBlank()) {
                missing.add(field);
                return null;
            }
            return value;
        }

        private void review(ProjectFormField field, String warning) {
            reviewRequired.add(field);
            warnings.add(warning);
        }
    }
}
