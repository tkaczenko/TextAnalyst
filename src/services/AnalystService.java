package services;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for calculating letter frequency in {@code List<String>}
 *
 * @author tkaczenko
 */
public class AnalystService {
    private Locale locale;
    /**
     * Text for calculating
     */
    private List<String> strings;

    /**
     * Map of letter and its frequency
     */
    private Map<Character, Long> characterCount;

    /**
     * Map of letter and its range
     */
    private Map<Character, Integer> ranges;

    /**
     * Number of letter's range
     */
    private int numOfRange;

    private double delta;
    private Long maxFrequency;
    private Long minFrequency;

    public AnalystService(List<String> strings, int numOfRange, Locale locale) {
        setStrings(strings);
        setNumOfRange(numOfRange);
        setLocale(locale);
    }

    public void analyse() throws IllegalArgumentException {
        List<String> words = splitWords();
        countLetters(words);

        Map.Entry<Character, Long> maxEntry = max();
        Map.Entry<Character, Long> minEntry = min();

        if (maxEntry == null || minEntry == null) {
            throw new IllegalArgumentException("Language must equal language of user interface.");
        }

        maxFrequency = max().getValue();
        minFrequency = min().getValue();
        delta = (maxFrequency - minFrequency) / numOfRange;

        List<Map.Entry<Character, Long>> entries = characterCount.entrySet().parallelStream()
                .sorted(Map.Entry.<Character, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        ranges = new HashMap<>();
        for (Map.Entry<Character, Long> entry : entries) {
            for (int i = 0; i < numOfRange; i++) {
                if (entry.getValue() >= minFrequency + (numOfRange - 1 - i) * delta &&
                        entry.getValue() <= maxFrequency - i * delta) {
                    ranges.put(entry.getKey(), i);
                    break;
                }
            }
        }
    }

    /**
     * Split words
     *
     * @return List of words
     */
    private List<String> splitWords() {
        return strings.parallelStream()
                .flatMap(line -> Arrays.stream(line.trim().split("\\s")))
                .map(word -> word.replaceAll(getLanguageRelatedCharacterRanges(), "").toUpperCase().trim())
                .filter(word -> word.length() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Calculate frequencies of all letters
     *
     * @param words List of words
     */
    private void countLetters(List<String> words) {
        characterCount = words.parallelStream()
                .map(s -> s.chars())
                .flatMap(intStream -> intStream.mapToObj(n -> (char) n))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /**
     * Calculate maximum frequency of letters
     *
     * @return Entry of letter and its frequency
     */
    private Map.Entry<Character, Long> min() {
        Optional<Map.Entry<Character, Long>> optional = characterCount.entrySet().parallelStream()
                .min((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()));
        return optional.isPresent() ? optional.get() : null;
    }

    /**
     * Calcualte minimum frequency of letters
     *
     * @return Entry of letter and its frequency
     */
    private Map.Entry<Character, Long> max() {
        Optional<Map.Entry<Character, Long>> optional = characterCount.entrySet().parallelStream()
                .max((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()));
        return optional.isPresent() ? optional.get() : null;
    }

    private String getLanguageRelatedCharacterRanges() {
        String res;
        switch (locale.getLanguage()) {
            case "en":
                res = "[^а-zA-Z]";
                break;
            case "ru":
                res = "[^а-яА-ЯёЁ]";
                break;
            case "uk":
                res = "[^а-щА-ЩЬьЮюЯяЇїІіЄєҐґ]";
                break;
            default:
                res = "[^a-zA-Z]";
                break;
        }
        return res;
    }

    public void setStrings(List<String> strings) {
        this.strings = strings;
    }

    public void setNumOfRange(int numOfRange) {
        this.numOfRange = numOfRange;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Map<Character, Integer> getRanges() {
        return ranges;
    }

    public Map<Character, Long> getCharacterCount() {
        return characterCount;
    }

    public double getDelta() {
        return delta;
    }

    public Long getMaxFrequency() {
        return maxFrequency;
    }

    public Long getMinFrequency() {
        return minFrequency;
    }
}
