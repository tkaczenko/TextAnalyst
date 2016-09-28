package services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by tkaczenko on 27.09.16.
 */
public class AnalystService {
    private List<String> strings;

    private Map<Character, Long> characterCount;
    private Map<Character, Integer> ranges;

    private int numOfRange;

    private double delta;
    private Long maxFrequency;
    private Long minFrequency;

    public void analyse() {
        List<String> words = splitWords();
        countLetters(words);

        maxFrequency = max().getValue();
        minFrequency = min().getValue();
        delta = (maxFrequency - minFrequency) / numOfRange;

        List<Map.Entry<Character, Long>> entries = characterCount.entrySet().stream()
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

    private List<String> splitWords() {
        return strings.parallelStream()
                .flatMap(line -> Arrays.stream(line.trim().split("\\s")))
                .map(word -> word.replaceAll("[^а-щА-ЩЬьЮюЯяЇїІіЄєҐґ]", "").toUpperCase().trim())
                .filter(word -> word.length() > 0)
                .collect(Collectors.toList());
    }

    private void countLetters(List<String> words) {
        characterCount = words.parallelStream()
                .map(s -> s.chars())
                .flatMap(intStream -> intStream.mapToObj(n -> (char) n))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private Map.Entry<Character, Long> min() {
        return characterCount.entrySet().parallelStream()
                .min((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
                .get();
    }

    private Map.Entry<Character, Long> max() {
        return characterCount.entrySet().parallelStream()
                .max((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
                .get();
    }

    public void setStrings(List<String> strings) {
        this.strings = strings;
    }

    public void setNumOfRange(int numOfRange) {
        this.numOfRange = numOfRange;
    }

    public int getNumOfRange() {
        return numOfRange;
    }

    public double getDelta() {
        return delta;
    }

    public Map<Character, Integer> getRanges() {
        return ranges;
    }

    public Map<Character, Long> getCharacterCount() {
        return characterCount;
    }

}
