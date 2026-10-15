package com.example.pdfcompressor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfAiService {

    private final PdfConversionService conversionService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PdfAiService(PdfConversionService conversionService) {
        this.conversionService = conversionService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generates a comprehensive summary of the PDF document.
     */
    public Map<String, Object> summarize(byte[] inputPdf) throws IOException {
        String docText = conversionService.extractText(inputPdf);
        if (docText == null || docText.trim().isEmpty()) {
            return Map.of("summary", "Document contains no readable text or is an image-only scan.",
                    "keyPoints", List.of(), "source", "local");
        }

        String geminiKey = System.getenv("GEMINI_API_KEY");
        if (geminiKey != null && !geminiKey.trim().isEmpty()) {
            String prompt = "Please provide an executive summary followed by 5 bulleted key takeaways for this document:\n\n"
                    + truncate(docText, 12000);
            String aiResult = callGemini(geminiKey, prompt);
            if (aiResult != null && !aiResult.isEmpty()) {
                return Map.of("summary", aiResult, "source", "gemini-ai");
            }
        }

        // Local NLP Heuristic Summarizer
        List<String> paragraphs = getCleanParagraphs(docText);
        List<String> sentences = extractSentences(docText);

        Map<String, Integer> wordFrequencies = getWordFrequencies(docText);
        List<ScoredSentence> scored = scoreSentences(sentences, wordFrequencies);

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        int topCount = Math.min(5, scored.size());
        List<String> keyPoints = new ArrayList<>();
        for (int i = 0; i < topCount; i++) {
            keyPoints.add(scored.get(i).sentence);
        }

        StringBuilder execSummary = new StringBuilder();
        execSummary.append("### Executive Overview\n");
        if (!paragraphs.isEmpty()) {
            execSummary.append(paragraphs.get(0)).append("\n\n");
            if (paragraphs.size() > 1) {
                execSummary.append(paragraphs.get(1)).append("\n\n");
            }
        }

        execSummary.append("### Key Takeaways\n");
        for (String kp : keyPoints) {
            execSummary.append("- ").append(kp.trim()).append("\n");
        }

        return Map.of(
                "summary", execSummary.toString(),
                "keyPoints", keyPoints,
                "source", "local-nlp",
                "notice", "AI-generated answers are grounded in document content and may require human verification."
        );
    }

    /**
     * Answers questions grounded in document content.
     */
    public Map<String, Object> chatWithPdf(byte[] inputPdf, String question) throws IOException {
        String docText = conversionService.extractText(inputPdf);
        if (docText == null || docText.trim().isEmpty()) {
            return Map.of("answer", "No readable text found in document.", "sources", List.of());
        }

        String geminiKey = System.getenv("GEMINI_API_KEY");
        if (geminiKey != null && !geminiKey.trim().isEmpty()) {
            String prompt = "Answer the following user question strictly based on the provided document text. "
                    + "If the answer is not in the text, state that clearly.\n\nQuestion: " + question + "\n\nDocument:\n"
                    + truncate(docText, 12000);
            String aiResult = callGemini(geminiKey, prompt);
            if (aiResult != null && !aiResult.isEmpty()) {
                return Map.of("answer", aiResult, "source", "gemini-ai");
            }
        }

        // Local Semantic Q&A Retriever
        List<String> paragraphs = getCleanParagraphs(docText);
        Set<String> queryWords = extractKeywords(question);

        List<ScoredSentence> matchedParagraphs = new ArrayList<>();
        for (String p : paragraphs) {
            double score = 0;
            String lowerP = p.toLowerCase();
            for (String qw : queryWords) {
                if (lowerP.contains(qw)) {
                    score += 1.0;
                }
            }
            if (score > 0) {
                matchedParagraphs.add(new ScoredSentence(p, score));
            }
        }

        matchedParagraphs.sort((a, b) -> Double.compare(b.score, a.score));

        StringBuilder answer = new StringBuilder();
        if (matchedParagraphs.isEmpty()) {
            answer.append("I searched the document, but did not find specific passages matching your question: '")
                    .append(question).append("'. Try phrasing with different keywords from the text.");
        } else {
            answer.append("Based on the document passages:\n\n");
            int maxPassages = Math.min(3, matchedParagraphs.size());
            for (int i = 0; i < maxPassages; i++) {
                answer.append("> ").append(matchedParagraphs.get(i).sentence.replace("\n", " ")).append("\n\n");
            }
        }

        return Map.of(
                "answer", answer.toString(),
                "source", "local-semantic-retriever",
                "notice", "Generated from document passages. Please verify against full document context."
        );
    }

    /**
     * Extracts action items, dates, and critical entities from document.
     */
    public Map<String, Object> extractKeyPoints(byte[] inputPdf) throws IOException {
        String docText = conversionService.extractText(inputPdf);
        List<String> actionItems = new ArrayList<>();
        List<String> statistics = new ArrayList<>();

        Pattern actionPattern = Pattern.compile("(?i)(must|shall|should|will|required to|action item|deadline|due date).*");
        Pattern statsPattern = Pattern.compile("(?i)(\\b\\d+([.,]\\d+)?%|\\$\\d+([.,]\\d+)?|\\b\\d{4}\\b).*");

        for (String line : docText.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.length() < 10) continue;

            Matcher mAction = actionPattern.matcher(trimmed);
            if (mAction.find() && actionItems.size() < 8) {
                actionItems.add(trimmed);
            }

            Matcher mStats = statsPattern.matcher(trimmed);
            if (mStats.find() && statistics.size() < 8) {
                statistics.add(trimmed);
            }
        }

        return Map.of(
                "actionItems", actionItems,
                "statistics", statistics,
                "fullSummary", summarize(inputPdf)
        );
    }

    /**
     * Generates study questions and answers based on document content.
     */
    public Map<String, Object> generateQuestions(byte[] inputPdf) throws IOException {
        String docText = conversionService.extractText(inputPdf);
        List<String> sentences = extractSentences(docText);
        List<Map<String, String>> qaList = new ArrayList<>();

        for (String s : sentences) {
            String clean = s.trim();
            if (clean.length() > 40 && clean.length() < 160 && clean.contains(" is ")) {
                String[] parts = clean.split(" is ", 2);
                if (parts.length == 2 && parts[0].length() > 5) {
                    qaList.add(Map.of(
                            "question", "What is " + parts[0].trim() + "?",
                            "answer", "According to the document: " + clean
                    ));
                }
            }
            if (qaList.size() >= 6) break;
        }

        if (qaList.isEmpty()) {
            qaList.add(Map.of(
                    "question", "What is the primary subject of this document?",
                    "answer", "Refer to the executive overview in the document header."
            ));
        }

        return Map.of("questions", qaList);
    }

    private String callGemini(String apiKey, String prompt) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            String payload = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }

    private List<String> getCleanParagraphs(String text) {
        List<String> list = new ArrayList<>();
        for (String p : text.split("\\r?\\n\\r?\\n")) {
            String trimmed = p.trim().replaceAll("\\s+", " ");
            if (trimmed.length() > 40) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private List<String> extractSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher m = Pattern.compile("[^.!?]+[.!?]").matcher(text);
        while (m.find()) {
            String s = m.group().trim().replaceAll("\\s+", " ");
            if (s.length() > 25) {
                sentences.add(s);
            }
        }
        return sentences;
    }

    private Map<String, Integer> getWordFrequencies(String text) {
        Map<String, Integer> freq = new HashMap<>();
        String[] words = text.toLowerCase().split("\\W+");
        Set<String> stopWords = Set.of("the", "and", "or", "to", "in", "of", "a", "an", "is", "for", "with", "as", "by", "on", "at", "it", "from", "that", "this");
        for (String w : words) {
            if (w.length() > 3 && !stopWords.contains(w)) {
                freq.put(w, freq.getOrDefault(w, 0) + 1);
            }
        }
        return freq;
    }

    private Set<String> extractKeywords(String text) {
        Set<String> kw = new HashSet<>();
        String[] words = text.toLowerCase().split("\\W+");
        Set<String> stopWords = Set.of("what", "how", "why", "where", "when", "who", "which", "is", "are", "the", "a", "an", "do", "does", "can", "in", "on", "of");
        for (String w : words) {
            if (w.length() > 2 && !stopWords.contains(w)) {
                kw.add(w);
            }
        }
        return kw;
    }

    private List<ScoredSentence> scoreSentences(List<String> sentences, Map<String, Integer> freq) {
        List<ScoredSentence> scored = new ArrayList<>();
        for (String s : sentences) {
            double score = 0;
            String[] words = s.toLowerCase().split("\\W+");
            for (String w : words) {
                score += freq.getOrDefault(w, 0);
            }
            if (words.length > 0) {
                score /= Math.sqrt(words.length);
            }
            scored.add(new ScoredSentence(s, score));
        }
        return scored;
    }

    private record ScoredSentence(String sentence, double score) {}
}
